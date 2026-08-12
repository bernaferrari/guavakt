package com.bernaferrari.guavakt.math

import com.bernaferrari.guavakt.base.Preconditions

/** Exact Guava-shaped arithmetic over common [BigInteger] values. */
object BigIntegerMath {
    fun sqrt(x: BigInteger, mode: RoundingMode): BigInteger {
        Preconditions.checkNotNull(mode)
        MathPreconditions.checkNonNegative("x", x.signum.toLong())
        if (x.isZero) return BigInteger.ZERO

        var low = BigInteger.ZERO
        var high = BigInteger.TWO.pow((x.bitLength() + 1) / 2)
        while (low < high) {
            val middle = (low + high + BigInteger.ONE) / BigInteger.TWO
            if (middle * middle <= x) low = middle else high = middle - BigInteger.ONE
        }
        val floor = low
        val square = floor * floor
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(square == x)
                floor
            }
            RoundingMode.FLOOR, RoundingMode.DOWN -> floor
            RoundingMode.CEILING, RoundingMode.UP -> if (square == x) floor else floor + BigInteger.ONE
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN ->
                if (x <= square + floor) floor else floor + BigInteger.ONE
        }
    }

    fun div(p: BigInteger, q: BigInteger, mode: RoundingMode): BigInteger {
        Preconditions.checkNotNull(mode)
        val (quotient, remainder) = p.divideAndRemainder(q)
        if (remainder.isZero) return quotient
        val signum = if (p.signum == q.signum) 1 else -1
        val increment = when (mode) {
            RoundingMode.UNNECESSARY -> throw ArithmeticException("mode was UNNECESSARY")
            RoundingMode.DOWN -> false
            RoundingMode.UP -> true
            RoundingMode.CEILING -> signum > 0
            RoundingMode.FLOOR -> signum < 0
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val comparison = (remainder.abs() * BigInteger.TWO).compareTo(q.abs())
                when {
                    comparison < 0 -> false
                    comparison > 0 -> true
                    mode == RoundingMode.HALF_UP -> true
                    mode == RoundingMode.HALF_DOWN -> false
                    else -> !quotient.isEven()
                }
            }
        }
        return if (increment) quotient + BigInteger.of(signum) else quotient
    }

    fun factorial(n: Int): BigInteger {
        Preconditions.checkArgument(n >= 0)
        var result = BigInteger.ONE
        for (value in 2..n) result *= value
        return result
    }

    fun binomial(n: Int, k: Int): BigInteger {
        Preconditions.checkArgument(n >= 0)
        Preconditions.checkArgument(k >= 0)
        Preconditions.checkArgument(k <= n)
        val reducedK = minOf(k, n - k)
        var result = BigInteger.ONE
        for (i in 1..reducedK) result = (result * (n - reducedK + i)) / BigInteger.of(i)
        return result
    }

    fun log2(x: BigInteger, mode: RoundingMode): Int {
        Preconditions.checkNotNull(mode)
        Preconditions.checkArgument(x.signum > 0, "x ($x) must be > 0")
        val floor = x.bitLength() - 1
        val floorPower = BigInteger.TWO.pow(floor)
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(x == floorPower)
                floor
            }
            RoundingMode.FLOOR, RoundingMode.DOWN -> floor
            RoundingMode.CEILING, RoundingMode.UP -> if (x == floorPower) floor else floor + 1
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val threshold = BigInteger.TWO.pow(2 * floor + 1)
                floor + if (x * x > threshold) 1 else 0
            }
        }
    }

    /** Long compatibility overload retained for Kotlin callers of the earlier portable tier. */
    fun sqrt(x: Long, mode: RoundingMode): Long = sqrt(BigInteger.of(x), mode).toLongExact()

    /** Long compatibility overload retained for Kotlin callers of the earlier portable tier. */
    fun div(p: Long, q: Long, mode: RoundingMode): Long =
        div(BigInteger.of(p), BigInteger.of(q), mode).toLongExact()

    /** Long compatibility overload retained for Kotlin callers of the earlier portable tier. */
    fun factorialLong(n: Int): Long = factorial(n).toLongExact()

    /** Long compatibility overload retained for Kotlin callers of the earlier portable tier. */
    fun binomialLong(n: Int, k: Int): Long = binomial(n, k).toLongExact()

    /** Long compatibility overload retained for Kotlin callers of the earlier portable tier. */
    fun log2(x: Long, mode: RoundingMode): Int = log2(BigInteger.of(x), mode)

    private fun BigInteger.isEven(): Boolean = (this % BigInteger.TWO).isZero
}
