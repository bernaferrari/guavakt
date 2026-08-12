package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImmutableRangeCollectionsContractTest {
    @Test
    fun rangeSetOrdersCoalescesAndProvidesImmutableAlgebra() {
        val set = ImmutableRangeSet.builder<Int>()
            .add(Range.greaterThan(10))
            .add(Range.closedOpen(1, 3))
            .add(Range.lessThan(-5))
            .add(Range.closed(3, 5))
            .build()

        assertEquals(
            listOf(Range.lessThan(-5), Range.closed(1, 5), Range.greaterThan(10)),
            set.asRanges().toList(),
        )
        assertEquals(set.asRanges().toList().asReversed(), set.asDescendingSetOfRanges().toList())
        assertEquals(Range.all(), set.span())
        assertEquals(Range.closed(1, 5), set.rangeContaining(4))
        assertTrue(set.intersects(Range.closed(4, 7)))
        assertFalse(set.intersects(Range.closed(6, 7)))

        val complement = set.complement()
        assertSame(complement, set.complement())
        assertSame(set, complement.complement())
        assertEquals(listOf(Range.closedOpen(-5, 1), Range.openClosed(5, 10)), complement.asRanges().toList())
        assertEquals(
            listOf(Range.closed(4, 5), Range.openClosed(10, 12)),
            set.intersection(ImmutableRangeSet.of(Range.closed(4, 12))).asRanges().toList(),
        )
        assertEquals(
            listOf(Range.lessThan(-5), Range.closed(1, 2), Range.greaterThan(10)),
            set.difference(ImmutableRangeSet.of(Range.openClosed(2, 5))).asRanges().toList(),
        )
        assertEquals(listOf(Range.closed(2, 5)), set.subRangeSet(Range.closed(2, 7)).asRanges().toList())
        assertSame(set, set.subRangeSet(Range.all()))
        assertSame(set, ImmutableRangeSet.copyOf(set))
    }

    @Test
    fun rangeSetConstructionAndMutationFailuresAreStrict() {
        assertFailsWith<IllegalArgumentException> {
            ImmutableRangeSet.builder<Int>().add(Range.closed(1, 3)).add(Range.closed(3, 4)).build()
        }
        assertFailsWith<IllegalArgumentException> {
            ImmutableRangeSet.builder<Int>().add(Range.closedOpen(1, 1))
        }
        assertEquals(
            listOf(Range.closed(1, 4)),
            ImmutableRangeSet.unionOf(listOf(Range.closed(1, 3), Range.closed(3, 4))).asRanges().toList(),
        )

        val set = ImmutableRangeSet.of(Range.closed(1, 3))
        assertFailsWith<UnsupportedOperationException> { set.add(Range.closed(5, 6)) }
        assertFailsWith<UnsupportedOperationException> { set.addAll(ImmutableRangeSet.of<Int>()) }
        assertFailsWith<UnsupportedOperationException> { set.remove(Range.closed(1, 2)) }
        assertFailsWith<UnsupportedOperationException> { set.clear() }
        assertFailsWith<UnsupportedOperationException> { set.asRanges().remove(Range.closed(1, 3)) }
        assertFailsWith<NoSuchElementException> { ImmutableRangeSet.of<Int>().span() }
    }

    @Test
    fun rangeMapOrdersClipsSnapshotsAndExposesImmutableMaps() {
        val builder = ImmutableRangeMap.builder<Int, String>()
            .put(Range.closedOpen(5, 7), "c")
            .put(Range.closedOpen(1, 3), "a")
            .put(Range.closedOpen(3, 5), "b")
        val first = builder.build()
        builder.put(Range.closed(8, 9), "d")

        assertEquals(listOf("a", "b", "c"), first.asMapOfRanges().values.toList())
        assertEquals(listOf("c", "b", "a"), first.asDescendingMapOfRanges().values.toList())
        assertEquals(Range.closedOpen(1, 7), first.span())
        assertEquals("b", first.get(4))
        assertEquals(Maps.immutableEntry(Range.closedOpen(3, 5), "b"), first.getEntry(4))
        assertEquals(
            listOf(
                Range.closedOpen(2, 3) to "a",
                Range.closedOpen(3, 5) to "b",
                Range.closed(5, 6) to "c",
            ),
            first.subRangeMap(Range.closed(2, 6)).asMapOfRanges().map { it.key to it.value },
        )
        assertSame(first, first.subRangeMap(Range.all()))
        assertSame(first, ImmutableRangeMap.copyOf(first))
        assertEquals(4, builder.build().asMapOfRanges().size)
    }

    @Test
    fun rangeMapRejectsInvalidConstructionAndEveryMutationRoute() {
        assertFailsWith<IllegalArgumentException> {
            ImmutableRangeMap.builder<Int, String>()
                .put(Range.closed(1, 3), "a").put(Range.closed(3, 4), "b").build()
        }
        assertFailsWith<IllegalArgumentException> {
            ImmutableRangeMap.builder<Int, String>().put(Range.closedOpen(1, 1), "empty")
        }
        assertFailsWith<NullPointerException> {
            ImmutableRangeMap.builder<Int, String?>().put(Range.closed(1, 2), null)
        }

        val map = ImmutableRangeMap.of(Range.closed(1, 3), "a")
        assertFailsWith<UnsupportedOperationException> { map.put(Range.closed(4, 5), "b") }
        assertFailsWith<UnsupportedOperationException> { map.remove(Range.closed(1, 2)) }
        assertFailsWith<UnsupportedOperationException> { map.clear() }
        assertFailsWith<UnsupportedOperationException> {
            (map.asMapOfRanges() as MutableMap<Range<Int>, String>).remove(Range.closed(1, 3))
        }
        assertFailsWith<UnsupportedOperationException> {
            (map.asMapOfRanges().entries.first() as MutableMap.MutableEntry<Range<Int>, String>).setValue("b")
        }
        assertFailsWith<NoSuchElementException> { ImmutableRangeMap.of<Int, String>().span() }

        val emptyRangeMap = ImmutableRangeMap.of(Range.closedOpen(1, 1), "accepted by Guava factory")
        assertEquals(1, emptyRangeMap.asMapOfRanges().size)
        assertEquals(null, emptyRangeMap.get(1))
    }
}
