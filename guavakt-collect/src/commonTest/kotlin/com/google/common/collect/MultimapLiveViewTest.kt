package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultimapLiveViewTest {
    @Test
    fun arrayListMultimapGetIsLiveView() {
        val m = ArrayListMultimap.create<String, Int>()
        val view = m.get("a")
        assertTrue(view is MutableList)
        (view as MutableList).add(1)
        (view as MutableList).add(2)
        assertEquals(2, m.size())
        assertTrue(m.containsEntry("a", 1))
        assertEquals(listOf(1, 2), m.get("a").toList())
    }

    @Test
    fun arrayListMultimapRemoveThroughView() {
        val m = ArrayListMultimap.create<String, Int>()
        m.put("a", 1)
        m.put("a", 2)
        val view = m.get("a") as MutableList
        view.remove(1)
        assertEquals(1, m.size())
        assertFalse(m.containsEntry("a", 1))
        assertTrue(m.containsEntry("a", 2))
    }

    @Test
    fun hashMultimapGetIsLiveSetView() {
        val m = HashMultimap.create<String, Int>()
        val view = m.get("k") as MutableSet
        assertTrue(view.add(1))
        assertFalse(view.add(1))
        assertEquals(1, m.size())
        assertTrue(m.containsEntry("k", 1))
    }

    @Test
    fun keySetRemoveClearsValues() {
        val m = ArrayListMultimap.create<String, Int>()
        m.put("a", 1)
        m.put("a", 2)
        m.put("b", 3)
        assertTrue((m.keySet() as MutableSet).remove("a"))
        assertEquals(1, m.size())
        assertFalse(m.containsKey("a"))
        assertTrue(m.containsEntry("b", 3))
    }
}
