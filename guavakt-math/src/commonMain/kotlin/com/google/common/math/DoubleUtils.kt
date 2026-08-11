package dev.guavakt.math

/** Guava DoubleUtils — bit-level double helpers. */
internal object DoubleUtils {
    const val SIGNIFICAND_BITS = 52
    const val EXPONENT_BIAS = 1023
    const val SIGNIFICAND_MASK = 0x000fffffffffffffL
    const val EXPONENT_MASK = 0x7ff0000000000000L
    val SIGN_MASK: Long = 1L shl 63
    const val IMPLICIT_BIT = 0x0010000000000000L
    const val ONE_BITS = 0x3ff0000000000000L

    fun getSignificand(d: Double): Long {
        check(isFinite(d))
        val exponent = getExponent(d)
        var bits = d.toBits()
        bits = bits and SIGNIFICAND_MASK
        return if (exponent == -EXPONENT_BIAS) bits shl 1 else bits or IMPLICIT_BIT
    }

    fun isFinite(d: Double): Boolean = d.toBits() and EXPONENT_MASK != EXPONENT_MASK

    fun getExponent(d: Double): Int =
        (((d.toBits() and EXPONENT_MASK) ushr SIGNIFICAND_BITS) - EXPONENT_BIAS).toInt()

    fun nextDown(d: Double): Double = -nextUp(-d)

    fun nextUp(d: Double): Double {
        if (d.isNaN() || d == Double.POSITIVE_INFINITY) return d
        val bits = d.toBits()
        return if (d >= 0) Double.fromBits(bits + 1) else Double.fromBits(bits - 1)
    }

    fun bigToDouble(x: Long): Double = x.toDouble()
}
