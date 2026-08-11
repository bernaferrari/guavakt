package dev.guavakt.util.concurrent

/**
 * Guava AbstractFuture — settable [ListenableFuture] base with listener fan-out,
 * cancellation, and future chaining.
 *
 * Blocking [get] is supported on JVM. On JS, Wasm, and Native an incomplete future
 * throws [UnsupportedOperationException]; use listeners or coroutine adapters there.
 */
abstract class AbstractFuture<V> : ListenableFuture<V> {
    private val lock = Any()
    private var done = false
    private var cancelled = false
    private var cancellationWasInterrupted = false
    private var value: V? = null
    private var exception: Throwable? = null
    private val listeners = ArrayList<() -> Unit>()
    private var nestedFuture: ListenableFuture<out V>? = null

    override fun isDone(): Boolean = platformMonitorSync(lock) { done }
    override fun isCancelled(): Boolean = platformMonitorSync(lock) { cancelled }

    override open fun cancel(mayInterruptIfRunning: Boolean): Boolean =
        cancelInternal(mayInterruptIfRunning, cancelNested = true)

    /** Completes from a cancelled delegate without treating it as a direct caller cancellation. */
    internal fun cancelFromDelegate(): Boolean = cancelInternal(false, cancelNested = false)

    private fun cancelInternal(mayInterruptIfRunning: Boolean, cancelNested: Boolean): Boolean {
        val listenersCopy = ArrayList<() -> Unit>()
        var nested: ListenableFuture<out V>? = null
        val ok = platformMonitorSync(lock) {
            if (done) return@platformMonitorSync false
            cancelled = true
            cancellationWasInterrupted = mayInterruptIfRunning
            done = true
            nested = nestedFuture
            nestedFuture = null
            platformMonitorNotifyAll(lock)
            listenersCopy.addAll(listeners)
            listeners.clear()
            true
        }
        if (ok) {
            if (cancelNested) nested?.cancel(mayInterruptIfRunning)
            if (mayInterruptIfRunning) interruptTask()
            afterDone()
            for (l in listenersCopy) runListener(l)
        }
        return ok
    }

    override fun get(): V {
        if (!isDone() && !platformSupportsBlockingWait()) {
            throw UnsupportedOperationException(
                "Blocking Future.get() is unavailable on this target; use addListener or coroutines",
            )
        }
        platformMonitorSync(lock) {
            while (!done) {
                platformMonitorWait(lock, 10)
            }
        }
        if (cancelled) throw CancellationException("Future was cancelled")
        exception?.let { throw ExecutionException(it) }
        @Suppress("UNCHECKED_CAST")
        return value as V
    }

    /** Non-blocking: value if done, rethrows failure, null if incomplete. */
    fun getIfDone(): V? {
        if (!isDone()) return null
        if (isCancelled()) throw CancellationException("Future was cancelled")
        exception?.let { throw ExecutionException(it) }
        @Suppress("UNCHECKED_CAST")
        return value as V
    }

    override fun addListener(listener: () -> Unit) {
        val runNow = platformMonitorSync(lock) {
            if (done) true else {
                listeners.add(listener)
                false
            }
        }
        if (runNow) runListener(listener)
    }

    /** Complete successfully. Returns false if already completed. */
    protected fun setValue(value: V): Boolean = complete(value, null, cancelled = false)

    /** Complete with failure. Returns false if already completed. */
    protected fun setFailure(throwable: Throwable): Boolean =
        complete(null, throwable, cancelled = false)

    /**
     * Complete this future with the result of [future] when it finishes.
     * Cancellation of this future propagates to [future].
     */
    protected fun setAsync(future: ListenableFuture<out V>): Boolean {
        var cancelRejectedDelegate = false
        var interruptRejectedDelegate = false
        val accepted = platformMonitorSync(lock) {
            if (done) {
                cancelRejectedDelegate = cancelled
                interruptRejectedDelegate = cancellationWasInterrupted
                return@platformMonitorSync false
            }
            nestedFuture = future
            true
        }
        if (!accepted) {
            if (cancelRejectedDelegate) future.cancel(interruptRejectedDelegate)
            return false
        }
        if (future.isDone()) {
            propagateFrom(future)
            return true
        }
        future.addListener { propagateFrom(future) }
        if (isCancelled()) future.cancel(false)
        return true
    }

    private fun propagateFrom(future: ListenableFuture<out V>) {
        try {
            if (future.isCancelled()) {
                cancelFromDelegate()
            } else {
                val v = future.get()
                setValue(v)
            }
        } catch (t: Throwable) {
            val cause = if (t is ExecutionException) (t.cause ?: t) else t
            if (t is CancellationException) cancelFromDelegate()
            else setFailure(cause)
        }
    }

    private fun complete(value: V?, exception: Throwable?, cancelled: Boolean): Boolean {
        val listenersCopy = ArrayList<() -> Unit>()
        val ok = platformMonitorSync(lock) {
            if (done) return@platformMonitorSync false
            this.cancelled = cancelled
            this.value = value
            this.exception = exception
            this.done = true
            this.nestedFuture = null
            platformMonitorNotifyAll(lock)
            listenersCopy.addAll(listeners)
            listeners.clear()
            true
        }
        if (ok) {
            afterDone()
            for (l in listenersCopy) runListener(l)
        }
        return ok
    }

    protected open fun afterDone() {}
    protected open fun interruptTask() {}

    /** Whether this future was directly cancelled with interruption requested. */
    protected fun wasInterrupted(): Boolean =
        platformMonitorSync(lock) { cancelled && cancellationWasInterrupted }

    private fun runListener(listener: () -> Unit) {
        try {
            listener()
        } catch (_: Throwable) {
        }
    }

    internal fun completeValue(value: V): Boolean = setValue(value)
    internal fun completeExceptionally(throwable: Throwable): Boolean = setFailure(throwable)
    internal fun completeWithFuture(future: ListenableFuture<out V>): Boolean = setAsync(future)
}
