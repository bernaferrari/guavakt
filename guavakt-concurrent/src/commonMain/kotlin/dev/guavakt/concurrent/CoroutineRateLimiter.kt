package dev.guavakt.concurrent

import dev.guavakt.base.Ticker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * A suspending smooth rate limiter for Kotlin Multiplatform.
 *
 * Reservations are serialized per limiter, while waits suspend rather than occupy a thread.
 * Bursty limiters retain at most one second of unused permits. Warm-up limiters move gradually
 * from a cold interval to the configured rate.
 *
 * Cancelling a wait does not refund its reservation: refunding could let later reservations exceed
 * the configured rate. A coroutine cancelled before reservation is never charged.
 */
class CoroutineRateLimiter private constructor(
    private val ticker: Ticker,
    private val state: SmoothRateState,
) {
    private val mutex = Mutex()
    private var completedAcquisitionCount = 0L
    private var acquiredPermitCount = 0L
    private var cancelledAcquisitionCount = 0L
    private var cancelledPermitCount = 0L
    private var rejectedAcquisitionCount = 0L
    private var totalReservedWaitTimeNanos = 0L

    suspend fun setRate(permitsPerSecond: Double) {
        mutex.withLock { state.setRate(permitsPerSecond, ticker.read()) }
    }

    suspend fun rate(): Double = mutex.withLock { state.rate() }

    suspend fun acquire(): Duration = acquire(1)

    /** Reserves [permits], suspends until available, and returns the scheduled wait. */
    suspend fun acquire(permits: Int): Duration {
        validatePermits(permits)
        currentCoroutineContext().ensureActive()
        val waitNanos = mutex.withLock {
            state.reserve(permits, ticker.read()).also(::recordReservation)
        }
        awaitReservation(permits, waitNanos)
        return waitNanos.nanoseconds
    }

    suspend fun tryAcquire(): Boolean = tryAcquire(1, Duration.ZERO)

    suspend fun tryAcquire(permits: Int): Boolean = tryAcquire(permits, Duration.ZERO)

    suspend fun tryAcquire(timeout: Duration): Boolean = tryAcquire(1, timeout)

    /** Reserves only when capacity is available within [timeout], then suspends for that wait. */
    suspend fun tryAcquire(permits: Int, timeout: Duration): Boolean {
        validatePermits(permits)
        currentCoroutineContext().ensureActive()
        val timeoutNanos = if (timeout.isNegative()) 0L else timeout.inWholeNanoseconds
        val waitNanos = mutex.withLock {
            val now = ticker.read()
            if (!state.canAcquire(now, timeoutNanos)) {
                rejectedAcquisitionCount = saturatedIncrement(rejectedAcquisitionCount)
                return@withLock null
            }
            state.reserve(permits, now).also(::recordReservation)
        } ?: return false
        awaitReservation(permits, waitNanos)
        return true
    }

    suspend fun stats(): CoroutineRateLimiterStats = mutex.withLock {
        CoroutineRateLimiterStats(
            completedAcquisitionCount = completedAcquisitionCount,
            acquiredPermitCount = acquiredPermitCount,
            cancelledAcquisitionCount = cancelledAcquisitionCount,
            cancelledPermitCount = cancelledPermitCount,
            rejectedAcquisitionCount = rejectedAcquisitionCount,
            totalReservedWaitTimeNanos = totalReservedWaitTimeNanos,
        )
    }

    private suspend fun awaitReservation(permits: Int, waitNanos: Long) {
        try {
            if (waitNanos > 0L) delay(waitNanos.nanoseconds)
            mutex.withLock {
                completedAcquisitionCount = saturatedIncrement(completedAcquisitionCount)
                acquiredPermitCount = saturatedAdd(acquiredPermitCount, permits.toLong())
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                mutex.withLock {
                    cancelledAcquisitionCount = saturatedIncrement(cancelledAcquisitionCount)
                    cancelledPermitCount = saturatedAdd(cancelledPermitCount, permits.toLong())
                }
            }
            throw cancelled
        }
    }

    private fun recordReservation(waitNanos: Long) {
        totalReservedWaitTimeNanos = saturatedAdd(totalReservedWaitTimeNanos, waitNanos)
    }

    private fun validatePermits(permits: Int) {
        require(permits > 0) { "Requested permits must be positive" }
    }

    private fun saturatedIncrement(value: Long): Long =
        if (value == Long.MAX_VALUE) value else value + 1L

    private fun saturatedAdd(left: Long, right: Long): Long =
        if (right > 0 && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    companion object {
        fun create(permitsPerSecond: Double): CoroutineRateLimiter =
            create(permitsPerSecond, Ticker.systemTicker())

        fun create(permitsPerSecond: Double, ticker: Ticker): CoroutineRateLimiter =
            CoroutineRateLimiter(ticker, SmoothBurstyState(permitsPerSecond, ticker.read()))

        fun create(permitsPerSecond: Double, warmupPeriod: Duration): CoroutineRateLimiter =
            create(permitsPerSecond, warmupPeriod, Ticker.systemTicker())

        fun create(
            permitsPerSecond: Double,
            warmupPeriod: Duration,
            ticker: Ticker,
        ): CoroutineRateLimiter {
            require(!warmupPeriod.isNegative()) { "warmupPeriod must not be negative" }
            val now = ticker.read()
            val state = if (warmupPeriod == Duration.ZERO) {
                SmoothBurstyState(permitsPerSecond, now)
            } else {
                SmoothWarmingUpState(permitsPerSecond, now, warmupPeriod.inWholeNanoseconds)
            }
            return CoroutineRateLimiter(ticker, state)
        }
    }
}
