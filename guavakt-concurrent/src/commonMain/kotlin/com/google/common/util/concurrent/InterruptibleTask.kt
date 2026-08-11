package dev.guavakt.util.concurrent

/**
 * Guava-shaped one-shot task runner.
 *
 * A task is claimed at most once, including when [isDone] is already true. JVM callers can still
 * use the surrounding executor's interruption facilities; common targets record an interruption
 * request for a cooperative task to observe through [wasInterruptRequested] rather than claiming
 * to stop arbitrary running code.
 */
internal abstract class InterruptibleTask<T> {
    private val stateLock = Any()
    private var runner: Any? = null
    private var interruptRequested = false

    final operator fun invoke() {
        val current = Any()
        if (!claim(current)) return

        val shouldRun = !isDone()
        val outcome = if (shouldRun) {
            try {
                Result.success(runInterruptibly())
            } catch (failure: Throwable) {
                Result.failure(failure)
            }
        } else {
            null
        }

        complete(current)
        if (shouldRun) {
            outcome!!.fold(
                onSuccess = ::afterRanInterruptiblySuccess,
                onFailure = ::afterRanInterruptiblyFailure,
            )
        }
    }

    /** Records a cooperative cancellation request if the task is currently running. */
    fun interruptTask() {
        monitorSync(stateLock) {
            if (runner != null && runner !== DONE) interruptRequested = true
        }
    }

    /** Lets a common task stop itself at a safe point after [interruptTask] was called. */
    protected fun wasInterruptRequested(): Boolean = monitorSync(stateLock) { interruptRequested }

    private fun claim(current: Any): Boolean = monitorSync(stateLock) {
        if (runner != null) return@monitorSync false
        runner = current
        true
    }

    private fun complete(current: Any) {
        monitorSync(stateLock) {
            if (runner === current) runner = DONE
        }
    }

    abstract fun runInterruptibly(): T
    abstract fun afterRanInterruptiblySuccess(result: T)
    abstract fun afterRanInterruptiblyFailure(error: Throwable)
    abstract fun isDone(): Boolean
    abstract override fun toString(): String

    private companion object {
        val DONE = Any()
    }
}
