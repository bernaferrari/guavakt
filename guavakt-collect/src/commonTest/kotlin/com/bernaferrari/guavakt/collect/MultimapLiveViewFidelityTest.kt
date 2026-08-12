package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultimapLiveViewFidelityTest {
    @Test
    fun arrayListMultimap_getAdd_mutatesMultimap() {
        val mm = ArrayListMultimap.create<String, Int>()
        assertTrue(mm.get("k").add(1))
        assertTrue(mm.get("k").add(2))
        assertEquals(2, mm.size())
        assertEquals(listOf(1, 2), mm.get("k").toList())
    }

    @Test
    fun hashMultimap_getAdd_mutatesMultimap() {
        val mm = HashMultimap.create<String, Int>()
        assertTrue(mm.get("k").add(1))
        assertTrue(!mm.get("k").add(1)) // set semantics
        assertEquals(1, mm.size())
    }

    @Test
    fun removeAll_viaViewClear() {
        val mm = ArrayListMultimap.create<String, Int>()
        mm.put("a", 1)
        mm.put("a", 2)
        mm.get("a").clear()
        assertEquals(0, mm.size())
        assertTrue(mm.get("a").isEmpty())
    }
}
