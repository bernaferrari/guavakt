package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regressions for correctness bugs fixed in review passes. */
class BugFixRegressionTest {
    @Test
    fun forwardingListMultimap_keys() {
        val inner = ArrayListMultimap.create<String, Int>()
        val fwd = object : ForwardingListMultimap<String, Int>() {
            override fun delegate() = inner
        }
        fwd.put("a", 1)
        fwd.put("a", 2)
        fwd.put("b", 3)
        assertEquals(2, fwd.keys().count("a"))
        assertEquals(1, fwd.keys().count("b"))
        assertEquals(3, fwd.keys().size)
    }

    @Test
    fun forwardingSetMultimap_isSetMultimap() {
        val inner = HashMultimap.create<String, Int>()
        val fwd = object : ForwardingSetMultimap<String, Int>() {
            override fun delegate() = inner
        }
        assertTrue(fwd.put("a", 1))
        assertFalse(fwd.put("a", 1))
        assertEquals(setOf(1), fwd.get("a").toSet())
        assertEquals(1, fwd.keys().count("a"))
    }

    @Test
    fun immutableRangeSet_complement_notEmptyWhenNonEmpty() {
        val set = ImmutableRangeSet.of(Range.closed(0, 0))
        val comp = set.complement()
        assertFalse(comp.contains(0))
        assertTrue(comp.contains(-1))
        assertTrue(comp.contains(1))
    }

    @Test
    fun treeRangeMap_remove_respectsBoundTypes() {
        val map = TreeRangeMap.create<Int, String>()
        map.put(Range.closed(1, 10), "x")
        map.remove(Range.closed(4, 6))
        assertEquals("x", map.get(3))
        assertEquals(null, map.get(5))
        assertEquals("x", map.get(7))
    }

    @Test
    fun mapsFilterKeys_iteratorRemove_mutatesBacking() {
        val backing = linkedMapOf("a" to 1, "b" to 2, "aa" to 3)
        val view = Maps.filterKeys(backing) { it.startsWith("a") } as MutableMap
        val it = view.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.key == "a") it.remove()
        }
        assertFalse(backing.containsKey("a"))
        assertTrue(backing.containsKey("aa"))
        assertTrue(backing.containsKey("b"))
    }

    @Test
    fun immutableRangeMap_span_usesRangeSpan() {
        val map = ImmutableRangeMap.builder<Int, String>()
            .put(Range.closed(1, 2), "a")
            .put(Range.closed(5, 6), "b")
            .build()
        assertEquals(Range.closed(1, 6), map.span())
    }

    @Test
    fun forwardingTable_putGet() {
        val inner = HashBasedTable.create<String, String, Int>()
        val fwd = object : ForwardingTable<String, String, Int>() {
            override fun delegate() = inner
        }
        assertEquals(null, fwd.put("r", "c", 1))
        assertEquals(1, fwd.get("r", "c"))
        assertTrue(fwd.contains("r", "c"))
        assertEquals(1, fwd.size())
    }

    @Test
    fun range_rejectsInvertedEndpoints() {
        assertFailsWith<IllegalArgumentException> { Range.closed(5, 3) }
    }

    @Test
    fun hashBiMap_putAll_keepsInverse() {
        val b = HashBiMap.create<String, Int>()
        b.putAll(mapOf("a" to 1, "b" to 2))
        assertEquals("a", b.inverse()[1])
        assertEquals("b", b.inverse()[2])
    }
}
