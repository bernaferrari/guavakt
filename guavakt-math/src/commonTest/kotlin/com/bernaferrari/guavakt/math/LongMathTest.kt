package com.bernaferrari.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongMathTest {
    @Test
    fun powersRoundingAndSaturatingCombinatoricsHandleLongBoundaries() {
        assertEquals(1L shl 62, LongMath.ceilingPowerOfTwo((1L shl 62) - 1L))
        assertEquals(1L shl 62, LongMath.floorPowerOfTwo(Long.MAX_VALUE))
        assertFailsWith<ArithmeticException> { LongMath.ceilingPowerOfTwo((1L shl 62) + 1L) }
        assertEquals(3_037_000_499L, LongMath.sqrt(3_037_000_499L * 3_037_000_499L, RoundingMode.UNNECESSARY))
        assertEquals(-4L, LongMath.divide(-17L, 5L, RoundingMode.FLOOR))
        assertEquals(Long.MAX_VALUE, LongMath.factorial(21))
        assertEquals(Long.MAX_VALUE, LongMath.binomial(67, 33))
        assertEquals(Long.MIN_VALUE, LongMath.checkedPower(-2L, 63))
        assertFailsWith<ArithmeticException> { LongMath.checkedPower(2L, 63) }
        assertEquals(Long.MAX_VALUE, LongMath.saturatedPower(2L, 63))
        assertEquals(Long.MIN_VALUE, LongMath.saturatedPower(-2L, 65))
    }

    @Test
    fun meanAvoidsSignedAdditionOverflow() {
        assertEquals(-1L, LongMath.mean(Long.MIN_VALUE, Long.MAX_VALUE))
        assertEquals(-1L, LongMath.mean(-1L, 0L))
    }

    @Test
    fun primalityUsesTheFullLongRangeAlgorithmAndRejectsNegativeInput() {
        assertEquals(true, LongMath.isPrime(2L))
        assertEquals(true, LongMath.isPrime(2_305_843_009_213_693_951L))
        assertEquals(false, LongMath.isPrime(341_550_071_728_321L))
        assertFailsWith<IllegalArgumentException> { LongMath.isPrime(-1L) }
    }
}
