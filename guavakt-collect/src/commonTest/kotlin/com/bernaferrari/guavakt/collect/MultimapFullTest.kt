package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultimapFullTest {
    @Test
    fun arrayListMultimap_putGetSizeKeys() {
        val m = ArrayListMultimap.create<String, Int>()
        assertTrue(m.put("a", 1))
        assertTrue(m.put("a", 2))
        assertTrue(m.put("b", 3))
        assertEquals(3, m.size())
        assertEquals(listOf(1, 2), m.get("a").toList())
        assertEquals(2, m.keys().count("a"))
        assertEquals(1, m.keys().count("b"))
        assertEquals(setOf("a", "b"), m.keySet())
    }

    @Test
    fun liveView_add_remove_clear() {
        val m = ArrayListMultimap.create<String, String>()
        m.get("k").add("x")
        m.get("k").add("y")
        assertEquals(2, m.size())
        m.get("k").remove("x")
        assertEquals(1, m.size())
        m.get("k").clear()
        assertEquals(0, m.size())
        assertFalse(m.containsKey("k"))
    }

    @Test
    fun hashMultimap_dedupes() {
        val m = HashMultimap.create<Int, String>()
        assertTrue(m.put(1, "a"))
        assertFalse(m.put(1, "a"))
        assertTrue(m.put(1, "b"))
        assertEquals(2, m.size())
    }

    @Test
    fun linkedListMultimap_entryOrder() {
        val m = LinkedListMultimap.create<String, Int>()
        m.put("a", 1)
        m.put("b", 2)
        m.put("a", 3)
        assertEquals(listOf("a" to 1, "b" to 2, "a" to 3), m.entries().map { it.key to it.value })
        m.remove("a", 1)
        assertEquals(listOf("b" to 2, "a" to 3), m.entries().map { it.key to it.value })
    }

    @Test
    fun treeMultimap_sortedKeysAndValues() {
        val m = TreeMultimap.create<String, Int>()
        m.put("b", 2)
        m.put("a", 9)
        m.put("a", 1)
        assertEquals(listOf("a", "b"), m.keySet().toList())
        assertEquals(listOf(1, 9), m.get("a").toList())
    }

    @Test
    fun multimaps_forMap_live() {
        val map = linkedMapOf("x" to 1, "y" to 2)
        val mm = Multimaps.forMap(map)
        assertEquals(1, mm.get("x").single())
        mm.put("z", 3)
        assertEquals(3, map["z"])
        mm.remove("x", 1)
        assertFalse(map.containsKey("x"))
    }

    @Test
    fun multimaps_index_and_filter() {
        val indexed = Multimaps.index(listOf("a", "bb", "c")) { it.length }
        assertEquals(listOf("a", "c"), indexed.get(1).toList())
        val filtered = Multimaps.filterKeys(indexed) { it == 1 }
        assertEquals(2, filtered.size())
        assertFalse(filtered.containsKey(2))
    }

    @Test
    fun multimapBuilder_hashKeys_arrayListValues() {
        val m = MultimapBuilder.hashKeys().arrayListValues().build<String, Int>()
        m.put("a", 1)
        m.put("a", 1)
        assertEquals(2, m.size())
        assertEquals(listOf(1, 1), m.get("a").toList())
    }

    @Test
    fun immutableListMultimap_snapshot() {
        val m = ImmutableListMultimap.builder<String, Int>().put("a", 1).put("a", 2).build()
        assertEquals(listOf(1, 2), m.get("a").toList())
        assertEquals(2, m.keys().count("a"))
    }
}
