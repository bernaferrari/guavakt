package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListsMapsSetsTest {
    @Test
    fun listsPartitionAndReverse() {
        val list = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), Lists.partition(list, 2))
        assertEquals(listOf(5, 4, 3, 2, 1), Lists.reverse(list).toList())
        assertEquals(listOf('a', 'b'), Lists.charactersOf("ab").toList())
    }

    @Test
    fun listsTransformIsLive() {
        val src = mutableListOf(1, 2)
        val view = Lists.transform(src) { it * 10 }
        assertEquals(10, view[0])
        src[0] = 3
        assertEquals(30, view[0])
    }

    @Test
    fun setsAlgebra() {
        val a = setOf(1, 2, 3)
        val b = setOf(2, 3, 4)
        assertEquals(setOf(2, 3), Sets.intersection(a, b).toSet())
        assertEquals(setOf(1, 2, 3, 4), Sets.union(a, b).toSet())
        assertEquals(setOf(1), Sets.difference(a, b).toSet())
        assertEquals(setOf(1, 4), Sets.symmetricDifference(a, b).toSet())
        assertEquals(8, Sets.powerSet(setOf(1, 2, 3)).size)
    }

    @Test
    fun mapsDifferenceAndUniqueIndex() {
        val left = mapOf("a" to 1, "b" to 2)
        val right = mapOf("b" to 3, "c" to 4)
        val d = Maps.difference(left, right)
        assertFalse(d.areEqual())
        assertEquals(mapOf("a" to 1), d.entriesOnlyOnLeft())
        assertEquals(mapOf("c" to 4), d.entriesOnlyOnRight())
        assertTrue(d.entriesDiffering().containsKey("b"))
        val idx = Maps.uniqueIndex(listOf("x", "yy")) { it.length }
        assertEquals("x", idx[1])
        assertEquals("yy", idx[2])
    }

    @Test
    fun iteratorsPeekAndPartition() {
        val it = Iterators.peekingIterator(listOf(1, 2, 3).iterator())
        assertEquals(1, it.peek())
        assertEquals(1, it.next())
        assertEquals(2, it.peek())
        val parts = Iterators.partition(listOf(1, 2, 3, 4, 5).iterator(), 2).asSequence().toList()
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), parts)
    }

    @Test
    fun rangeSpanGap() {
        val a = Range.closed(1, 3)
        val b = Range.closed(5, 7)
        assertEquals(Range.closed(1, 7), a.span(b))
        assertTrue(a.gap(b).contains(4))
        assertFalse(a.isConnected(b))
        assertTrue(Range.closed(1, 3).isConnected(Range.closed(3, 5)))
    }

    @Test
    fun immutableSetMultimap() {
        val m = ImmutableSetMultimap.builder<String, Int>().put("a", 1).put("a", 1).put("a", 2).build()
        assertEquals(2, m.size())
        assertEquals(setOf(1, 2), m.get("a"))
    }
}
