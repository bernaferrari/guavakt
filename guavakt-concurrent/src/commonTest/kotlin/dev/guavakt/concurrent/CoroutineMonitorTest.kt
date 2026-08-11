package dev.guavakt.concurrent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineMonitorTest {
    @Test
    fun waitsForStateChangeAndRunsWhileProtected() = runTest {
        val monitor = CoroutineMonitor()
        var value = 0
        val positive = monitor.newGuard { value > 0 }
        val result = async { monitor.withLockWhen(positive) { value-- } }
        runCurrent()
        assertFalse(result.isCompleted)

        monitor.withLock { value = 2 }
        runCurrent()
        assertEquals(2, result.await())
        assertEquals(1, monitor.withLock { value })
    }

    @Test
    fun immediateTimeoutChecksOnceButDoesNotTimeProtectedWork() = runTest {
        val monitor = CoroutineMonitor()
        var ready = false
        val guard = monitor.newGuard { ready }
        assertFalse(monitor.tryWithLockWhen(guard, ZERO) { throw AssertionError() })

        monitor.withLock { ready = true }
        var ran = false
        assertTrue(monitor.tryWithLockWhen(guard, ZERO) {
            delay(10.seconds)
            ran = true
        })
        assertTrue(ran)
    }

    @Test
    fun cancellationNeverLeaksTheMutex() = runTest {
        val monitor = CoroutineMonitor()
        val ready = monitor.newGuard { true }
        val entered = CompletableDeferred<Unit>()
        val waiter = launch {
            monitor.withLockWhen(ready) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        entered.await()
        waiter.cancel(CancellationException())
        waiter.join()

        assertFalse(monitor.isLocked())
        assertEquals(7, monitor.withLock { 7 })
    }
}
