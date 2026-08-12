package com.bernaferrari.guavakt.math

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions
import kotlin.math.sqrt

@GwtCompatible(emulated = true)
object IntMath {
    fun power(b: Int, k: Int): Int {
        Preconditions.checkArgument(k >= 0, "exponent (%s) must be >= 0", k)
        when (b) {
            0 -> return if (k == 0) 1 else 0
            1 -> return 1
            -1 -> return if (k and 1 == 0) 1 else -1
            2 -> return if (k < Int.SIZE_BITS) 1 shl k else 0
            -2 -> return when {
                k < Int.SIZE_BITS -> if (k and 1 == 0) 1 shl k else -(1 shl k)
                else -> 0
            }
        }
        var accum = 1
        var base = b
        var exp = k
        while (true) {
            when (exp) {
                0 -> return accum
                1 -> return base * accum
            }
            if (exp and 1 != 0) accum *= base
            exp = exp shr 1
            if (exp > 0) {
                base *= base
            }
        }
    }

    /** Guava's `checkedPow`, named for Kotlin consistency with [power]. */
    fun checkedPower(b: Int, k: Int): Int {
        Preconditions.checkArgument(k >= 0, "exponent (%s) must be >= 0", k)
        when (b) {
            0 -> return if (k == 0) 1 else 0
            1 -> return 1
            -1 -> return if (k and 1 == 0) 1 else -1
            2 -> {
                if (k >= Int.SIZE_BITS - 1) throw ArithmeticException("overflow: checkedPower($b, $k)")
                return 1 shl k
            }
            -2 -> {
                if (k >= Int.SIZE_BITS) throw ArithmeticException("overflow: checkedPower($b, $k)")
                return if (k and 1 == 0) 1 shl k else -(1 shl k)
            }
        }
        var accum = 1
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
    fun saturatedPower(b: Int, k: Int): Int {
        Preconditions.checkArgument(k >= 0, "exponent (%s) must be >= 0", k)
        when (b) {
            0 -> return if (k == 0) 1 else 0
            1 -> return 1
            -1 -> return if (k and 1 == 0) 1 else -1
            2 -> return if (k >= Int.SIZE_BITS - 1) Int.MAX_VALUE else 1 shl k
            -2 -> return when {
                k < Int.SIZE_BITS -> if (k and 1 == 0) 1 shl k else -(1 shl k)
                k and 1 == 0 -> Int.MAX_VALUE
                else -> Int.MIN_VALUE
            }
        }
        var accum = 1
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

    fun divide(p: Int, q: Int, mode: RoundingMode): Int {
        Preconditions.checkNotNull(mode)
        if (q == 0) throw ArithmeticException("/ by zero")
        val div = p / q
        val rem = p % q
        if (rem == 0) return div
        val signum = 1 or ((p xor q) shr (Int.SIZE_BITS - 1))
        val increment = when (mode) {
            RoundingMode.UNNECESSARY -> throw ArithmeticException("mode was UNNECESSARY, but rounding was necessary")
            RoundingMode.DOWN -> false
            RoundingMode.UP -> true
            RoundingMode.CEILING -> signum > 0
            RoundingMode.FLOOR -> signum < 0
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val cmpRemToHalfDivisor = (kotlin.math.abs(rem.toLong()) * 2L)
                    .compareTo(kotlin.math.abs(q.toLong()))
                when {
                    cmpRemToHalfDivisor == 0 -> mode == RoundingMode.HALF_UP || (mode == RoundingMode.HALF_EVEN && div and 1 != 0)
                    cmpRemToHalfDivisor > 0 -> true
                    else -> false
                }
            }
        }
        return if (increment) div + signum else div
    }

    fun mod(x: Int, m: Int): Int {
        if (m <= 0) throw ArithmeticException("Modulus $m must be > 0")
        val result = x % m
        return if (result >= 0) result else result + m
    }

    fun gcd(a: Int, b: Int): Int {
        Preconditions.checkArgument(a >= 0, "a (%s) must be >= 0", a)
        Preconditions.checkArgument(b >= 0, "b (%s) must be >= 0", b)
        if (a == 0) return b
        if (b == 0) return a
        var a0 = a
        var b0 = b
        val aTwos = a0.countTrailingZeroBits()
        a0 = a0 shr aTwos
        val bTwos = b0.countTrailingZeroBits()
        b0 = b0 shr bTwos
        while (a0 != b0) {
            val delta = a0 - b0
            val minDeltaOrZero = delta and (delta shr (Int.SIZE_BITS - 1))
            a0 = delta - minDeltaOrZero - minDeltaOrZero
            b0 += minDeltaOrZero
            a0 = a0 shr a0.countTrailingZeroBits()
        }
        return a0 shl minOf(aTwos, bTwos)
    }

    fun checkedAdd(a: Int, b: Int): Int {
        val result = a.toLong() + b
        checkNoOverflow(result == result.toInt().toLong(), "checkedAdd", a, b)
        return result.toInt()
    }

    fun checkedSubtract(a: Int, b: Int): Int {
        val result = a.toLong() - b
        checkNoOverflow(result == result.toInt().toLong(), "checkedSubtract", a, b)
        return result.toInt()
    }

    fun checkedMultiply(a: Int, b: Int): Int {
        val result = a.toLong() * b
        checkNoOverflow(result == result.toInt().toLong(), "checkedMultiply", a, b)
        return result.toInt()
    }

    fun saturatedAdd(a: Int, b: Int): Int = (a.toLong() + b).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    fun saturatedSubtract(a: Int, b: Int): Int = (a.toLong() - b).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    fun saturatedMultiply(a: Int, b: Int): Int = (a.toLong() * b).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    /** Absolute value that saturates [Int.MIN_VALUE] to [Int.MAX_VALUE]. */
    fun saturatedAbs(x: Int): Int = if (x == Int.MIN_VALUE) Int.MAX_VALUE else kotlin.math.abs(x)

    fun factorial(n: Int): Int {
        Preconditions.checkArgument(n >= 0, "n (%s) must be >= 0", n)
        return if (n < FACTORIALS.size) FACTORIALS[n] else Int.MAX_VALUE
    }

    fun isPowerOfTwo(x: Int): Boolean = x > 0 && x and (x - 1) == 0

    fun log2(x: Int, mode: RoundingMode): Int {
        MathPreconditions.checkPositive("x", x)
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(isPowerOfTwo(x))
                Int.SIZE_BITS - 1 - x.countLeadingZeroBits()
            }
            RoundingMode.DOWN, RoundingMode.FLOOR -> Int.SIZE_BITS - 1 - x.countLeadingZeroBits()
            RoundingMode.UP, RoundingMode.CEILING -> Int.SIZE_BITS - (x - 1).countLeadingZeroBits()
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val leadingZeros = x.countLeadingZeroBits()
                val cmp = MAX_POWER_OF_SQRT2_UNSIGNED ushr leadingZeros
                val logFloor = Int.SIZE_BITS - 1 - leadingZeros
                logFloor + lessThanBranchFree(cmp, x)
            }
        }
    }

    fun sqrt(x: Int, mode: RoundingMode): Int {
        MathPreconditions.checkNonNegative("x", x)
        val sqrtFloor = sqrt(x.toDouble()).toInt()
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                MathPreconditions.checkRoundingUnnecessary(sqrtFloor * sqrtFloor == x)
                sqrtFloor
            }
            RoundingMode.FLOOR, RoundingMode.DOWN -> sqrtFloor
            RoundingMode.CEILING, RoundingMode.UP -> sqrtFloor + lessThanBranchFree(sqrtFloor * sqrtFloor, x)
            RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN -> {
                val halfSquare = sqrtFloor * sqrtFloor + sqrtFloor
                sqrtFloor + lessThanBranchFree(halfSquare, x)
            }
        }
    }

    private fun lessThanBranchFree(x: Int, y: Int): Int = (x - y) ushr (Int.SIZE_BITS - 1)

    private fun checkNoOverflow(condition: Boolean, methodName: String, a: Int, b: Int) {
        if (!condition) throw ArithmeticException("overflow: $methodName($a, $b)")
    }

    private const val MAX_POWER_OF_SQRT2_UNSIGNED = -0x4afb0ccd
    private val FACTORIALS = intArrayOf(
        1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600,
    )
}
