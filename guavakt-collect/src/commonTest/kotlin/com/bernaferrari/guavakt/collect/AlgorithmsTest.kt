package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlgorithmsTest {
    @Test
    fun comparators_lexicographical_and_order() {
        val cmp = Comparators.lexicographical(naturalOrder<Int>())
        assertTrue(cmp.compare(listOf(1), listOf(1, 1)) < 0)
        assertTrue(Comparators.isInOrder(listOf(1, 2, 2, 3), naturalOrder()))
        assertFalse(Comparators.isInStrictOrder(listOf(1, 2, 2, 3), naturalOrder()))
    }

    @Test
    fun interner_returns_same_instance() {
        val interner = Interners.newStrongInterner<String>()
        val a = interner.intern("hello")
        val b = interner.intern("hello")
        assertTrue(a === b)
    }

    @Test
    fun topK_least() {
        val sel = TopKSelector.least<Int>(3)
        sel.offerAll(listOf(5, 1, 4, 2, 3))
        assertEquals(listOf(1, 2, 3), sel.topK())
    }

    @Test
    fun treeMultiset_sorted_elements() {
        val tm = TreeMultiset.create(listOf("b", "a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), tm.elementSet().toList())
        assertEquals(2, tm.count("b"))
    }

    @Test
    fun treeRangeSet_contains() {
        val rs = TreeRangeSet.create<Int>()
        rs.add(Range.closed(1, 3))
        rs.add(Range.closed(5, 7))
        assertTrue(rs.contains(2))
        assertFalse(rs.contains(4))
        assertTrue(rs.contains(6))
    }

    @Test
    fun contiguousSet_closed_range() {
        val set = ContiguousSet.create(Range.closed(1, 5))
        assertEquals(5, set.size)
        assertTrue(3 in set)
        assertEquals(listOf(1, 2, 3), set.headSet(3, true).toList())
    }

    @Test
    fun contiguousSet_large_ranges_are_lazy_and_sizes_saturate() {
        val set = ContiguousSet.create(Range.closed(Int.MIN_VALUE, Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE, set.size)
        val iterator = set.iterator()
        assertEquals(Int.MIN_VALUE, iterator.next())
        assertEquals(Int.MIN_VALUE + 1, iterator.next())
        assertTrue(iterator.hasNext())
    }

    @Test
    fun contiguousSet_unbounded_ranges_use_discrete_domain_extremes() {
        val atMost = ContiguousSet.create(Range.atMost(2))
        assertEquals(Int.MIN_VALUE, atMost.first())
        assertEquals(2, atMost.last())
        val atLeast = ContiguousSet.create(Range.greaterThan(Int.MAX_VALUE - 1))
        assertEquals(Int.MAX_VALUE, atLeast.first())
        assertEquals(Int.MAX_VALUE, atLeast.last())
    }

    @Test
    fun contiguousSet_supports_long_and_custom_discrete_domains() {
        val longs = ContiguousSet.create(Range.openClosed(10L, 13L), DiscreteDomain.longs())
        assertEquals(listOf(11L, 12L, 13L), longs.toList())
        val evens = object : DiscreteDomain<Int>() {
            override fun next(value: Int): Int? = if (value >= 8) null else value + 2
            override fun previous(value: Int): Int? = if (value <= 0) null else value - 2
            override fun distance(start: Int, end: Int): Long = (end - start).toLong() / 2
            override fun minValue(): Int = 0
            override fun maxValue(): Int = 8
        }
        assertEquals(listOf(2, 4, 6), ContiguousSet.create(Range.closed(2, 6), evens).toList())
    }

    @Test
    fun contiguousSet_supports_unbounded_arbitrary_precision_integer_domain() {
        val start = BigInteger.parse("999999999999999999999999999999")
        val end = start + BigInteger.of(2)
        val set = ContiguousSet.create(Range.closed(start, end), DiscreteDomain.bigIntegers())

        assertEquals(listOf(start, start + BigInteger.ONE, end), set.toList())
        assertEquals(3, set.size)
        assertEquals(start, set.first())
        assertEquals(end, set.last())
        assertTrue(DiscreteDomain.bigIntegers() === DiscreteDomain.bigIntegers())

        val huge = BigInteger.TEN.pow(100)
        val saturated = ContiguousSet.create(Range.closed(-huge, huge), DiscreteDomain.bigIntegers())
        assertEquals(Int.MAX_VALUE, saturated.size)
        assertEquals(-huge, saturated.first())
        assertEquals(huge, saturated.last())
        assertEquals(Long.MAX_VALUE, DiscreteDomain.bigIntegers().distance(-huge, huge))
    }

    @Test
    fun contiguousSet_factories_views_and_domain_identity_match_the_discrete_contract() {
        val set = ContiguousSet.closed(3, 7)
        assertEquals(3, set.first())
        assertEquals(7, set.last())
        assertEquals("[3..7]", set.toString())
        assertEquals(Range.open(2, 8), set.range(BoundType.OPEN, BoundType.OPEN))
        assertEquals(Range.all(), ContiguousSet.closed(Int.MIN_VALUE, Int.MAX_VALUE).range(BoundType.OPEN, BoundType.OPEN))
        assertEquals(listOf(3, 4), set.headSet(5).toList())
        assertEquals(listOf(3, 4, 5), set.headSet(5, true).toList())
        assertEquals(listOf(5, 6, 7), set.tailSet(5).toList())
        assertEquals(listOf(4, 5, 6), set.subSet(4, 7).toList())
        assertEquals(listOf(4, 5, 6, 7), set.subSet(4, true, 7, true).toList())
        assertEquals(listOf(3L, 4L), ContiguousSet.closedOpen(3L, 5L).toList())
        assertTrue(DiscreteDomain.integers() === DiscreteDomain.integers())
        assertTrue(DiscreteDomain.longs() === DiscreteDomain.longs())

        val empty = ContiguousSet.closedOpen(4, 4)
        assertTrue(empty.isEmpty())
        assertEquals("[]", empty.toString())
        assertFailsWith<NoSuchElementException> { empty.first() }
        assertFailsWith<NoSuchElementException> { empty.range(BoundType.CLOSED, BoundType.OPEN) }

        val unbounded = object : DiscreteDomain<Int>() {
            override fun next(value: Int): Int? = value + 1
            override fun previous(value: Int): Int? = value - 1
            override fun distance(start: Int, end: Int): Long = end.toLong() - start.toLong()
        }
        assertFailsWith<IllegalArgumentException> { ContiguousSet.create(Range.all(), unbounded) }
    }

    @Test
    fun range_canonical_normalizes_discrete_integer_cuts() {
        val integers = DiscreteDomain.integers()
        assertEquals(Range.closedOpen(4, 5), Range.openClosed(3, 4).canonical(integers))
        assertEquals(Range.closedOpen(4, 4), Range.open(3, 4).canonical(integers))
        assertEquals(
            Range.closedOpen(Int.MIN_VALUE, Int.MIN_VALUE + 1),
            Range.atMost(Int.MIN_VALUE).canonical(integers),
        )
        assertEquals(
            Range.downTo(Int.MIN_VALUE, BoundType.CLOSED),
            Range.all<Int>().canonical(integers),
        )
    }

    @Test
    fun immutableRangeSet_asSet_is_lazy_immutable_and_domain_bounded() {
        val rangeSet = ImmutableRangeSet.copyOf(listOf(Range.closed(1, 3), Range.closed(5, 6)))
        val values = rangeSet.asSet(DiscreteDomain.integers())
        assertEquals(listOf(1, 2, 3, 5, 6), values.toList())
        assertEquals(5, values.size)
        assertTrue(5 in values)
        assertFalse(4 in values)
        assertEquals("[[1..3], [5..6]]", values.toString())
        assertFailsWith<UnsupportedOperationException> { (values as MutableSet<Int>).add(7) }

        val large = ImmutableRangeSet.of(Range.greaterThan(0)).asSet(DiscreteDomain.integers())
        assertEquals(Int.MAX_VALUE, large.size)
        val iterator = large.iterator()
        assertEquals(1, iterator.next())
        assertEquals(2, iterator.next())

        val unbounded = object : DiscreteDomain<Int>() {
            override fun next(value: Int): Int? = value + 1
            override fun previous(value: Int): Int? = value - 1
            override fun distance(start: Int, end: Int): Long = end.toLong() - start.toLong()
        }
        assertFailsWith<IllegalArgumentException> { ImmutableRangeSet.of(Range.all<Int>()).asSet(unbounded) }
    }

    @Test
    fun priorityQueue_orders() {
        val q = Queues.newPriorityQueue<Int>()
        q.offer(3); q.offer(1); q.offer(2)
        assertEquals(1, q.poll())
        assertEquals(2, q.poll())
        assertEquals(3, q.poll())
    }
}
