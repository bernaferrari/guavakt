package dev.guavakt.util.concurrent

import dev.guavakt.base.Ticker
import kotlin.math.round
import kotlin.time.Duration

/**
 * Guava-shaped smooth rate limiter.
 *
 * Blocking waits are real on JVM only. On JS, Wasm, and Native an acquisition that would need to
 * wait throws instead of granting a permit early; zero-wait [tryAcquire] remains usable. New
 * common code should use the suspending [CoroutineRateLimiter].
 */
abstract class RateLimiter protected constructor(
    private val ticker: Ticker,
    initialRate: Double,
    warmupPeriod: Duration? = null,
) {
    /** Retained for source compatibility with custom Guava-shaped subclasses. */
    protected constructor(ticker: Ticker) : this(ticker, 1.0, null)

    private val state: SmoothRateState = createState(initialRate, warmupPeriod, readNanos())

    fun setRate(permitsPerSecond: Double) = monitorSync(this) {
        state.setRate(permitsPerSecond, readNanos())
    }

    fun getRate(): Double = monitorSync(this) { state.rate() }

    fun acquire(): Double = acquire(1)

    fun acquire(permits: Int): Double {
        require(permits > 0) { "Requested permits must be positive" }
        val nanosToWait = monitorSync(this) {
            val now = readNanos()
            rejectPortableWaitIfNeeded(now)
            state.reserve(permits, now)
        }
        sleepNanosUninterruptibly(nanosToWait)
        return nanosToWait / 1_000_000_000.0
    }

    fun tryAcquire(): Boolean = tryAcquire(1)

    fun tryAcquire(permits: Int): Boolean = tryAcquireNanos(permits, 0L)

    fun tryAcquire(timeout: Duration): Boolean = tryAcquire(1, timeout)

    fun tryAcquire(permits: Int, timeout: Duration): Boolean =
        tryAcquireNanos(permits, nonNegativeNanos(timeout))

    /** Guava-shaped compatibility overload whose timeout unit is microseconds. */
    fun tryAcquire(permits: Int, timeoutMicros: Long): Boolean =
        tryAcquireNanos(permits, saturatedMultiply(timeoutMicros.coerceAtLeast(0L), 1_000L))

    private fun tryAcquireNanos(permits: Int, timeoutNanos: Long): Boolean {
        require(permits > 0) { "Requested permits must be positive" }
        val nanosToWait = monitorSync(this) {
            val now = readNanos()
            if (!state.canAcquire(now, timeoutNanos)) return@monitorSync null
            rejectPortableWaitIfNeeded(now)
            state.reserve(permits, now)
        } ?: return false
        sleepNanosUninterruptibly(nanosToWait)
        return true
    }

    /** Refuse before mutating rate state, so callers can switch to the suspending alternative. */
    private fun rejectPortableWaitIfNeeded(now: Long) {
        if (!platformSupportsBlockingWait() && !state.canAcquire(now, 0L)) {
            throw UnsupportedOperationException(
                "RateLimiter cannot block on this target; use CoroutineRateLimiter.acquire",
            )
        }
    }

    private fun sleepNanosUninterruptibly(nanos: Long) {
        if (nanos <= 0L) return
        platformSleepNanosUninterruptibly(nanos)
    }

    private fun readNanos(): Long = ticker.read()

    override fun toString(): String {
        val rounded = round(getRate() * 10.0) / 10.0
        val integral = rounded.toLong()
        val display = if (rounded.isFinite() && rounded == integral.toDouble()) {
            "$integral.0"
        } else {
            rounded.toString()
        }
        return "RateLimiter[stableRate=${display}qps]"
    }

    companion object {
        fun create(permitsPerSecond: Double): RateLimiter =
            create(permitsPerSecond, Ticker.systemTicker())

        fun create(permitsPerSecond: Double, ticker: Ticker): RateLimiter =
            DefaultRateLimiter(ticker, permitsPerSecond, null)

        fun create(permitsPerSecond: Double, warmupPeriod: Duration): RateLimiter =
            create(permitsPerSecond, warmupPeriod, Ticker.systemTicker())

        fun create(
            permitsPerSecond: Double,
            warmupPeriod: Duration,
            ticker: Ticker,
        ): RateLimiter {
            require(!warmupPeriod.isNegative()) { "warmupPeriod must not be negative" }
            return DefaultRateLimiter(ticker, permitsPerSecond, warmupPeriod)
        }

        private fun createState(
            permitsPerSecond: Double,
            warmupPeriod: Duration?,
            nowNanos: Long,
        ): SmoothRateState {
            require(permitsPerSecond > 0.0 && !permitsPerSecond.isNaN()) {
                "rate must be positive"
            }
            return if (warmupPeriod == null || warmupPeriod == Duration.ZERO) {
                SmoothBurstyState(permitsPerSecond, nowNanos)
            } else {
                SmoothWarmingUpState(
                    permitsPerSecond,
                    nowNanos,
                    nonNegativeNanos(warmupPeriod),
                )
            }
        }

        private fun nonNegativeNanos(duration: Duration): Long =
            if (duration.isNegative()) 0L else duration.inWholeNanoseconds

        private fun saturatedMultiply(value: Long, factor: Long): Long =
            if (value > Long.MAX_VALUE / factor) Long.MAX_VALUE else value * factor

    }

    private class DefaultRateLimiter(
        ticker: Ticker,
        rate: Double,
        warmupPeriod: Duration?,
    ) : RateLimiter(ticker, rate, warmupPeriod)
}
