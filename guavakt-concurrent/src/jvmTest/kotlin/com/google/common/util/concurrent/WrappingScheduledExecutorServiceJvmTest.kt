package dev.guavakt.util.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class WrappingScheduledExecutorServiceJvmTest {
    @Test
    fun periodicFailure_completesTheScheduledFutureExceptionally_andStopsFurtherRuns() {
        val executor = WrappingScheduledExecutorService()
        val ran = CountDownLatch(1)
        val future = executor.scheduleAtFixedRate(0.milliseconds, 10.milliseconds) {
            ran.countDown()
            throw IllegalStateException("boom")
        }

        assertTrue(ran.await(1, TimeUnit.SECONDS))
        assertFailsWith<ExecutionException> { future.get() }
        assertTrue(future.isDone())
        executor.shutdown()
    }

    @Test
    fun fixedDelay_waitsForThePreviousRunBeforeSchedulingTheNextOne() {
        val executor = WrappingScheduledExecutorService()
        val starts = ArrayList<Long>()
        val completed = CountDownLatch(2)
        val future = executor.scheduleWithFixedDelay(10.milliseconds, 40.milliseconds) {
            starts.add(System.nanoTime())
            Thread.sleep(30)
            completed.countDown()
        }

        assertTrue(completed.await(2, TimeUnit.SECONDS))
        future.cancel(false)
        val elapsedMillis = (starts[1] - starts[0]) / 1_000_000L
        assertTrue(elapsedMillis >= 60L, "gap=$elapsedMillis ms must include the work plus fixed delay")
        executor.shutdown()
    }
}
