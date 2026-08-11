package dev.guavakt.util.concurrent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorContractTest {
    @Test
    fun immediateOperationsAreReentrantAndGuardAware() {
        val monitor = Monitor(fair = true)
        var ready = false
        val guard = monitor.newGuard { ready }

        assertTrue(monitor.isFair())
        assertFalse(monitor.isOccupied())
        assertFalse(monitor.tryEnterIf(guard))
        assertFalse(monitor.isOccupied())

        monitor.enter()
        monitor.enter()
        assertTrue(monitor.isOccupiedByCurrentThread())
        assertEquals(2, monitor.getOccupiedDepth())
        ready = true
        assertTrue(monitor.tryEnterIf(guard))
        assertEquals(3, monitor.getOccupiedDepth())
        monitor.leave()
        monitor.leave()
        monitor.leave()
        assertFalse(monitor.isOccupied())
    }

    @Test
    fun zeroTimeoutChecksOnceAndNeverLeaksOwnership() {
        val monitor = Monitor()
        var ready = false
        val guard = monitor.newGuard { ready }

        assertFalse(monitor.enterWhen(guard, ZERO))
        assertFalse(monitor.isOccupiedByCurrentThread())
        assertFalse(monitor.enterWhenUninterruptibly(guard, ZERO))
        assertFalse(monitor.enterIf(guard, ZERO))
        ready = true
        assertTrue(monitor.enterWhen(guard, ZERO))
        monitor.leave()
    }

    @Test
    fun wrongMonitorAndUnoccupiedWaitThrowMonitorStateException() {
        val first = Monitor()
        val second = Monitor()
        val guard = first.newGuard { true }

        assertFailsWith<IllegalMonitorStateException> { second.tryEnterIf(guard) }
        assertFailsWith<IllegalMonitorStateException> { first.waitFor(guard) }
        assertFailsWith<IllegalMonitorStateException> { first.leave() }
    }

    @Test
    fun coroutineMonitorWaitsWithoutPollingAndRunsUnderProtection() = runTest {
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
    fun coroutineTimeoutDoesImmediateCheckAndDoesNotTimeProtectedWork() = runTest {
        val monitor = CoroutineMonitor()
        var ready = false
        val guard = monitor.newGuard { ready }
        assertFalse(monitor.tryWithLockWhen(guard, ZERO) { error("must not run") })

        monitor.withLock { ready = true }
        var ran = false
        assertTrue(monitor.tryWithLockWhen(guard, ZERO) {
            delay(10.seconds)
            ran = true
        })
        assertTrue(ran)
    }

    @Test
    fun cancelledCoroutineWaiterDoesNotOwnOrPoisonMonitor() = runTest {
        val monitor = CoroutineMonitor()
        var ready = true
        val guard = monitor.newGuard { ready }
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val waiter = launch {
            monitor.withLockWhen(guard) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        entered.await()
        waiter.cancel(CancellationException("test"))
        waiter.join()

        assertFalse(monitor.isLocked())
        assertEquals(7, monitor.withLock { 7 })
        monitor.withLock { ready = false }
        assertFalse(monitor.tryWithLockWhen(guard, 1.seconds) { error("must not run") })
    }
}
