package dev.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleMathTest {
    @Test
    fun roundingUsesExplicitTieModesAndRejectsNonRepresentableTargets() {
        assertEquals(3L, DoubleMath.roundToLong(2.5, RoundingMode.HALF_UP))
        assertEquals(2L, DoubleMath.roundToLong(2.5, RoundingMode.HALF_DOWN))
        assertEquals(2L, DoubleMath.roundToLong(2.5, RoundingMode.HALF_EVEN))
        assertEquals(-3L, DoubleMath.roundToLong(-2.5, RoundingMode.HALF_UP))
        assertEquals(-2L, DoubleMath.roundToLong(-2.5, RoundingMode.HALF_DOWN))
        assertFailsWith<ArithmeticException> { DoubleMath.roundToLong(Double.NaN, RoundingMode.DOWN) }
        assertFailsWith<ArithmeticException> { DoubleMath.roundToLong(Long.MAX_VALUE.toDouble(), RoundingMode.DOWN) }
        assertFailsWith<ArithmeticException> { DoubleMath.roundToInt(Int.MAX_VALUE.toDouble() + 1.0, RoundingMode.DOWN) }
    }
}
