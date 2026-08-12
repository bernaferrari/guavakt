package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultimapsTest {
    @Test
    fun newListMultimap_putGet() {
        val mm = Multimaps.newListMultimap<String, Int>()
        assertTrue(mm.put("a", 1))
        assertTrue(mm.put("a", 2))
        assertEquals(listOf(1, 2), mm.get("a"))
        assertEquals(2, mm.size())
    }

    @Test
    fun index_and_forMap() {
        val indexed = Multimaps.index(listOf("aa", "b", "ccc")) { it.length }
        assertEquals(listOf("b"), indexed.get(1))
        assertEquals(listOf("aa"), indexed.get(2))
        val fromMap = Multimaps.forMap(mapOf("x" to 1, "y" to 2))
        assertEquals(setOf(1), fromMap.get("x"))
    }
}
