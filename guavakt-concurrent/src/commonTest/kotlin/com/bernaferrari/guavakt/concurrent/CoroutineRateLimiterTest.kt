package com.bernaferrari.guavakt.concurrent

import com.bernaferrari.guavakt.base.Ticker
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
    fun cancelledReservationsRemainChargedToProtectTheRate() = runTest {
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
    }

    @Test
    fun timeoutRejectionDoesNotMutateTheSchedule() = runTest {
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
    fun warmupUsesColdThenStableIntervals() = runTest {
        val limiter = CoroutineRateLimiter.create(2.0, 2.seconds, SchedulerTicker(testScheduler))
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

    private class SchedulerTicker(private val scheduler: TestCoroutineScheduler) : Ticker() {
        override fun read(): Long = scheduler.currentTime * 1_000_000L
    }
}
