package dev.guavakt.util.concurrent

/**
 * JVM-shaped blocking time limiter.
 *
 * A common [DirectExecutorLike] cannot preempt a blocking lambda or provide Java thread
 * interruption, so pretending to enforce a timeout would be unsafe. Use [CoroutineTimeLimiter]
 * for structured, cancellable common code. [FakeTimeLimiter] remains the explicit no-time test
 * double.
 */
class SimpleTimeLimiter private constructor(
    @Suppress("unused") private val executor: DirectExecutorLike,
) : TimeLimiter {
    override fun <T> callWithTimeout(callable: () -> T, timeoutMillis: Long): T = unavailable(timeoutMillis)
    override fun <T> callUninterruptiblyWithTimeout(callable: () -> T, timeoutMillis: Long): T = unavailable(timeoutMillis)
    override fun runWithTimeout(runnable: () -> Unit, timeoutMillis: Long): Unit = unavailable(timeoutMillis)
    override fun runUninterruptiblyWithTimeout(runnable: () -> Unit, timeoutMillis: Long): Unit = unavailable(timeoutMillis)
    override fun <T> newProxy(target: T, interfaceType: Any, timeoutMillis: Long): T = unavailable(timeoutMillis)

    private fun unavailable(timeoutMillis: Long): Nothing {
        require(timeoutMillis > 0) { "timeout must be positive: $timeoutMillis" }
        throw UnsupportedOperationException(
            "SimpleTimeLimiter cannot enforce blocking timeouts on common targets; use CoroutineTimeLimiter",
        )
    }

    companion object {
        fun create(executor: DirectExecutorLike): SimpleTimeLimiter = SimpleTimeLimiter(executor)
    }
}
