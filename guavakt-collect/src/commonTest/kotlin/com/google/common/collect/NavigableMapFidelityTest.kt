package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigableMapFidelityTest {
    @Test
    fun abstractNavigableMap_orderAndNav() {
        val m = AbstractNavigableMap.create<Int, String>()
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
        assertNull(m.lowerKey(1))
    }
}
