package com.bernaferrari.guavakt.math

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions
import kotlin.math.sqrt

@GwtCompatible(emulated = true)
object LongMath {
    fun power(b: Long, k: Int): Long {
        Preconditions.checkArgument(k >= 0, "exponent (%s) must be >= 0", k)
        when (b) {
            0L -> return if (k == 0) 1L else 0L
            1L -> return 1L
            -1L -> return if (k and 1 == 0) 1L else -1L
            2L -> return if (k < Long.SIZE_BITS) 1L shl k else 0L
            -2L -> return when {
                k < Long.SIZE_BITS -> if (k and 1 == 0) 1L shl k else -(1L shl k)
                else -> 0L
            }
        }
        var accum = 1L
        var base = b
        var exponent = k
        while (true) {
            when (exponent) {
                0 -> return accum
                1 -> return base * accum
            }
            if (exponent and 1 != 0) accum *= base
            exponent = exponent shr 1
            if (exponent > 0) base *= base
        }
    }

    /** Guava's `checkedPow`, named for Kotlin consistency with [power]. */
    fun checkedPower(b: Long, k: Int): Long {
        Preconditions.checkArgument(k >= 0, "exponent (%s) must be >= 0", k)
        when (b) {
            0L -> return if (k == 0) 1L else 0L
            1L -> return 1L
            -1L -> return if (k and 1 == 0) 1L else -1L
            2L -> {
                if (k >= Long.SIZE_BITS - 1) throw ArithmeticException("overflow: checkedPower($b, $k)")
                return 1L shl k
            }
            -2L -> {
                if (k >= Long.SIZE_BITS) throw ArithmeticException("overflow: checkedPower($b, $k)")
                return if (k and 1 == 0) 1L shl k else -(1L shl k)
            }
        }
        var accum = 1L
        var base = b
        var exponent = k
        while (true) {
            when (exponent) {
                0 -> return accum
                1 -> return checkedMultiply(accum, base)
            }
            if (exponent and 1 != 0) accum = checkedMultiply(accum, base)
            exponent = exponent shr 1
            if (exponent > 0) base = checkedMultiply(base, base)
        }
    }

    /** Guava's `saturatedPow`, named for Kotlin consistency with [power]. */
    fun saturatedPower(b: Long, k: Int): Long {
        Preconditions.checkArgument(k >= 0, "exponent (%s) must be >= 0", k)
        when (b) {
            0L -> return if (k == 0) 1L else 0L
            1L -> return 1L
            -1L -> return if (k and 1 == 0) 1L else -1L
            2L -> return if (k >= Long.SIZE_BITS - 1) Long.MAX_VALUE else 1L shl k
            -2L -> return when {
                k < Long.SIZE_BITS -> if (k and 1 == 0) 1L shl k else -(1L shl k)
                k and 1 == 0 -> Long.MAX_VALUE
                else -> Long.MIN_VALUE
            }
        }
        var accum = 1L
        var base = b
        var exponent = k
        while (true) {
            when (exponent) {
                0 -> return accum
                1 -> return saturatedMultiply(accum, base)
            }
            if (exponent and 1 != 0) accum = saturatedMultiply(accum, base)
            exponent = exponent shr 1
            if (exponent > 0) base = saturatedMultiply(base, base)
        }
    }

    fun ceilingPowerOfTwo(x: Long): Long {
        MathPreconditions.checkPositive("x", x)
        if (x > MAX_POWER_OF_TWO) throw ArithmeticException("ceilingPowerOfTwo($x) is not representable as a long")
        return 1L shl (Long.SIZE_BITS - (x - 1L).countLeadingZeroBits())
    }

    fun floorPowerOfTwo(x: Long): Long {
        MathPreconditions.checkPositive("x", x)
        return 1L shl (Long.SIZE_BITS - 1 - x.countLeadingZeroBits())
    }

