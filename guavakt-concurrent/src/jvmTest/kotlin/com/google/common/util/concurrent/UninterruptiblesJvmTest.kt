package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UninterruptiblesJvmTest {
    @Test
    fun timedGetWaitsWithoutCancellingItsInput() {
        val future = SettableFuture.create<Int>()
        platformSchedule(20) { future.set(42) }

        assertEquals(42, Uninterruptibles.getUninterruptibly(future, 500))
        assertTrue(!future.isCancelled())
    }

    @Test
    fun timedGetReportsTimeoutWithoutCancellingItsInput() {
        val future = SettableFuture.create<Int>()

        assertFailsWith<TimeoutException> {
            Uninterruptibles.getUninterruptibly(future, 1)
        }
        assertTrue(!future.isCancelled())
    }

    @Test
    fun sleepUsesTheRequestedMillisecondUnit() {
        val startedAt = System.nanoTime()
        Uninterruptibles.sleepUninterruptibly(5)
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L

        assertTrue(elapsedMillis >= 4L)
    }
}
