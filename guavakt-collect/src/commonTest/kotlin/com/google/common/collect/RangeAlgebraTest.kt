package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeAlgebraTest {
    @Test
    fun treeRangeSet_partialRemove_leavesRemnants() {
        val set = TreeRangeSet.create<Int>()
        set.add(Range.closed(1, 10))
        set.remove(Range.closed(4, 6))
        assertTrue(set.contains(3))
        assertFalse(set.contains(5))
        assertTrue(set.contains(7))
        assertTrue(set.contains(1))
        assertTrue(set.contains(10))
    }

    @Test
    fun treeRangeSet_complement_containsOutside() {
        val set = TreeRangeSet.create<Int>()
        set.add(Range.closed(0, 0))
        val comp = set.complement()
        assertFalse(comp.contains(0))
        assertTrue(comp.contains(-1))
        assertTrue(comp.contains(1))
    }

    @Test
    fun treeRangeSet_complementAndSubRangeViewsStayLiveAndWriteThrough() {
        val set = TreeRangeSet.create<Int>()
        set.add(Range.closed(1, 3))
        val complement = set.complement()
        val subRange = set.subRangeSet(Range.closed(2, 6))

        set.add(Range.closed(5, 7))
        assertEquals(listOf(Range.closed(2, 3), Range.closed(5, 6)), subRange.asRanges().toList())
        assertFalse(complement.contains(2))
        assertTrue(complement.contains(4))

        complement.add(Range.closed(2, 5))
        assertEquals(listOf(Range.closedOpen(1, 2), Range.openClosed(5, 7)), set.asRanges().toList())
        assertEquals(listOf(Range.openClosed(5, 6)), subRange.asRanges().toList())
        assertTrue(complement.complement() === set)

        subRange.add(Range.closed(3, 4))
        assertTrue(set.contains(3))
        assertTrue(set.contains(4))
        subRange.remove(Range.closed(3, 6))
        assertFalse(set.contains(3))
        assertFalse(set.contains(6))
        assertTrue(set.contains(1))
        assertTrue(set.contains(7))
        assertFailsWith<IllegalArgumentException> { subRange.add(Range.singleton(7)) }

        subRange.clear()
        assertTrue(set.contains(1))
        assertTrue(set.contains(7))
        assertFalse(set.contains(2))
        assertFalse(set.contains(6))
    }

    @Test
    fun treeRangeSet_asRangesIsCachedLiveAndRemovalCapable() {
        val set = TreeRangeSet.create<Int>().apply { add(Range.closedOpen(1, 3)) }
        val ascending = set.asRanges()
        val descending = set.asDescendingSetOfRanges()

        set.add(Range.closed(5, 7))
        assertEquals(listOf(Range.closedOpen(1, 3), Range.closed(5, 7)), ascending.toList())
        assertEquals(listOf(Range.closed(5, 7), Range.closedOpen(1, 3)), descending.toList())
        assertTrue(ascending === set.asRanges())
        assertTrue(descending === set.asDescendingSetOfRanges())

        assertTrue(ascending.remove(Range.closedOpen(1, 3)))
        assertFalse(set.contains(1))
        descending.iterator().also { iterator ->
            assertEquals(Range.closed(5, 7), iterator.next())
            iterator.remove()
        }
        assertTrue(set.isEmpty())
        assertFailsWith<UnsupportedOperationException> { ascending.add(Range.singleton(9)) }

        set.add(Range.closed(10, 12))
        ascending.clear()
        assertTrue(set.isEmpty())
    }

    @Test
    fun treeRangeMap_putCoalescing_mergesEqual() {
        val map = TreeRangeMap.create<Int, String>()
        map.put(Range.closedOpen(1, 3), "a")
        map.putCoalescing(Range.closedOpen(3, 5), "a")
        val ranges = map.asMapOfRanges()
        assertEquals(1, ranges.size)
        assertEquals("a", map.get(2))
        assertEquals("a", map.get(4))
    }

    @Test
    fun treeRangeMap_putCoalescing_keepsUnequalSeparate() {
        val map = TreeRangeMap.create<Int, String>()
        map.put(Range.closedOpen(1, 3), "a")
        map.putCoalescing(Range.closedOpen(3, 5), "b")
        assertEquals("a", map.get(2))
        assertEquals("b", map.get(4))
        assertEquals(2, map.asMapOfRanges().size)
    }

    @Test
    fun treeRangeMap_putCoalescing_ordersClosedStartBeforeOpenStartAtSameEndpoint() {
        val map = TreeRangeMap.create<Int, String>()
        map.put(Range.closedOpen(-3, 2), "value")
        map.putCoalescing(Range.singleton(0), "value")
        assertEquals(mapOf(Range.closedOpen(-3, 2) to "value"), map.asMapOfRanges())
    }

    @Test
    fun treeRangeMap_subRangeMapAndRangeMapsStayLiveAndWriteThrough() {
        val map = TreeRangeMap.create<Int, String>().apply {
            put(Range.closed(1, 3), "a")
            put(Range.closed(5, 7), "b")
        }
        val subRangeMap = map.subRangeMap(Range.closed(2, 6))
        val subRanges = subRangeMap.asMapOfRanges()

        assertEquals(
            listOf(Range.closed(2, 3) to "a", Range.closed(5, 6) to "b"),
            subRanges.entries.map { it.key to it.value },
        )
        map.put(Range.closed(9, 10), "c")
        assertEquals(2, subRanges.size)

        subRangeMap.put(Range.closed(3, 4), "x")
        assertEquals("x", map[4])
        subRangeMap.remove(Range.closed(3, 6))
        assertEquals("a", map[2])
        assertEquals(null, map[3])
        assertEquals("b", map[7])
        assertFailsWith<IllegalArgumentException> { subRangeMap.put(Range.singleton(7), "outside") }

        val rootRanges = map.asMapOfRanges()
        assertTrue(rootRanges.remove(Range.closedOpen(1, 3)) == "a")
        assertEquals(null, map[2])
        rootRanges.entries.iterator().also { iterator ->
            assertEquals(Range.openClosed(6, 7), iterator.next().key)
            iterator.remove()
        }
        assertEquals(null, map[7])
        assertFailsWith<UnsupportedOperationException> { rootRanges[Range.singleton(11)] = "nope" }

        subRangeMap.clear()
        assertEquals(null, map[4])
    }

    @Test
    fun treeRangeMap_merge_remapsOverlap_only_putsValueOnGap() {
        val map = TreeRangeMap.create<Int, Int>()
        map.put(Range.closed(1, 5), 10)
        // Guava: keys in [3,5] → remapping(10, 1)=11; keys in (5,7] unmapped → 1; [1,3) stays 10
        map.merge(Range.closed(3, 7), 1) { a, b -> a + b }
        assertEquals(10, map.get(2))
        assertEquals(11, map.get(3))
        assertEquals(11, map.get(4))
        assertEquals(11, map.get(5))
        assertEquals(1, map.get(6))
        assertEquals(1, map.get(7))
    }
}
