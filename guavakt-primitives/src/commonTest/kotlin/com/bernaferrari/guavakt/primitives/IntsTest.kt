package com.bernaferrari.guavakt.primitives

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntsTest {
    @Test
    fun minMax() {
        assertEquals(1, Ints.min(intArrayOf(3, 1, 2)))
        assertEquals(3, Ints.max(intArrayOf(3, 1, 2)))
    }

    @Test
    fun saturatedCast() {
        assertEquals(Int.MAX_VALUE, Ints.saturatedCast(Long.MAX_VALUE))
        assertEquals(Int.MIN_VALUE, Ints.saturatedCast(Long.MIN_VALUE))
    }

    @Test
    fun tryParse() {
        assertEquals(42, Ints.tryParse("42"))
        assertTrue(Ints.tryParse("nope") == null)
    }
}
