package com.bernaferrari.guavakt.math

/** Exact decimal-to-`Double` rounding for common [BigDecimal] values. */
object BigDecimalMath {
    /** Guava-shaped overload using the portable mirror of `java.math.RoundingMode`. */
    fun roundToDouble(value: BigDecimal, mode: RoundingMode): Double =
        roundToDouble(value, RoundingModeLike.valueOf(mode.name))

    fun roundToDouble(value: BigDecimal, mode: RoundingModeLike): Double {
        if (value.unscaledValue.isZero) return 0.0
        // Scientific notation avoids materializing scale-dependent zero runs before delegating to
        // the platform's IEEE decimal parser.
        val candidate = "${value.unscaledValue}e${-value.scale}".toDouble()
        if (candidate == 0.0) return roundUnderflow(value, mode)
        if (!candidate.isFinite()) return roundOverflow(value, mode)

        val absoluteComparison = compareDecimalToBinary(
            value.unscaledValue.abs(),
            value.scale,
            binaryMagnitude(abs(candidate)),
        )
        val comparison = if (value.unscaledValue.signum > 0) absoluteComparison else -absoluteComparison
        if (comparison == 0) return candidate

        return when (mode) {
            RoundingModeLike.UNNECESSARY -> throw ArithmeticException("Rounding was necessary")
            RoundingModeLike.FLOOR -> if (comparison > 0) candidate else DoubleUtils.nextDown(candidate)
            RoundingModeLike.CEILING -> if (comparison < 0) candidate else DoubleUtils.nextUp(candidate)
            RoundingModeLike.DOWN -> if (value.unscaledValue.signum > 0) {
                if (comparison > 0) candidate else DoubleUtils.nextDown(candidate)
            } else {
                if (comparison < 0) candidate else DoubleUtils.nextUp(candidate)
            }
            RoundingModeLike.UP -> if (value.unscaledValue.signum > 0) {
                if (comparison < 0) candidate else DoubleUtils.nextUp(candidate)
            } else {
                if (comparison > 0) candidate else DoubleUtils.nextDown(candidate)
            }
            RoundingModeLike.HALF_EVEN -> candidate
            RoundingModeLike.HALF_UP, RoundingModeLike.HALF_DOWN -> {
                val other = if (comparison > 0) DoubleUtils.nextUp(candidate) else DoubleUtils.nextDown(candidate)
                if (!isExactlyHalfway(value.unscaledValue.abs(), value.scale, candidate, other)) return candidate
                val lower = minOf(candidate, other)
                val upper = maxOf(candidate, other)
                if (mode == RoundingModeLike.HALF_UP) {
                    if (value.unscaledValue.signum > 0) upper else lower
                } else {
                    if (value.unscaledValue.signum > 0) lower else upper
                }
            }
        }
    }

    /** Compatibility overload for the old `unscaled × 10^-scale` helper. */
    fun roundToDouble(unscaled: Long, scale: Int, mode: RoundingMode): Double =
        roundToDouble(unscaled, scale, RoundingModeLike.valueOf(mode.name))

    /** Compatibility overload for the old `unscaled × 10^-scale` helper. */
    fun roundToDouble(unscaled: Long, scale: Int, mode: RoundingModeLike): Double =
        roundToDouble(BigDecimal.of(BigInteger.of(unscaled), scale), mode)

