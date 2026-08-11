package dev.guavakt.util.concurrent

/** Guava FakeTimeLimiter — TimeLimiter that runs immediately (testing). */
class FakeTimeLimiter : TimeLimiter {
    override fun <T> callWithTimeout(callable: () -> T, timeoutMillis: Long): T = callable()
    override fun <T> callUninterruptiblyWithTimeout(callable: () -> T, timeoutMillis: Long): T = callable()
    override fun runWithTimeout(runnable: () -> Unit, timeoutMillis: Long) = runnable()
    override fun runUninterruptiblyWithTimeout(runnable: () -> Unit, timeoutMillis: Long) = runnable()
    override fun <T> newProxy(target: T, interfaceType: Any, timeoutMillis: Long): T = target
}
