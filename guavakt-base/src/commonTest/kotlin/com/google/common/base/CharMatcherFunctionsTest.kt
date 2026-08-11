package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharMatcherFunctionsTest {
    @Test
    fun charMatcher_digit_remove() {
        val m = CharMatcher.digit()
        assertTrue(m.matches('5'))
        assertFalse(m.matches('a'))
        assertEquals("abc", m.removeFrom("a1b2c3"))
    }

    @Test
    fun functions_compose() {
        val toStringFn: Function<Int, String> = Function { it.toString() }
        val id = Functions.identity<Int>()
        val f = Functions.compose(toStringFn, id)
        assertEquals("3", f.apply(3))
    }
}
