package com.bernaferrari.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BigIntegerMathTest {
    @Test
    fun longSizedSqrtAndLog2RoundAtTheirBoundaries() {
        val square = 3_037_000_499L * 3_037_000_499L
        assertEquals(3_037_000_499L, BigIntegerMath.sqrt(square, RoundingMode.UNNECESSARY))
        assertEquals(3_037_000_500L, BigIntegerMath.sqrt(square + 1L, RoundingMode.CEILING))
        assertEquals(63, BigIntegerMath.log2(Long.MAX_VALUE, RoundingMode.CEILING))
        assertEquals(62, BigIntegerMath.log2(1L shl 62, RoundingMode.UNNECESSARY))
        assertFailsWith<ArithmeticException> { BigIntegerMath.sqrt(2L, RoundingMode.UNNECESSARY) }
    }

    @Test
    fun divisionAndBinomialAvoidIntermediateLongOverflow() {
        assertEquals(-4L, BigIntegerMath.div(-17L, 5L, RoundingMode.FLOOR))
        assertEquals(-3L, BigIntegerMath.div(-17L, 5L, RoundingMode.CEILING))
        assertEquals(BigInteger.of(2_145L), BigIntegerMath.binomial(66, 2))
        assertEquals(65, BigIntegerMath.factorial(50).toString().length)
        assertFailsWith<ArithmeticException> { BigIntegerMath.div(Long.MIN_VALUE, -1L, RoundingMode.DOWN) }
    }

    @Test
    fun arbitraryPrecisionParsingArithmeticAndRootsAreExact() {
        val tenToTheFifty = BigInteger.TEN.pow(50)
        val square = tenToTheFifty * tenToTheFifty
        assertEquals(tenToTheFifty, BigIntegerMath.sqrt(square, RoundingMode.UNNECESSARY))
        assertEquals(100, BigIntegerMath.log2(BigInteger.TWO.pow(100), RoundingMode.UNNECESSARY))
        assertEquals(BigInteger.parse("9223372036854775808"), BigInteger.of(Long.MIN_VALUE).abs())
        assertEquals(Long.MIN_VALUE, BigInteger.of(Long.MIN_VALUE).toLongExact())
        assertEquals(42, BigInteger.of(42).toIntExact())
        assertEquals(BigInteger.of(6), BigInteger.of(54).gcd(BigInteger.of(-24)))
        assertEquals("100000000000000000000", (BigInteger.parse("99999999999999999999") + BigInteger.ONE).toString())
    }
}
