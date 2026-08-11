package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class ScheduledExecutorContractTest {
    @Test
    fun oneShotSchedule_completesThroughTheListeningFuture() = runTest {
        val executor = WrappingScheduledExecutorService()
        val future = executor.schedule(kotlin.time.Duration.ZERO) { 42 }
        var listenerCalls = 0
        future.addListener { listenerCalls++ }

        assertEquals(42, future.await())
        assertTrue(future.isDone())
        assertEquals(1, listenerCalls)
        executor.shutdown()
    }

    @Test
    fun cancellation_stopsPendingWork_andShutdownRejectsNewSchedules() {
        val executor = WrappingScheduledExecutorService()
        val future = executor.schedule(30.seconds) { error("cancelled work ran") }

        assertTrue(future.remainingDelay() > kotlin.time.Duration.ZERO)
        assertTrue(future.cancel(false))
        assertTrue(future.isCancelled())
        assertFalse(future.cancel(false))

        executor.shutdown()
        kotlin.test.assertFailsWith<IllegalStateException> {
            executor.schedule(kotlin.time.Duration.ZERO) { Unit }
        }
    }
}
