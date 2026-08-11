package dev.guavakt.util.concurrent

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertTrue

class RateLimiterJvmTest {
    @Test
    fun blockingAcquireRestoresInterruptStatusLikeGuava() {
        val limiter = RateLimiter.create(20.0)
        limiter.acquire()
        val interruptedAfterAcquire = AtomicBoolean(false)
        val worker = Thread {
            Thread.currentThread().interrupt()
            limiter.acquire()
            interruptedAfterAcquire.set(Thread.currentThread().isInterrupted)
        }

        worker.start()
        worker.join(2_000)

        assertTrue(!worker.isAlive, "acquire should finish after its reservation")
        assertTrue(interruptedAfterAcquire.get(), "uninterruptible wait must restore interrupt status")
    }
}
