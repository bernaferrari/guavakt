package com.bernaferrari.guavakt.parity

import com.google.common.collect.ImmutableRangeMap as GuavaImmutableRangeMap
import com.google.common.collect.ImmutableRangeSet as GuavaImmutableRangeSet
import com.google.common.collect.Range as GuavaRange
import com.bernaferrari.guavakt.collect.ImmutableRangeMap as GuavaKtImmutableRangeMap
import com.bernaferrari.guavakt.collect.ImmutableRangeSet as GuavaKtImmutableRangeSet
import com.bernaferrari.guavakt.collect.Range as GuavaKtRange
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableRangeCollectionsDifferentialTest {
    @Test
    fun rangeSetOrderingQueriesAlgebraAndIdentitiesMatchGuava() {
        val guava = GuavaImmutableRangeSet.builder<Int>()
            .add(GuavaRange.greaterThan(10)).add(GuavaRange.closedOpen(1, 3))
            .add(GuavaRange.lessThan(-5)).add(GuavaRange.closed(3, 5)).build()
        val guavaKt = GuavaKtImmutableRangeSet.builder<Int>()
            .add(GuavaKtRange.greaterThan(10)).add(GuavaKtRange.closedOpen(1, 3))
            .add(GuavaKtRange.lessThan(-5)).add(GuavaKtRange.closed(3, 5)).build()

        assertEquals(
            listOf(
                guava.asRanges().map(Any::toString), guava.asDescendingSetOfRanges().map(Any::toString),
                guava.span().toString(), guava.rangeContaining(4)?.toString(),
                guava.intersects(GuavaRange.closed(4, 7)), guava.intersects(GuavaRange.closed(6, 7)),
                guava.complement().asRanges().map(Any::toString),
                guava.intersection(GuavaImmutableRangeSet.of(GuavaRange.closed(4, 12))).asRanges().map(Any::toString),
                guava.difference(GuavaImmutableRangeSet.of(GuavaRange.openClosed(2, 5))).asRanges().map(Any::toString),
                guava.subRangeSet(GuavaRange.closed(2, 7)).asRanges().map(Any::toString),
                guava.complement() === guava.complement(), guava.complement().complement() === guava,
                GuavaImmutableRangeSet.copyOf(guava) === guava,
            ),
            listOf(
                guavaKt.asRanges().map(Any::toString), guavaKt.asDescendingSetOfRanges().map(Any::toString),
                guavaKt.span().toString(), guavaKt.rangeContaining(4)?.toString(),
                guavaKt.intersects(GuavaKtRange.closed(4, 7)), guavaKt.intersects(GuavaKtRange.closed(6, 7)),
                guavaKt.complement().asRanges().map(Any::toString),
                guavaKt.intersection(GuavaKtImmutableRangeSet.of(GuavaKtRange.closed(4, 12))).asRanges().map(Any::toString),
                guavaKt.difference(GuavaKtImmutableRangeSet.of(GuavaKtRange.openClosed(2, 5))).asRanges().map(Any::toString),
                guavaKt.subRangeSet(GuavaKtRange.closed(2, 7)).asRanges().map(Any::toString),
                guavaKt.complement() === guavaKt.complement(), guavaKt.complement().complement() === guavaKt,
                GuavaKtImmutableRangeSet.copyOf(guavaKt) === guavaKt,
            ),
        )
    }

    @Test
    fun rangeSetConstructionAndMutationFailuresMatchGuava() {
        val guava = GuavaImmutableRangeSet.of(GuavaRange.closed(1, 3))
        val guavaKt = GuavaKtImmutableRangeSet.of(GuavaKtRange.closed(1, 3))
        assertEquals(
            listOf(
                failureName { GuavaImmutableRangeSet.builder<Int>().add(GuavaRange.closed(1, 3)).add(GuavaRange.closed(3, 4)).build() },
                failureName { GuavaImmutableRangeSet.builder<Int>().add(GuavaRange.closedOpen(1, 1)) },
                failureName { guava.add(GuavaRange.closed(5, 6)) },
                failureName { guava.addAll(GuavaImmutableRangeSet.of()) },
                failureName { guava.asRanges().remove(GuavaRange.closed(1, 3)) },
                failureName { GuavaImmutableRangeSet.of<Int>().span() },
                GuavaImmutableRangeSet.unionOf(listOf(GuavaRange.closed(1, 3), GuavaRange.closed(3, 4))).asRanges().map(Any::toString),
            ),
            listOf(
                failureName { GuavaKtImmutableRangeSet.builder<Int>().add(GuavaKtRange.closed(1, 3)).add(GuavaKtRange.closed(3, 4)).build() },
                failureName { GuavaKtImmutableRangeSet.builder<Int>().add(GuavaKtRange.closedOpen(1, 1)) },
                failureName { guavaKt.add(GuavaKtRange.closed(5, 6)) },
                failureName { guavaKt.addAll(GuavaKtImmutableRangeSet.of()) },
                failureName { guavaKt.asRanges().remove(GuavaKtRange.closed(1, 3)) },
                failureName { GuavaKtImmutableRangeSet.of<Int>().span() },
                GuavaKtImmutableRangeSet.unionOf(listOf(GuavaKtRange.closed(1, 3), GuavaKtRange.closed(3, 4))).asRanges().map(Any::toString),
            ),
        )
    }

    @Test
    fun rangeMapOrderingClippingIdentityAndEmptyFactoryMatchGuava() {
        val guavaBuilder = GuavaImmutableRangeMap.builder<Int, String>()
            .put(GuavaRange.closedOpen(5, 7), "c").put(GuavaRange.closedOpen(1, 3), "a")
            .put(GuavaRange.closedOpen(3, 5), "b")
        val guavaKtBuilder = GuavaKtImmutableRangeMap.builder<Int, String>()
            .put(GuavaKtRange.closedOpen(5, 7), "c").put(GuavaKtRange.closedOpen(1, 3), "a")
            .put(GuavaKtRange.closedOpen(3, 5), "b")
        val guava = guavaBuilder.build()
        val guavaKt = guavaKtBuilder.build()
        guavaBuilder.put(GuavaRange.closed(8, 9), "d")
        guavaKtBuilder.put(GuavaKtRange.closed(8, 9), "d")
        val guavaEmptyRange = GuavaImmutableRangeMap.of(GuavaRange.closedOpen(1, 1), "empty")
        val guavaKtEmptyRange = GuavaKtImmutableRangeMap.of(GuavaKtRange.closedOpen(1, 1), "empty")

        assertEquals(
            listOf(
                entries(guava.asMapOfRanges()), entries(guava.asDescendingMapOfRanges()), guava.span().toString(),
                guava[4], guava.getEntry(4)?.let { it.key.toString() to it.value },
                entries(guava.subRangeMap(GuavaRange.closed(2, 6)).asMapOfRanges()),
                guava.subRangeMap(GuavaRange.all()) === guava, GuavaImmutableRangeMap.copyOf(guava) === guava,
                guavaBuilder.build().asMapOfRanges().size,
                entries(guavaEmptyRange.asMapOfRanges()), guavaEmptyRange[1],
            ),
            listOf(
                entries(guavaKt.asMapOfRanges()), entries(guavaKt.asDescendingMapOfRanges()), guavaKt.span().toString(),
                guavaKt.get(4), guavaKt.getEntry(4)?.let { it.key.toString() to it.value },
                entries(guavaKt.subRangeMap(GuavaKtRange.closed(2, 6)).asMapOfRanges()),
                guavaKt.subRangeMap(GuavaKtRange.all()) === guavaKt, GuavaKtImmutableRangeMap.copyOf(guavaKt) === guavaKt,
                guavaKtBuilder.build().asMapOfRanges().size,
                entries(guavaKtEmptyRange.asMapOfRanges()), guavaKtEmptyRange.get(1),
            ),
        )
    }

    @Test
    fun rangeMapConstructionAndDeepMutationFailuresMatchGuava() {
        val guava = GuavaImmutableRangeMap.of(GuavaRange.closed(1, 3), "a")
        val guavaKt = GuavaKtImmutableRangeMap.of(GuavaKtRange.closed(1, 3), "a")
        assertEquals(
            listOf(
                failureName { GuavaImmutableRangeMap.builder<Int, String>().put(GuavaRange.closed(1, 3), "a").put(GuavaRange.closed(3, 4), "b").build() },
                failureName { GuavaImmutableRangeMap.builder<Int, String>().put(GuavaRange.closedOpen(1, 1), "empty") },
                GuavaImmutableNullHarness.rangeMapNullValueFailure(),
                failureName { guava.put(GuavaRange.closed(4, 5), "b") }, failureName { guava.clear() },
                failureName { (guava.asMapOfRanges() as MutableMap<GuavaRange<Int>, String>).remove(GuavaRange.closed(1, 3)) },
                failureName { (guava.asMapOfRanges().entries.first() as MutableMap.MutableEntry<GuavaRange<Int>, String>).setValue("b") },
                failureName { GuavaImmutableRangeMap.of<Int, String>().span() },
            ),
            listOf(
                failureName { GuavaKtImmutableRangeMap.builder<Int, String>().put(GuavaKtRange.closed(1, 3), "a").put(GuavaKtRange.closed(3, 4), "b").build() },
                failureName { GuavaKtImmutableRangeMap.builder<Int, String>().put(GuavaKtRange.closedOpen(1, 1), "empty") },
                failureName { GuavaKtImmutableRangeMap.builder<Int, String?>().put(GuavaKtRange.closed(1, 2), null) },
                failureName { guavaKt.put(GuavaKtRange.closed(4, 5), "b") }, failureName { guavaKt.clear() },
                failureName { (guavaKt.asMapOfRanges() as MutableMap<GuavaKtRange<Int>, String>).remove(GuavaKtRange.closed(1, 3)) },
                failureName { (guavaKt.asMapOfRanges().entries.first() as MutableMap.MutableEntry<GuavaKtRange<Int>, String>).setValue("b") },
                failureName { GuavaKtImmutableRangeMap.of<Int, String>().span() },
            ),
        )
    }

    private fun entries(map: Map<*, *>): List<Pair<String, Any?>> =
        map.entries.map { it.key.toString() to it.value }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
