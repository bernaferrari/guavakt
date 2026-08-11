package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectTest {
    @Test
    fun arrayListMultimap() {
        val mm = ArrayListMultimap.create<String, Int>()
        mm.put("a", 1)
        mm.put("a", 2)
        mm.put("b", 3)
        assertEquals(3, mm.size())
        assertEquals(listOf(1, 2), mm.get("a"))
        assertTrue(mm.containsEntry("b", 3))
    }

    @Test
    fun immutableList() {
        val list = ImmutableList.builder<String>().add("x").add("y").build()
        assertEquals(2, list.size)
        assertEquals("x", list[0])
        val copy = ImmutableList.copyOf(listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), copy.toList())
    }

    @Test
    fun sets_intersection() {
        assertEquals(setOf(2), Sets.intersection(setOf(1, 2), setOf(2, 3)))
    }
}
