package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectParityTest {
    @Test
    fun hashMultimap_dedupesValues() {
        val mm = HashMultimap.create<String, Int>()
        assertTrue(mm.put("a", 1))
        assertFalse(mm.put("a", 1))
        assertTrue(mm.put("a", 2))
        assertEquals(2, mm.size())
        assertEquals(setOf(1, 2), mm.get("a"))
    }

    @Test
    fun multiset_counts() {
        val ms = HashMultiset.create(listOf("a", "b", "a"))
        assertEquals(2, ms.count("a"))
        assertEquals(2, ms.setCount("a", 5))
        assertEquals(5, ms.count("a"))
        assertEquals(6, ms.size)
    }

    @Test
    fun table_putGet() {
        val t = HashBasedTable.create<String, String, Int>()
        t.put("r1", "c1", 10)
        assertEquals(10, t.get("r1", "c1"))
        assertEquals(mapOf("c1" to 10), t.row("r1"))
        assertEquals(1, t.size())
    }

    @Test
    fun immutableMap_andMultimap() {
        val map = ImmutableMap.builder<String, Int>().put("x", 1).put("y", 2).build()
        assertEquals(1, map["x"])
        val imm = ImmutableListMultimap.builder<String, Int>().put("a", 1).put("a", 2).build()
        assertEquals(listOf(1, 2), imm.get("a"))
    }

    @Test
    fun range_contains() {
        val r = Range.closedOpen(1, 5)
        assertTrue(r.contains(1))
        assertTrue(r.contains(4))
        assertFalse(r.contains(5))
        assertTrue(Range.closed(1, 1).contains(1))
    }

    @Test
    fun range_isConnected_and_intersection() {
        val a = Range.closed(1, 2)
        val b = Range.closed(3, 4)
        assertFalse(a.isConnected(b), "disjoint closed ranges must not be connected")
        assertFalse(b.isConnected(a))

        val c = Range.closed(2, 5)
        assertTrue(a.isConnected(c), "overlapping / touching at 2 must be connected")
        assertTrue(c.isConnected(a))

        val touch = Range.closed(2, 3)
        assertTrue(a.isConnected(touch))

        val inter = a.intersection(c)
        assertTrue(inter.contains(2))
        assertFalse(inter.contains(1) && !a.contains(1)) // 1 in a but not in intersection with [2,5]
        assertFalse(inter.contains(1))
        assertTrue(inter.contains(2))
    }

    @Test
    fun ordering_and_comparisonChain() {
        val ord: Ordering<Int> = Ordering.natural<Int>().reverse()
        assertEquals(listOf(3, 2, 1), ord.sortedCopy(listOf(1, 3, 2)))
        val cmp = ComparisonChain.start()
            .compare(1, 2)
            .compare("a", "b")
            .result()
        assertTrue(cmp < 0)
    }

    @Test
    fun iterables_getOnlyElement() {
        assertEquals(7, Iterables.getOnlyElement(listOf(7)))
        assertEquals(listOf(1, 2), Iterables.limit(listOf(1, 2, 3), 2).toList())
    }
}
