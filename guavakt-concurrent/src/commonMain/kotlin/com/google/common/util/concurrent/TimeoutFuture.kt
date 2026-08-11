package dev.guavakt.util.concurrent

/**
 * Guava TimeoutFuture — fails with [TimeoutException] if [input] is not done in time.
 * The deadline is delivered by [platformSchedule] on every supported target. Prefer [await] from
 * a coroutine on non-JVM targets because incomplete [ListenableFuture.get] is intentionally a
 * JVM-only blocking bridge.
 */
class TimeoutFuture<V> private constructor(
    private val input: ListenableFuture<V>,
    private val timeoutMillis: Long,
) : AbstractFuture<V>() {
    private var timer: PlatformScheduledHandle? = null

    init {
        input.addListener {
            if (isDone()) return@addListener
            if (input.isCancelled()) cancel(false)
            else try {
                setValue(input.get())
            } catch (t: Throwable) {
                val cause = if (t is ExecutionException) (t.cause ?: t) else t
                setFailure(cause)
            }
        }
        if (!isDone()) {
            val scheduled = platformSchedule(timeoutMillis.coerceAtLeast(0L)) { runTimeout() }
            timer = scheduled
            // A listener may have completed us while an immediate callback was being registered.
            if (isDone()) scheduled.cancel()
        }
    }

    /** Force timeout (tests / non-JVM). Safe if already completed. */
    fun runTimeout() {
        if (setFailure(TimeoutException("Timeout after ${timeoutMillis}ms"))) {
            input.cancel(true)
        }
    }

    override fun afterDone() {
        timer?.cancel()
        if (isCancelled()) input.cancel(false)
    }

    companion object {
        fun <V> create(input: ListenableFuture<V>, timeoutMillis: Long): TimeoutFuture<V> =
            TimeoutFuture(input, timeoutMillis)
    }
}

class TimeoutException(message: String) : Exception(message)
