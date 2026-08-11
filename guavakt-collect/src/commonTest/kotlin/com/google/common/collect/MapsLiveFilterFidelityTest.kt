package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapsLiveFilterFidelityTest {
    @Test
    fun filterKeys_liveView_reflectsBackingMutations() {
        val backing = linkedMapOf("a" to 1, "b" to 2, "aa" to 3)
        val view = Maps.filterKeys(backing) { it.startsWith("a") }
        assertEquals(2, view.size)
        assertEquals(1, view["a"])
        backing["ab"] = 4
        assertEquals(4, view["ab"])
        backing.remove("a")
        assertNull(view["a"])
        assertEquals(2, view.size)
    }

    @Test
    fun filterKeys_rejectedPut_throws() {
        val backing = linkedMapOf("a" to 1)
        val view = Maps.filterKeys(backing) { it.startsWith("a") } as MutableMap
        assertFailsWith<IllegalArgumentException> { view["b"] = 2 }
        view["aa"] = 9
        assertEquals(9, backing["aa"])
    }

    @Test
    fun filterValues_liveView() {
        val backing = linkedMapOf("x" to 1, "y" to 2, "z" to 3)
        val view = Maps.filterValues(backing) { it % 2 == 1 }
        assertEquals(2, view.size)
        backing["w"] = 5
        assertEquals(5, view["w"])
        backing["x"] = 10
        assertNull(view["x"])
    }

    @Test
    fun filterEntries_liveView() {
        val backing = linkedMapOf(1 to "a", 2 to "bb", 3 to "c")
        val view = Maps.filterEntries(backing) { it.key > 1 && it.value.length == 1 }
        assertEquals(1, view.size)
        assertEquals("c", view[3])
        backing[4] = "d"
        assertEquals("d", view[4])
    }

    @Test
    fun comparatorTreeMap_navigableOrder() {
        val m = ComparatorTreeMap<Int, String>(null)
        m[3] = "c"
        m[1] = "a"
        m[2] = "b"
        assertEquals(listOf(1, 2, 3), m.keys.toList())
        assertEquals(1, m.firstKey())
        assertEquals(3, m.lastKey())
        assertEquals(1, m.lowerKey(2))
        assertEquals(2, m.floorKey(2))
        assertEquals(2, m.ceilingKey(2))
        assertEquals(3, m.higherKey(2))
        assertEquals("a", m.lowerEntry(2)?.value)
        assertEquals("c", m.higherEntry(2)?.value)
        assertEquals(listOf(1, 2), m.subMap(1, 3).keys.toList())
        assertEquals(listOf(1, 2), m.headMap(3).keys.toList())
        assertEquals(listOf(2, 3), m.tailMap(2).keys.toList())
        assertEquals(listOf(3, 2, 1), m.descendingMap().keys.toList())
    }

    @Test
    fun comparatorTreeMap_reverseComparator() {
        val m = ComparatorTreeMap<Int, String>(compareByDescending { it })
        m[1] = "a"
        m[3] = "c"
        m[2] = "b"
        assertEquals(listOf(3, 2, 1), m.keys.toList())
        assertEquals(3, m.firstKey())
        assertEquals(2, m.ceilingKey(2))
        assertEquals(1, m.higherKey(2))
    }
}
