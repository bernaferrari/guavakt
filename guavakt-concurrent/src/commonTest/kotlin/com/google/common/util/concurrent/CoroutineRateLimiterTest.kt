package dev.guavakt.util.concurrent

import dev.guavakt.base.Ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineRateLimiterTest {
    @Test
    fun acquireSuspendsWithoutBlockingUntilItsReservation() = runTest {
        val limiter = CoroutineRateLimiter.create(2.0, SchedulerTicker(testScheduler))
        assertEquals(0.milliseconds, limiter.acquire())

        val second = async { limiter.acquire() }
        runCurrent()
        assertFalse(second.isCompleted)
        advanceTimeBy(499)
        runCurrent()
        assertFalse(second.isCompleted)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(500.milliseconds, second.await())
    }

    @Test
    fun concurrentCallersReceiveMonotonicReservations() = runTest {
        val limiter = CoroutineRateLimiter.create(2.0, SchedulerTicker(testScheduler))
        val completions = mutableListOf<Long>()
        val calls = List(3) {
            async {
                limiter.acquire()
                completions += testScheduler.currentTime
            }
        }

        runCurrent()
        assertEquals(listOf(0L), completions)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf(0L, 500L), completions)
        advanceTimeBy(500)
        runCurrent()

        calls.forEach { it.await() }
        assertEquals(listOf(0L, 500L, 1_000L), completions)
    }

    @Test
    fun cancellationStopsWaitingButDoesNotRefundCommittedCapacity() = runTest {
        val limiter = CoroutineRateLimiter.create(2.0, SchedulerTicker(testScheduler))
        limiter.acquire()
        val cancelled = async { limiter.acquire() }
        val survivor = async { limiter.acquire() }
        runCurrent()

        advanceTimeBy(100)
        cancelled.cancelAndJoin()
        advanceTimeBy(899)
        runCurrent()
        assertFalse(survivor.isCompleted)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(1.seconds, survivor.await())
        val stats = limiter.stats()
        assertEquals(2, stats.completedAcquisitionCount)
        assertEquals(1, stats.cancelledAcquisitionCount)
        assertEquals(2, stats.acquiredPermitCount)
        assertEquals(1, stats.cancelledPermitCount)
        assertEquals(1_500_000_000L, stats.totalReservedWaitTimeNanos)
    }

    @Test
    fun manyCancelledReservationsStillProtectTheThroughputCeiling() = runTest {
        val limiter = CoroutineRateLimiter.create(10.0, SchedulerTicker(testScheduler))
        limiter.acquire()
        val queued = List(8) { async { limiter.acquire() } }
        runCurrent()

        queued.take(7).forEach { it.cancelAndJoin() }
        advanceTimeBy(799)
        runCurrent()
        assertFalse(queued.last().isCompleted)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(800.milliseconds, queued.last().await())
        val stats = limiter.stats()
        assertEquals(2, stats.completedAcquisitionCount)
        assertEquals(7, stats.cancelledAcquisitionCount)
        assertEquals(2, stats.acquiredPermitCount)
        assertEquals(7, stats.cancelledPermitCount)
        assertEquals(3_600_000_000L, stats.totalReservedWaitTimeNanos)
    }

    @Test
    fun timeoutRejectionIsImmediateAndDoesNotMutateSchedule() = runTest {
        val limiter = CoroutineRateLimiter.create(2.0, SchedulerTicker(testScheduler))
        limiter.acquire()

        assertFalse(limiter.tryAcquire(499.milliseconds))
        val accepted = async { limiter.tryAcquire(500.milliseconds) }
        runCurrent()
        assertFalse(accepted.isCompleted)
        advanceTimeBy(500)
        runCurrent()

        assertTrue(accepted.await())
        assertEquals(1, limiter.stats().rejectedAcquisitionCount)
    }

    @Test
    fun idleLimiterAccumulatesBurstPermits() = runTest {
        val limiter = CoroutineRateLimiter.create(2.0, SchedulerTicker(testScheduler))
        limiter.acquire()
        advanceTimeBy(1_500)

        assertEquals(0.milliseconds, limiter.acquire(2))
        assertEquals(0.milliseconds, limiter.acquire())
        val next = async { limiter.acquire() }
        runCurrent()
        assertFalse(next.isCompleted)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(500.milliseconds, next.await())
    }

    @Test
    fun warmupUsesColdThenStableIntervals() = runTest {
        val limiter = CoroutineRateLimiter.create(
            permitsPerSecond = 2.0,
            warmupPeriod = 2.seconds,
            ticker = SchedulerTicker(testScheduler),
        )
        assertEquals(0.milliseconds, limiter.acquire())

        val second = async { limiter.acquire() }
        runCurrent()
        advanceTimeBy(1_249)
        runCurrent()
        assertFalse(second.isCompleted)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(1_250.milliseconds, second.await())
    }

    @Test
    fun negativeTimeoutActsAsImmediateTry() = runTest {
        val limiter = CoroutineRateLimiter.create(1.0, SchedulerTicker(testScheduler))
        assertTrue(limiter.tryAcquire((-1).milliseconds))
        assertFalse(limiter.tryAcquire((-1).milliseconds))
    }

    private class SchedulerTicker(
        private val scheduler: TestCoroutineScheduler,
    ) : Ticker() {
        override fun read(): Long = scheduler.currentTime * 1_000_000L
    }
}
