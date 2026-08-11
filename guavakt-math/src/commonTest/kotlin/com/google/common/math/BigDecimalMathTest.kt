package dev.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BigDecimalMathTest {
    @Test
    fun longDecimalTierUsesExactDirectionalAndHalfRounding() {
        val halfway = 9_007_199_254_740_993L
        assertEquals(9_007_199_254_740_992.0, BigDecimalMath.roundToDouble(halfway, 0, RoundingModeLike.HALF_EVEN))
        assertEquals(9_007_199_254_740_994.0, BigDecimalMath.roundToDouble(halfway, 0, RoundingModeLike.HALF_UP))
        assertEquals(9_007_199_254_740_992.0, BigDecimalMath.roundToDouble(halfway, 0, RoundingModeLike.HALF_DOWN))
        assertEquals(-9_007_199_254_740_994.0, BigDecimalMath.roundToDouble(-halfway, 0, RoundingModeLike.HALF_UP))
        assertEquals(-9_007_199_254_740_992.0, BigDecimalMath.roundToDouble(-halfway, 0, RoundingModeLike.HALF_DOWN))

        assertEquals(0.1, BigDecimalMath.roundToDouble(1, 1, RoundingModeLike.HALF_EVEN))
        assertFailsWith<ArithmeticException> { BigDecimalMath.roundToDouble(1, 1, RoundingModeLike.UNNECESSARY) }
        assertEquals(1.0e-19, BigDecimalMath.roundToDouble(1, 19, RoundingModeLike.HALF_EVEN))
    }

    @Test
    fun arbitraryPrecisionDecimalPreservesScaleAndRoundsWideValues() {
        assertEquals("1.00", BigDecimal.parse("1.00").toString())
        assertEquals(0, BigDecimal.parse("1.00").compareTo(BigDecimal.parse("1.0")))
        assertEquals(1.0e100, BigDecimalMath.roundToDouble(BigDecimal.parse("1e100"), RoundingModeLike.HALF_EVEN))
        assertEquals(Double.MIN_VALUE, BigDecimalMath.roundToDouble(BigDecimal.parse("3e-324"), RoundingModeLike.HALF_UP))
        assertEquals(0.0, BigDecimalMath.roundToDouble(BigDecimal.parse("2e-324"), RoundingModeLike.HALF_EVEN))
        assertEquals(Double.MAX_VALUE, BigDecimalMath.roundToDouble(BigDecimal.parse("1e400"), RoundingModeLike.DOWN))
        assertEquals(Double.POSITIVE_INFINITY, BigDecimalMath.roundToDouble(BigDecimal.parse("1e400"), RoundingModeLike.UP))
    }

    @Test
    fun arbitraryPrecisionDecimalSupportsScalePreservingArithmetic() {
        val left = BigDecimal.parse("100.00")
        val right = BigDecimal.parse("2.0")
        assertEquals("102.00", (left + right).toString())
        assertEquals("98.00", (left - right).toString())
        assertEquals("200.000", (left * right).toString())
        assertEquals("50.0", (left / right).toString())
        assertEquals("0.333", BigDecimal.parse("1").divide(BigDecimal.parse("3"), 3, RoundingMode.HALF_UP).toString())
        assertEquals("1.20", BigDecimal.parse("1.2").setScale(2).toString())
        assertEquals("1.3", BigDecimal.parse("1.25").setScale(1, RoundingMode.HALF_UP).toString())
        assertEquals("1.23E+4", BigDecimal.parse("12300.00").stripTrailingZeros().toString())
        assertEquals("0.000001", BigDecimal.parse("0.000001").toString())
        assertEquals("1E-7", BigDecimal.parse("0.0000001").toString())
        assertEquals(BigInteger.of(12), BigDecimal.parse("12.9").toBigInteger())
        assertFailsWith<ArithmeticException> { BigDecimal.parse("12.9").toBigIntegerExact() }
        assertFailsWith<ArithmeticException> { BigDecimal.parse("1").divide(BigDecimal.parse("3")) }
    }
}
