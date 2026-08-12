package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoreAlgorithmsTest {
    @Test
    fun treeRangeMap_putGet() {
        val map = TreeRangeMap.create<Int, String>()
        map.put(Range.closed(1, 3), "a")
        map.put(Range.closed(5, 7), "b")
        assertEquals("a", map.get(2))
        assertEquals("b", map.get(6))
        assertNull(map.get(4))
    }

    @Test
    fun immutableRangeSet_copyOf() {
        val set = ImmutableRangeSet.unionOf(listOf(Range.closed(1, 2), Range.closed(2, 4)))
        assertTrue(set.contains(3))
        assertFalse(set.contains(10))
    }

    @Test
    fun immutableRangeMap_builder() {
        val map = ImmutableRangeMap.builder<Int, String>()
            .put(Range.closed(0, 5), "low")
            .build()
        assertEquals("low", map.get(3))
    }

    @Test
    fun mapMaker_computingMap() {
        var calls = 0
        val map = MapMaker().makeComputingMap<String, Int> { calls++; it.length }
        assertEquals(5, map["hello"])
        assertEquals(5, map["hello"])
        assertEquals(1, calls)
    }

    @Test
    fun minMaxPriorityQueue_minmax() {
        val q = MinMaxPriorityQueue.create(listOf(3, 1, 4, 1, 5))
        assertEquals(1, q.peekFirst())
        assertEquals(5, q.peekLast())
        assertEquals(1, q.pollFirst())
        assertEquals(5, q.pollLast())
    }

    @Test
    fun moreCollectors_onlyElement() {
        assertEquals(7, MoreCollectors.onlyElement(listOf(7)))
        assertEquals(null, MoreCollectors.toOptional(emptyList<String>()))
        assertEquals("x", MoreCollectors.toOptional(listOf("x")))
    }

    @Test
    fun streams_zip_concat() {
        val zipped = Streams.zip(listOf(1, 2), listOf("a", "b")) { i, s -> "$i$s" }.toList()
        assertEquals(listOf("1a", "2b"), zipped)
        assertEquals(listOf(1, 2, 3), Streams.concat(listOf(1), listOf(2, 3)).toList())
        assertEquals(3, Streams.findLast(listOf(1, 2, 3)))
    }
}
