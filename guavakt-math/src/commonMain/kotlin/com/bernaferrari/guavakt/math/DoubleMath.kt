package com.bernaferrari.guavakt.math

import com.bernaferrari.guavakt.annotations.GwtCompatible
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln

@GwtCompatible(emulated = true)
object DoubleMath {
    fun isMathematicalInteger(x: Double): Boolean = x.isFinite() && x == floor(x)

    fun roundToLong(x: Double, mode: RoundingMode): Long {
        val rounded = roundIntermediate(x, mode)
        // `Long.MAX_VALUE.toDouble()` is 2^63, one value above the largest representable Long.
        if (rounded < Long.MIN_VALUE.toDouble() || rounded >= -Long.MIN_VALUE.toDouble()) {
            throw ArithmeticException("rounded value is out of range for Long: $rounded")
        }
        return rounded.toLong()
    }

    fun roundToInt(x: Double, mode: RoundingMode): Int {
        val l = roundToLong(x, mode)
        if (l !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            throw ArithmeticException("rounded value is out of range for Int: $l")
        }
        return l.toInt()
    }

    fun log2(x: Double): Double {
        require(x > 0 && x.isFinite())
        return ln(x) / ln(2.0)
    }

    /** Guava's `roundIntermediate`, kept in Double form until the target-range check. */
    private fun roundIntermediate(x: Double, mode: RoundingMode): Double {
        if (!x.isFinite()) throw ArithmeticException("input is infinite or NaN")
        return when (mode) {
            RoundingMode.UNNECESSARY -> {
                if (!isMathematicalInteger(x)) throw ArithmeticException("rounding was necessary")
                x
            }
            RoundingMode.FLOOR -> floor(x)
            RoundingMode.CEILING -> ceil(x)
            RoundingMode.DOWN -> if (x >= 0.0) floor(x) else ceil(x)
            RoundingMode.UP -> if (x >= 0.0) ceil(x) else floor(x)
            RoundingMode.HALF_UP,
            RoundingMode.HALF_DOWN,
            RoundingMode.HALF_EVEN
            -> roundHalf(x, mode)
        }
    }

    private fun roundHalf(x: Double, mode: RoundingMode): Double {
        val lower = floor(x)
        val fraction = x - lower
        return when {
            fraction < 0.5 -> lower
            fraction > 0.5 -> lower + 1.0
            mode == RoundingMode.HALF_UP -> if (x >= 0.0) lower + 1.0 else lower
            mode == RoundingMode.HALF_DOWN -> if (x >= 0.0) lower else lower + 1.0
            // At an exact tie, select the even integral neighbor. `lower` is in the target
            // range for every value where its parity matters; extreme values are integral.
            lower.toLong() and 1L == 0L -> lower
            else -> lower + 1.0
        }
    }
}