    private fun roundUnderflow(value: BigDecimal, mode: RoundingModeLike): Double {
        if (mode == RoundingModeLike.UNNECESSARY) throw ArithmeticException("Rounding was necessary")
        val sign = value.unscaledValue.signum
        val minimum = if (sign > 0) Double.MIN_VALUE else -Double.MIN_VALUE
        return when (mode) {
            RoundingModeLike.FLOOR -> if (sign > 0) 0.0 else minimum
            RoundingModeLike.CEILING -> if (sign > 0) minimum else -0.0
            RoundingModeLike.DOWN -> if (sign > 0) 0.0 else -0.0
            RoundingModeLike.UP -> minimum
            RoundingModeLike.HALF_EVEN, RoundingModeLike.HALF_DOWN, RoundingModeLike.HALF_UP -> {
                val halfComparison = compareDecimalToBinary(
                    value.unscaledValue.abs() * BigInteger.TWO,
                    value.scale,
                    BinaryMagnitude(BigInteger.ONE, -1074),
                )
                when {
                    halfComparison < 0 -> if (sign > 0) 0.0 else -0.0
                    halfComparison > 0 -> minimum
                    mode == RoundingModeLike.HALF_UP -> minimum
                    else -> if (sign > 0) 0.0 else -0.0
                }
            }
            RoundingModeLike.UNNECESSARY -> error("handled above")
        }
    }

    private fun roundOverflow(value: BigDecimal, mode: RoundingModeLike): Double {
        if (mode == RoundingModeLike.UNNECESSARY) throw ArithmeticException("Rounding was necessary")
        val positive = value.unscaledValue.signum > 0
        val max = if (positive) Double.MAX_VALUE else -Double.MAX_VALUE
        val infinity = if (positive) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
        return when (mode) {
            RoundingModeLike.FLOOR -> if (positive) max else infinity
            RoundingModeLike.CEILING -> if (positive) infinity else max
            RoundingModeLike.DOWN -> max
            RoundingModeLike.UP -> infinity
            // Guava rounds finite BigDecimal inputs to the largest finite Double for nearest modes
            // once the decimal parser's candidate has overflowed.
            RoundingModeLike.HALF_EVEN, RoundingModeLike.HALF_DOWN, RoundingModeLike.HALF_UP -> max
            RoundingModeLike.UNNECESSARY -> error("handled above")
        }
    }

    private fun isExactlyHalfway(unscaled: BigInteger, scale: Int, first: Double, second: Double): Boolean {
        val firstMagnitude = binaryMagnitude(abs(first))
        val secondMagnitude = binaryMagnitude(abs(second))
        val exponent = minOf(firstMagnitude.exponent, secondMagnitude.exponent)
        val midpointTwice =
            firstMagnitude.significand.shiftLeft(firstMagnitude.exponent - exponent) +
                secondMagnitude.significand.shiftLeft(secondMagnitude.exponent - exponent)
        return compareDecimalToBinary(unscaled * BigInteger.TWO, scale, BinaryMagnitude(midpointTwice, exponent)) == 0
    }

    /** Compares `unscaled × 10^-scale` with a positive exact binary value. */
    private fun compareDecimalToBinary(unscaled: BigInteger, scale: Int, binary: BinaryMagnitude): Int {
        var left = unscaled
        var right = binary.significand
        if (scale < 0) left *= BigInteger.TEN.pow(-scale) else right *= BigInteger.TEN.pow(scale)
        if (binary.exponent < 0) left = left.shiftLeft(-binary.exponent) else right = right.shiftLeft(binary.exponent)
        return left.compareTo(right)
    }

    private fun binaryMagnitude(value: Double): BinaryMagnitude {
        val bits = value.toRawBits()
        val exponentBits = ((bits ushr FRACTION_BITS) and 0x7ffL).toInt()
        val fraction = bits and FRACTION_MASK
        return if (exponentBits == 0) {
            BinaryMagnitude(BigInteger.of(fraction), -1074)
        } else {
            BinaryMagnitude(BigInteger.of(fraction or IMPLICIT_BIT), exponentBits - EXPONENT_BIAS - FRACTION_BITS)
        }
    }

    private fun abs(value: Double): Double = if (value < 0.0) -value else value

    private data class BinaryMagnitude(val significand: BigInteger, val exponent: Int)

    private const val FRACTION_BITS = 52
    private const val EXPONENT_BIAS = 1023
    private const val FRACTION_MASK = 0x000fffffffffffffL
    private const val IMPLICIT_BIT = 0x0010000000000000L
}
