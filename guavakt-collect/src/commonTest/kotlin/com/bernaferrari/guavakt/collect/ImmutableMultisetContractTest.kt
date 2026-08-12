package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ImmutableMultisetContractTest {
    @Test
    fun factoriesPreserveFirstOccurrenceOrderCountsAndIdentity() {
        val multiset = ImmutableMultiset.of("b", "a", "b", "c", "a", "b")
        assertEquals(listOf("b", "a", "c"), multiset.elementSet().toList())
        assertEquals(listOf("b", "b", "b", "a", "a", "c"), multiset.asList())
        assertEquals(listOf("b" to 3, "a" to 2, "c" to 1), entries(multiset))
        assertEquals("[b x 3, a x 2, c]", multiset.toString())
        assertSame(multiset, ImmutableMultiset.copyOf(multiset))
        assertSame(multiset.asList(), multiset.asList())
    }

    @Test
    fun copyOfMultisetPreservesCountsWithoutDependingOnExpandedIteration() {
        val source = LinkedHashMultiset.create<String>().apply {
            add("first", 4)
            add("second", 2)
        }
        val snapshot = ImmutableMultiset.copyOf(source)
        source.setCount("first", 1)
        assertEquals(listOf("first" to 4, "second" to 2), entries(snapshot))
        assertEquals(snapshot, ImmutableMultiset.copyOf(snapshot.iterator()))
        assertEquals(snapshot, ImmutableMultiset.copyOf(snapshot.toList().toTypedArray()))
    }

    @Test
    fun builderSupportsCountsIteratorsAndReusableSnapshots() {
        val builder = ImmutableMultiset.builder<String>()
            .add("a", "b", "a")
            .addCopies("b", 2)
            .setCount("a", 4)
            .addAll(listOf("c", "c").iterator())
        val first = builder.build()
        builder.setCount("a", 0).add("d")
        assertEquals(listOf("a" to 4, "b" to 3, "c" to 2), entries(first))
        assertEquals(listOf("b" to 3, "c" to 2, "d" to 1), entries(builder.build()))
        assertFailsWith<IllegalArgumentException> { builder.addCopies("x", -1) }
        assertFailsWith<IllegalArgumentException> { builder.setCount("x", -1) }
    }

    @Test
    fun nullsAndEveryMutationRouteAreRejected() {
        assertFailsWith<NullPointerException> { ImmutableMultiset.of<String?>(null) }
        assertFailsWith<NullPointerException> { ImmutableMultiset.builder<String?>().addCopies(null, 0) }
        assertFailsWith<NullPointerException> { ImmutableMultiset.copyOf(listOf("a", null)) }

        val multiset = ImmutableMultiset.of("a", "a", "b")
        assertFailsWith<UnsupportedOperationException> { multiset.add("x", 0) }
        assertFailsWith<UnsupportedOperationException> { multiset.remove("missing", 0) }
        assertFailsWith<UnsupportedOperationException> { multiset.setCount("missing", 7, 0) }
        assertFailsWith<UnsupportedOperationException> { multiset.addAll(emptyList()) }
        assertFailsWith<UnsupportedOperationException> { multiset.removeAll(emptyList()) }
        assertFailsWith<UnsupportedOperationException> { multiset.retainAll(multiset) }
        assertFailsWith<UnsupportedOperationException> { multiset.iterator().remove() }
        assertFailsWith<UnsupportedOperationException> { (multiset.elementSet() as MutableSet<String>).remove("missing") }
        assertFailsWith<UnsupportedOperationException> { (multiset.entrySet() as MutableSet<Multiset.Entry<String>>).clear() }
        assertFailsWith<UnsupportedOperationException> { (multiset.asList() as MutableList<String>).remove("missing") }
    }

    private fun <E> entries(multiset: ImmutableMultiset<E>): List<Pair<E, Int>> =
        multiset.entrySet().map { it.getElement() to it.getCount() }
}
