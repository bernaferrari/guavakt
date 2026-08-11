package dev.guavakt.concurrent

internal abstract class SmoothRateState(nowNanos: Long) {
    protected var storedPermits = 0.0
    protected var maxPermits = 0.0
    protected var stableIntervalNanos = 0.0
    private var nextFreeTicketNanos = nowNanos

    fun rate(): Double = 1_000_000_000.0 / stableIntervalNanos

    fun setRate(permitsPerSecond: Double, nowNanos: Long) {
        require(permitsPerSecond > 0.0 && !permitsPerSecond.isNaN()) { "rate must be positive" }
        resync(nowNanos)
        stableIntervalNanos = 1_000_000_000.0 / permitsPerSecond
        doSetRate(permitsPerSecond, stableIntervalNanos)
    }

    fun canAcquire(nowNanos: Long, timeoutNanos: Long): Boolean =
        nextFreeTicketNanos - timeoutNanos.coerceAtLeast(0L) <= nowNanos

    fun reserve(permits: Int, nowNanos: Long): Long {
        require(permits > 0) { "Requested permits must be positive" }
        resync(nowNanos)
        val availableAt = nextFreeTicketNanos
        val storedToSpend = minOf(permits.toDouble(), storedPermits)
        val freshPermits = permits - storedToSpend
        val waitForStored = storedPermitsToWaitTime(storedPermits, storedToSpend)
        val waitForFresh = nanosAsLong(freshPermits * stableIntervalNanos)
        nextFreeTicketNanos = saturatedAdd(availableAt, saturatedAdd(waitForStored, waitForFresh))
        storedPermits -= storedToSpend
        return nonNegativeDifference(availableAt, nowNanos)
    }

    protected abstract fun doSetRate(permitsPerSecond: Double, stableIntervalNanos: Double)

    protected abstract fun storedPermitsToWaitTime(storedPermits: Double, permitsToTake: Double): Long

    protected abstract fun coolDownIntervalNanos(): Double

    private fun resync(nowNanos: Long) {
        if (nowNanos <= nextFreeTicketNanos) return
        val coolDown = coolDownIntervalNanos()
        val newPermits = when {
            coolDown == 0.0 -> Double.POSITIVE_INFINITY
            coolDown.isNaN() -> 0.0
            else -> (nowNanos - nextFreeTicketNanos) / coolDown
        }
        storedPermits = minOf(maxPermits, storedPermits + newPermits)
        nextFreeTicketNanos = nowNanos
    }

    protected fun nanosAsLong(nanos: Double): Long = when {
        nanos.isNaN() || nanos <= 0.0 -> 0L
        nanos >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
        else -> nanos.toLong()
    }

    protected fun saturatedAdd(left: Long, right: Long): Long {
        val sum = left + right
        return if ((left xor right) < 0 || (left xor sum) >= 0) sum else Long.MAX_VALUE
    }

    private fun nonNegativeDifference(later: Long, earlier: Long): Long {
        if (later <= earlier) return 0L
        val difference = later - earlier
        return if (difference < 0L) Long.MAX_VALUE else difference
    }
}

internal class SmoothBurstyState(
    permitsPerSecond: Double,
    nowNanos: Long,
    private val maxBurstSeconds: Double = 1.0,
) : SmoothRateState(nowNanos) {
    init {
        setRate(permitsPerSecond, nowNanos)
    }

    override fun doSetRate(permitsPerSecond: Double, stableIntervalNanos: Double) {
        val oldMaxPermits = maxPermits
        maxPermits = maxBurstSeconds * permitsPerSecond
        storedPermits = when {
            permitsPerSecond == Double.POSITIVE_INFINITY -> Double.POSITIVE_INFINITY
            oldMaxPermits == Double.POSITIVE_INFINITY -> maxPermits
            oldMaxPermits == 0.0 -> 0.0
            else -> storedPermits * maxPermits / oldMaxPermits
        }
    }

    override fun storedPermitsToWaitTime(storedPermits: Double, permitsToTake: Double): Long = 0L

    override fun coolDownIntervalNanos(): Double = stableIntervalNanos
}

internal class SmoothWarmingUpState(
    permitsPerSecond: Double,
    nowNanos: Long,
    private val warmupPeriodNanos: Long,
    private val coldFactor: Double = 3.0,
) : SmoothRateState(nowNanos) {
    private var slope = 0.0
    private var thresholdPermits = 0.0

    init {
        setRate(permitsPerSecond, nowNanos)
    }

    override fun doSetRate(permitsPerSecond: Double, stableIntervalNanos: Double) {
        val oldMaxPermits = maxPermits
        if (permitsPerSecond == Double.POSITIVE_INFINITY) {
            thresholdPermits = Double.POSITIVE_INFINITY
            maxPermits = Double.POSITIVE_INFINITY
            slope = 0.0
            storedPermits = Double.POSITIVE_INFINITY
            return
        }

        val coldIntervalNanos = stableIntervalNanos * coldFactor
        thresholdPermits = 0.5 * warmupPeriodNanos / stableIntervalNanos
        maxPermits = thresholdPermits + 2.0 * warmupPeriodNanos / (stableIntervalNanos + coldIntervalNanos)
        slope = if (maxPermits == thresholdPermits) 0.0 else
            (coldIntervalNanos - stableIntervalNanos) / (maxPermits - thresholdPermits)
        storedPermits = when {
            oldMaxPermits == Double.POSITIVE_INFINITY -> 0.0
            oldMaxPermits == 0.0 -> maxPermits
            else -> storedPermits * maxPermits / oldMaxPermits
        }
    }

    override fun storedPermitsToWaitTime(storedPermits: Double, permitsToTake: Double): Long {
        var remaining = permitsToTake
        var waitNanos = 0L
        val availableAboveThreshold = storedPermits - thresholdPermits
        if (availableAboveThreshold > 0.0) {
            val aboveThresholdToTake = minOf(availableAboveThreshold, remaining)
            val start = permitsToTime(availableAboveThreshold)
            val end = permitsToTime(availableAboveThreshold - aboveThresholdToTake)
            waitNanos = nanosAsLong(aboveThresholdToTake * (start + end) / 2.0)
            remaining -= aboveThresholdToTake
        }
        return saturatedAdd(waitNanos, nanosAsLong(remaining * stableIntervalNanos))
    }

    override fun coolDownIntervalNanos(): Double =
        if (maxPermits == 0.0) Double.POSITIVE_INFINITY else warmupPeriodNanos / maxPermits

    private fun permitsToTime(permits: Double): Double = stableIntervalNanos + permits * slope
}