    fun log2(x: Long, mode: RoundingMode): Int {
        if (x <= 0L) throw IllegalArgumentException("x ($x) must be > 0")
        val leadingZeros = x.countLeadingZeroBits()
        val logFloor = Long.SIZE_BITS - 1 - leadingZeros
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(isPowerOfTwo(x))
                logFloor
            }
            RoundingMode.FLOOR, RoundingMode.DOWN -> logFloor
            RoundingMode.CEILING, RoundingMode.UP -> if (isPowerOfTwo(x)) logFloor else logFloor + 1
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val sqrt2Threshold = MAX_POWER_OF_SQRT2_UNSIGNED ushr leadingZeros
                logFloor + if (x > sqrt2Threshold) 1 else 0
            }
        }
    }

    fun log10(x: Long, mode: RoundingMode): Int {
        MathPreconditions.checkPositive("x", x)
        var logFloor = POWERS_OF_10.lastIndex
        while (x < POWERS_OF_10[logFloor]) logFloor--
        val floorPower = POWERS_OF_10[logFloor]
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(x == floorPower)
                logFloor
            }
            RoundingMode.FLOOR, RoundingMode.DOWN -> logFloor
            RoundingMode.CEILING, RoundingMode.UP -> if (x == floorPower) logFloor else logFloor + 1
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN ->
                logFloor + if (x > HALF_POWERS_OF_10[logFloor]) 1 else 0
        }
    }

    fun sqrt(x: Long, mode: RoundingMode): Long {
        MathPreconditions.checkNonNegative("x", x)
        if (x == 0L) return 0L
        var floor = sqrt(x.toDouble()).toLong()
        while (floor > x / floor) floor--
        while (floor + 1L <= x / (floor + 1L)) floor++
        val square = floor * floor
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(square == x)
                floor
            }
            RoundingMode.FLOOR, RoundingMode.DOWN -> floor
            RoundingMode.CEILING, RoundingMode.UP -> if (square == x) floor else floor + 1L
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN ->
                if (x <= square + floor) floor else floor + 1L
        }
    }

    fun divide(p: Long, q: Long, mode: RoundingMode): Long {
        Preconditions.checkNotNull(mode)
        val div = p / q
        val rem = p % q
        if (rem == 0L) return div
        val signum = if ((p xor q) >= 0L) 1L else -1L
        val increment = when (mode) {
            RoundingMode.UNNECESSARY -> throw ArithmeticException("mode was UNNECESSARY, but rounding was necessary")
            RoundingMode.DOWN -> false
            RoundingMode.UP -> true
            RoundingMode.CEILING -> signum > 0L
            RoundingMode.FLOOR -> signum < 0L
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val comparison = (magnitude(rem) * 2u).compareTo(magnitude(q))
                comparison > 0 ||
                    (comparison == 0 && (mode == RoundingMode.HALF_UP ||
                        (mode == RoundingMode.HALF_EVEN && (div and 1L) != 0L)))
            }
        }
        return if (increment) div + signum else div
    }

    fun checkedAdd(a: Long, b: Long): Long {
        val result = a + b
        checkNoOverflow((a xor b) < 0 || (a xor result) >= 0, "checkedAdd", a, b)
        return result
    }

    fun checkedSubtract(a: Long, b: Long): Long {
        val result = a - b
        checkNoOverflow((a xor b) >= 0 || (a xor result) >= 0, "checkedSubtract", a, b)
        return result
    }

    fun checkedMultiply(a: Long, b: Long): Long {
        val overflow = when {
            a == 0L || b == 0L -> false
            a > 0 && b > 0 -> a > Long.MAX_VALUE / b
            a > 0 && b < 0 -> b < Long.MIN_VALUE / a
            a < 0 && b > 0 -> a < Long.MIN_VALUE / b
            else -> a != 0L && b < Long.MAX_VALUE / a
        }
        checkNoOverflow(!overflow, "checkedMultiply", a, b)
        return a * b
    }

    fun saturatedAdd(a: Long, b: Long): Long {
        val naive = a + b
        return if ((a xor b) < 0 || (a xor naive) >= 0) naive
        else Long.MAX_VALUE + ((naive ushr (Long.SIZE_BITS - 1)) xor 1)
    }

    /** Subtracts with Guava's signed-overflow saturation semantics. */
    fun saturatedSubtract(a: Long, b: Long): Long {
        val naive = a - b
        return if ((a xor b) >= 0 || (a xor naive) >= 0) naive
        else Long.MAX_VALUE + ((naive ushr (Long.SIZE_BITS - 1)) xor 1)
    }

    /** Multiplies with Guava's signed-overflow saturation semantics. */
    fun saturatedMultiply(a: Long, b: Long): Long = try {
        checkedMultiply(a, b)
    } catch (_: ArithmeticException) {
        if ((a xor b) >= 0) Long.MAX_VALUE else Long.MIN_VALUE
    }

    /** Absolute value that saturates [Long.MIN_VALUE] to [Long.MAX_VALUE]. */
    fun saturatedAbs(x: Long): Long = if (x == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(x)

    fun mod(x: Long, m: Int): Int {
        return mod(x, m.toLong()).toInt()
    }

    fun mod(x: Long, m: Long): Long {
        if (m <= 0) throw ArithmeticException("Modulus $m must be > 0")
        val result = x % m
        return if (result >= 0) result else result + m
    }

    fun gcd(a: Long, b: Long): Long {
        Preconditions.checkArgument(a >= 0, "a (%s) must be >= 0", a)
        Preconditions.checkArgument(b >= 0, "b (%s) must be >= 0", b)
        if (a == 0L) return b
        if (b == 0L) return a
        var a0 = a
        var b0 = b
        val aTwos = a0.countTrailingZeroBits()
        a0 = a0 shr aTwos
        val bTwos = b0.countTrailingZeroBits()
        b0 = b0 shr bTwos
        while (a0 != b0) {
            val delta = a0 - b0
            val minDeltaOrZero = delta and (delta shr (Long.SIZE_BITS - 1))
            a0 = delta - minDeltaOrZero - minDeltaOrZero
            b0 += minDeltaOrZero
            a0 = a0 shr a0.countTrailingZeroBits()
        }
        return a0 shl minOf(aTwos, bTwos)
    }

    fun isPowerOfTwo(x: Long): Boolean = x > 0 && x and (x - 1) == 0L

    fun factorial(n: Int): Long {
        Preconditions.checkArgument(n >= 0, "n (%s) must be >= 0", n)
        return if (n < FACTORIALS.size) FACTORIALS[n] else Long.MAX_VALUE
    }

    fun binomial(n: Int, k: Int): Long {
        Preconditions.checkArgument(n >= 0, "n (%s) must be >= 0", n)
        Preconditions.checkArgument(k >= 0, "k (%s) must be >= 0", k)
        Preconditions.checkArgument(k <= n, "k (%s) must be <= n (%s)", k, n)
        val reducedK = minOf(k, n - k)
        var result = 1L
        for (i in 1..reducedK) {
            var numerator = (n - reducedK + i).toLong()
            var denominator = i.toLong()
            val numeratorDivisor = positiveGcd(numerator, denominator)
            numerator /= numeratorDivisor
            denominator /= numeratorDivisor
            val resultDivisor = positiveGcd(result, denominator)
            result /= resultDivisor
            denominator /= resultDivisor
            check(denominator == 1L)
            if (result > Long.MAX_VALUE / numerator) return Long.MAX_VALUE
            result *= numerator
        }
        return result
    }

    fun mean(x: Long, y: Long): Long = (x and y) + ((x xor y) shr 1)

    /** Deterministic Miller-Rabin for every positive signed [Long]. */
    fun isPrime(n: Long): Boolean {
        MathPreconditions.checkNonNegative("n", n)
        if (n < 2L) return false
        if (n and 1L == 0L) return n == 2L

        val oddPart = (n - 1L) shr (n - 1L).countTrailingZeroBits()
        val squarings = (n - 1L).countTrailingZeroBits()
        for (base in MILLER_RABIN_BASES) {
            val reducedBase = base % n
            if (reducedBase == 0L) continue
            var residue = modularPower(reducedBase, oddPart, n)
            if (residue == 1L || residue == n - 1L) continue
            var foundMinusOne = false
            repeat(squarings - 1) {
                residue = modularMultiply(residue, residue, n)
                if (residue == n - 1L) foundMinusOne = true
            }
            if (!foundMinusOne) return false
        }
        return true
    }

    private fun checkNoOverflow(condition: Boolean, methodName: String, a: Long, b: Long) {
        if (!condition) throw ArithmeticException("overflow: $methodName($a, $b)")
    }

    private fun magnitude(value: Long): ULong =
        if (value >= 0L) value.toULong() else (-(value + 1L)).toULong() + 1u

    private fun positiveGcd(a: Long, b: Long): Long {
        var left = a
        var right = b
        while (right != 0L) {
            val remainder = left % right
            left = right
            right = remainder
        }
        return left
    }

    private fun modularPower(base: Long, exponent: Long, modulus: Long): Long {
        var result = 1L
        var factor = base
        var remaining = exponent
        while (remaining != 0L) {
            if (remaining and 1L != 0L) result = modularMultiply(result, factor, modulus)
            factor = modularMultiply(factor, factor, modulus)
            remaining = remaining ushr 1
        }
        return result
    }

    private fun modularMultiply(left: Long, right: Long, modulus: Long): Long {
        val mod = modulus.toULong()
        var result = 0uL
        var factor = left.toULong()
        var multiplier = right.toULong()
        while (multiplier != 0uL) {
            if (multiplier and 1uL != 0uL) result = modularAdd(result, factor, mod)
            factor = modularAdd(factor, factor, mod)
            multiplier = multiplier shr 1
        }
        return result.toLong()
    }

    private fun modularAdd(left: ULong, right: ULong, modulus: ULong): ULong =
        if (left >= modulus - right) left - (modulus - right) else left + right

    private const val MAX_POWER_OF_TWO = 1L shl (Long.SIZE_BITS - 2)
    private const val MAX_POWER_OF_SQRT2_UNSIGNED = -5402926248376769404L
    private val FACTORIALS = longArrayOf(
        1L,
        1L,
        2L,
        6L,
        24L,
        120L,
        720L,
        5_040L,
        40_320L,
        362_880L,
        3_628_800L,
        39_916_800L,
        479_001_600L,
        6_227_020_800L,
        87_178_291_200L,
        1_307_674_368_000L,
        20_922_789_888_000L,
        355_687_428_096_000L,
        6_402_373_705_728_000L,
        121_645_100_408_832_000L,
        2_432_902_008_176_640_000L,
    )
    private val POWERS_OF_10 = longArrayOf(
        1L,
        10L,
        100L,
        1_000L,
        10_000L,
        100_000L,
        1_000_000L,
        10_000_000L,
        100_000_000L,
        1_000_000_000L,
        10_000_000_000L,
        100_000_000_000L,
        1_000_000_000_000L,
        10_000_000_000_000L,
        100_000_000_000_000L,
        1_000_000_000_000_000L,
        10_000_000_000_000_000L,
        100_000_000_000_000_000L,
        1_000_000_000_000_000_000L,
    )
    private val HALF_POWERS_OF_10 = longArrayOf(
        3L,
        31L,
        316L,
        3_162L,
        31_622L,
        316_227L,
        3_162_277L,
        31_622_776L,
        316_227_766L,
        3_162_277_660L,
        31_622_776_601L,
        316_227_766_016L,
        3_162_277_660_168L,
        31_622_776_601_683L,
        316_227_766_016_837L,
        3_162_277_660_168_379L,
        31_622_776_601_683_793L,
        316_227_766_016_837_933L,
        3_162_277_660_168_379_331L,
    )
    private val MILLER_RABIN_BASES = longArrayOf(2L, 325L, 9_375L, 28_178L, 450_775L, 9_780_504L, 1_795_265_022L)
}
