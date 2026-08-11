package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SortedMultisetContractTest {
    @Test
    fun liveRangeViewsRespectOpenClosedBoundsAndWriteThrough() {
        val source = TreeMultiset.create(listOf(1, 2, 2, 3, 3, 3, 4))
        val range = source.subMultiset(2, BoundType.CLOSED, 4, BoundType.OPEN)
        assertEquals(listOf(2, 3), range.elementSet().toList())
        source.add(2, 1)
        assertEquals(3, range.count(2))
        assertEquals(3, range.remove(3, 2))
        assertEquals(1, source.count(3))
        assertEquals(3, range.add(2, 2))
        assertEquals(5, source.count(2))
        assertFailsWith<IllegalArgumentException> { range.add(4, 1) }
        assertEquals(0, range.add(4, 0))
        assertEquals(0, range.setCount(4, 0))
    }

    @Test
    fun descendingViewIsLiveReversibleAndMapsRanges() {
        val source = TreeMultiset.create(listOf(1, 2, 2, 3, 4))
        val descending = source.descendingMultiset()
        assertEquals(listOf(4, 3, 2, 1), descending.elementSet().toList())
        assertSame(source, descending.descendingMultiset())
        assertEquals(listOf(4, 3), descending.headMultiset(3, BoundType.CLOSED).elementSet().toList())
        assertEquals(listOf(2, 1), descending.tailMultiset(3, BoundType.OPEN).elementSet().toList())
        assertEquals(1, descending.remove(4, 1))
        assertEquals(0, source.count(4))
    }

    @Test
    fun pollingRemovesWholeBoundaryEntries() {
        val source = TreeMultiset.create(listOf(1, 1, 2, 3, 3, 3))
        assertEquals(Multisets.immutableEntry(1, 2), source.pollFirstEntry())
        assertEquals(Multisets.immutableEntry(3, 3), source.pollLastEntry())
        assertEquals(listOf(2), source.toList())
        assertEquals(null, TreeMultiset.create<Int>().pollFirstEntry())
    }

    @Test
    fun comparatorEquivalenceDefinesDistinctElements() {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val source = TreeMultiset.create(byLength)
        source.add("aa", 2)
        assertEquals(2, source.add("bb", 3))
        assertEquals(5, source.count("aa"))
        assertEquals(5, source.count("bb"))
        assertEquals(listOf("aa"), source.elementSet().toList())
        val range = source.headMultiset("xxx", BoundType.OPEN)
        assertEquals(5, range.count("zz"))
        assertEquals(5, range.remove("zz", 1))
        assertEquals(4, source.count("aa"))

        val immutable = ImmutableSortedMultiset.orderedBy(byLength)
            .addCopies("aa", 2)
            .addCopies("bb", 3)
            .build()
        assertEquals(5, immutable.count("zz"))
        assertEquals(listOf("aa"), immutable.elementSet().toList())
    }

    @Test
    fun immutableNaturalOrderingDefinesIdentityEvenWhenEqualsDiffers() {
        val first = ComparableToken(1, "first")
        val equivalent = ComparableToken(1, "equivalent")
        val later = ComparableToken(2, "later")
        val multiset = ImmutableSortedMultiset.copyOf(listOf(first, equivalent, later))

        assertEquals(2, multiset.count(equivalent))
        assertEquals(listOf(1, 2), multiset.elementSet().map { it.group })
        assertEquals(listOf(1, 1, 2), multiset.map { it.group })
    }

    @Test
    fun immutableAndUnmodifiableSortedViewsRejectEveryMutationRoute() {
        val immutable = ImmutableSortedMultiset.copyOf(listOf(3, 1, 2, 2))
        assertEquals(listOf(1, 2, 2, 3), immutable.toList())
        assertEquals(listOf(3, 2, 2, 1), immutable.descendingMultiset().toList())
        assertSame(immutable, immutable.descendingMultiset().descendingMultiset())
        assertFailsWith<UnsupportedOperationException> { immutable.pollFirstEntry() }
        assertFails {
            (immutable.entrySet() as MutableSet<Multiset.Entry<Int>>).clear()
        }

        val source = TreeMultiset.create(listOf(1, 2, 2))
        val view = Multisets.unmodifiableSortedMultiset(source)
        source.add(3, 2)
        assertEquals(2, view.count(3))
        assertTrue(view.comparator().compare(1, 2) < 0)
        assertSame(view, view.descendingMultiset().descendingMultiset())
        assertFailsWith<UnsupportedOperationException> { view.pollLastEntry() }
        assertFailsWith<UnsupportedOperationException> {
            (view.elementSet() as MutableSet<Int>).remove(1)
        }
    }
}

private data class ComparableToken(val group: Int, val label: String) : Comparable<ComparableToken> {
    override fun compareTo(other: ComparableToken): Int = group.compareTo(other.group)
}
