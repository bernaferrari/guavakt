package dev.guavakt.util.concurrent

/**
 * Guava-shaped blocking timeout interface for migration code.
 *
 * Its proxy and interruption semantics require JVM facilities. Common production code should use
 * [CoroutineTimeLimiter]; [SimpleTimeLimiter] refuses to pretend it can preempt a blocking lambda,
 * while [FakeTimeLimiter] remains the explicit no-time test double.
 */
interface TimeLimiter {
    fun <T> callWithTimeout(callable: () -> T, timeoutMillis: Long): T
    fun <T> callUninterruptiblyWithTimeout(callable: () -> T, timeoutMillis: Long): T
    fun runWithTimeout(runnable: () -> Unit, timeoutMillis: Long)
    fun runUninterruptiblyWithTimeout(runnable: () -> Unit, timeoutMillis: Long)
    fun <T> newProxy(target: T, interfaceType: Any, timeoutMillis: Long): T
}
