package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertTrue

class InternersTest {
    @Test
    fun strongInterner_identity() {
        val interner = Interners.newStrongInterner<String>()
        val a = interner.intern("hello")
        val b = interner.intern(StringBuilder("hello").toString())
        assertTrue(a === b)
    }
}
