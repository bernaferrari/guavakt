package dev.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IntMathTest {

    @Test
    fun powerUsesUncheckedTwoComplementOverflow() {
        val base = 46_341
        assertEquals(base * base, IntMath.power(base, 2))
        assertEquals((-base) * (-base), IntMath.power(-base, 2))
        assertEquals(Int.MIN_VALUE, IntMath.power(2, 31))
        assertEquals(0, IntMath.power(2, 32))
        assertEquals(Int.MIN_VALUE, IntMath.checkedPower(-2, 31))
        assertFailsWith<ArithmeticException> { IntMath.checkedPower(2, 31) }
        assertEquals(Int.MAX_VALUE, IntMath.saturatedPower(2, 31))
        assertEquals(Int.MIN_VALUE, IntMath.saturatedPower(-2, 33))
    }

    @Test
    fun gcd() {
        assertEquals(6, IntMath.gcd(54, 24))
    }

    @Test
    fun checkedAdd_overflow() {
        assertFailsWith<ArithmeticException> { IntMath.checkedAdd(Int.MAX_VALUE, 1) }
    }

    @Test
    fun isPowerOfTwo() {
        assertTrue(IntMath.isPowerOfTwo(16))
        assertTrue(!IntMath.isPowerOfTwo(12))
    }

    @Test
    fun mod() {
        assertEquals(1, IntMath.mod(-5, 3))
    }
}
