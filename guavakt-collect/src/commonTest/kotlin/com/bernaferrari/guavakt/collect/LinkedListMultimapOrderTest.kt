package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkedListMultimapOrderTest {
    @Test
    fun entries_globalInsertionOrder() {
        val mm = LinkedListMultimap.create<String, String>()
        mm.put("k1", "foo")
        mm.put("k2", "bar")
        mm.put("k1", "baz")
        assertEquals(
            listOf("k1" to "foo", "k2" to "bar", "k1" to "baz"),
            mm.entries().map { it.key to it.value },
        )
        mm.remove("k1", "foo")
        assertEquals(
            listOf("k2" to "bar", "k1" to "baz"),
            mm.entries().map { it.key to it.value },
        )
        assertEquals(listOf("k2", "k1"), mm.keySet().toList())
    }

    @Test
    fun get_liveView_add() {
        val mm = LinkedListMultimap.create<String, Int>()
        mm.get("a").add(1)
        mm.get("a").add(2)
        assertEquals(listOf(1, 2), mm.get("a").toList())
        assertEquals(2, mm.size())
    }
}
