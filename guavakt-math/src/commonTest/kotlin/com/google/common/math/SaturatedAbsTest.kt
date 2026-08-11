package dev.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals

class SaturatedAbsTest {
    @Test
    fun intMinimumSaturatesAndOtherValuesUseAbsoluteValue() {
        assertEquals(Int.MAX_VALUE, IntMath.saturatedAbs(Int.MIN_VALUE))
        assertEquals(Int.MAX_VALUE, IntMath.saturatedAbs(Int.MAX_VALUE))
        assertEquals(10, IntMath.saturatedAbs(-10))
        assertEquals(0, IntMath.saturatedAbs(0))
    }

    @Test
    fun longMinimumSaturatesAndOtherValuesUseAbsoluteValue() {
        assertEquals(Long.MAX_VALUE, LongMath.saturatedAbs(Long.MIN_VALUE))
        assertEquals(Long.MAX_VALUE, LongMath.saturatedAbs(Long.MAX_VALUE))
        assertEquals(10L, LongMath.saturatedAbs(-10))
        assertEquals(0L, LongMath.saturatedAbs(0))
    }

    @Test
    fun longSaturatedArithmeticClampsAtBothBoundaries() {
        assertEquals(Long.MAX_VALUE, LongMath.saturatedSubtract(Long.MAX_VALUE, -1))
        assertEquals(Long.MIN_VALUE, LongMath.saturatedSubtract(Long.MIN_VALUE, 1))
        assertEquals(Long.MAX_VALUE, LongMath.saturatedMultiply(Long.MAX_VALUE, 2))
        assertEquals(Long.MIN_VALUE, LongMath.saturatedMultiply(Long.MIN_VALUE, 2))
    }
}
