package dev.guavakt.parity

import com.google.common.collect.LinkedHashMultiset as GuavaLinkedHashMultiset
import com.google.common.collect.Multisets as GuavaMultisets
import dev.guavakt.collect.HashMultiset as GuavaKtHashMultiset
import dev.guavakt.collect.LinkedHashMultiset as GuavaKtLinkedHashMultiset
import dev.guavakt.collect.Multiset as GuavaKtMultiset
import dev.guavakt.collect.Multisets as GuavaKtMultisets
import kotlin.test.Test
import kotlin.test.assertEquals

class MultisetCoreDifferentialTest {
    @Test
    fun valueSemanticsAndFormattingMatchGuava() {
        val guavaFirst = GuavaLinkedHashMultiset.create(listOf("a", "b", "a"))
        val guavaSecond = GuavaLinkedHashMultiset.create(listOf("b", "a", "a"))
        val guavaKtFirst = GuavaKtLinkedHashMultiset.create(listOf("a", "b", "a"))
        val guavaKtSecond = GuavaKtHashMultiset.create(listOf("b", "a", "a"))

        assertEquals(
            listOf(
                guavaFirst == guavaSecond,
                guavaFirst.hashCode() == guavaSecond.hashCode(),
                guavaFirst.toString(),
            ),
            listOf(
                guavaKtFirst == guavaKtSecond,
                guavaKtFirst.hashCode() == guavaKtSecond.hashCode(),
                guavaKtFirst.toString(),
            ),
        )
    }

    @Test
    fun elementAndEntryViewsMatchGuava() {
        assertEquals(guavaViewTrace(), guavaKtViewTrace())
    }

    @Test
    fun occurrenceValidationSaturationAndIteratorRemovalMatchGuava() {
        assertEquals(guavaOccurrenceTrace(), guavaKtOccurrenceTrace())
    }

    @Test
    fun unmodifiableMultisetIsALiveReadOnlyViewLikeGuava() {
        val guavaSource = GuavaLinkedHashMultiset.create(listOf("a", "a"))
        val guava = GuavaMultisets.unmodifiableMultiset(guavaSource)
        val guavaKtSource = GuavaKtLinkedHashMultiset.create(listOf("a", "a"))
        val guavaKt = GuavaKtMultisets.unmodifiableMultiset(guavaKtSource)

        guavaSource.add("b", 3)
        guavaKtSource.add("b", 3)
        assertEquals(
            listOf(
                guava.count("b"),
                failureName { guava.add("c", 1) },
                failureName { guava.elementSet().remove("a") },
                failureName { guava.entrySet().remove(GuavaMultisets.immutableEntry("a", 2)) },
            ),
            listOf(
                guavaKt.count("b"),
                failureName { guavaKt.add("c", 1) },
                failureName { (guavaKt.elementSet() as MutableSet).remove("a") },
                failureName {
                    (guavaKt.entrySet() as MutableSet).remove(GuavaKtMultisets.immutableEntry("a", 2))
                },
            ),
        )
    }

    @Test
    fun nullableElementBehaviorMatchesGuavaRuntime() {
        val multiset = GuavaKtLinkedHashMultiset.create<String?>()
        val trace = mutableListOf<Any?>(
            multiset.add(null, 3),
            multiset.count(null),
            multiset.remove(null, 1),
            multiset.count(null),
            (multiset.elementSet() as MutableSet<String?>).remove(null),
            multiset.isEmpty(),
        )
        assertEquals(GuavaMultisetNullHarness.trace(), trace)
    }

    @Test
    fun computedViewsStayLiveAndReadOnlyLikeGuava() {
        assertEquals(guavaComputedTrace(), guavaKtComputedTrace())
    }

    @Test
    fun filteredViewIsLiveAndWritesThroughLikeGuava() {
        assertEquals(guavaFilteredTrace(), guavaKtFilteredTrace())
    }

    private fun guavaViewTrace(): List<Any?> {
        val multiset = GuavaLinkedHashMultiset.create<String>()
        multiset.add("a", 2)
        multiset.add("b", 1)
        val elements = multiset.elementSet()
        val entries = multiset.entrySet()
        multiset.add("c", 3)
        val trace = mutableListOf<Any?>(
            elements.toList(),
            entryPairs(entries),
            entries.contains(GuavaMultisets.immutableEntry("a", 2)),
            entries.remove(GuavaMultisets.immutableEntry("a", 1)),
            entries.remove(GuavaMultisets.immutableEntry("a", 2)),
            multiset.size,
            elements.remove("b"),
            multiset.size,
        )
        val iterator = entries.iterator()
        trace.add(iterator.next().let { it.element to it.count })
        iterator.remove()
        trace.addAll(listOf(multiset.isEmpty(), entries.isEmpty(), elements.isEmpty()))
        multiset.add("d", 4)
        trace.addAll(listOf(elements.toList(), entryPairs(entries)))
        return trace
    }

    private fun guavaKtViewTrace(): List<Any?> {
        val multiset = GuavaKtLinkedHashMultiset.create<String>()
        multiset.add("a", 2)
        multiset.add("b", 1)
        val elements = multiset.elementSet() as MutableSet<String>
        val entries = multiset.entrySet() as MutableSet<GuavaKtMultiset.Entry<String>>
        multiset.add("c", 3)
        val trace = mutableListOf<Any?>(
            elements.toList(),
            entryPairsKt(entries),
            entries.contains(GuavaKtMultisets.immutableEntry("a", 2)),
            entries.remove(GuavaKtMultisets.immutableEntry("a", 1)),
            entries.remove(GuavaKtMultisets.immutableEntry("a", 2)),
            multiset.size,
            elements.remove("b"),
            multiset.size,
        )
        val iterator = entries.iterator()
        trace.add(iterator.next().let { it.getElement() to it.getCount() })
        iterator.remove()
        trace.addAll(listOf(multiset.isEmpty(), entries.isEmpty(), elements.isEmpty()))
        multiset.add("d", 4)
        trace.addAll(listOf(elements.toList(), entryPairsKt(entries)))
        return trace
    }

    private fun guavaOccurrenceTrace(): List<Any?> {
        val multiset = GuavaLinkedHashMultiset.create<String>()
        val trace = mutableListOf<Any?>(
            failureName { multiset.add("a", -1) },
            failureName { multiset.remove("a", -1) },
            failureName { multiset.setCount("a", -1) },
            failureName { multiset.setCount("a", -1, 0) },
            failureName { multiset.setCount("a", 0, -1) },
            multiset.add("a", Int.MAX_VALUE),
            multiset.size,
            multiset.add("b", 1),
            multiset.size,
            failureName { multiset.add("a", 1) },
        )
        val small = GuavaLinkedHashMultiset.create(listOf("x", "x", "x"))
        val iterator = small.iterator()
        trace.add(iterator.next())
        iterator.remove()
        trace.addAll(listOf(small.count("x"), small.size))
        return trace
    }

    private fun guavaComputedTrace(): List<Any?> {
        val first = GuavaLinkedHashMultiset.create(listOf("a", "a", "b"))
        val second = GuavaLinkedHashMultiset.create(listOf("a", "c", "c", "c"))
        val union = GuavaMultisets.union(first, second)
        val intersection = GuavaMultisets.intersection(first, second)
        val sum = GuavaMultisets.sum(first, second)
        val difference = GuavaMultisets.difference(first, second)
        val trace = mutableListOf<Any?>(computedCounts(union, intersection, sum, difference))
        first.add("c", 2)
        second.add("b", 4)
        trace.add(computedCounts(union, intersection, sum, difference))
        trace.addAll(listOf(
            failureName { union.add("z", 1) },
            failureName { intersection.remove("a", 1) },
            failureName { sum.elementSet().remove("a") },
            failureName { difference.elementSet().clear() },
        ))
        return trace
    }

    private fun guavaKtComputedTrace(): List<Any?> {
        val first = GuavaKtLinkedHashMultiset.create(listOf("a", "a", "b"))
        val second = GuavaKtLinkedHashMultiset.create(listOf("a", "c", "c", "c"))
        val union = GuavaKtMultisets.union(first, second)
        val intersection = GuavaKtMultisets.intersection(first, second)
        val sum = GuavaKtMultisets.sum(first, second)
        val difference = GuavaKtMultisets.difference(first, second)
        val trace = mutableListOf<Any?>(computedCountsKt(union, intersection, sum, difference))
        first.add("c", 2)
        second.add("b", 4)
        trace.add(computedCountsKt(union, intersection, sum, difference))
        trace.addAll(listOf(
            failureName { union.add("z", 1) },
            failureName { intersection.remove("a", 1) },
            failureName { (sum.elementSet() as MutableSet).remove("a") },
            failureName { (difference.elementSet() as MutableSet).clear() },
        ))
        return trace
    }

    private fun computedCounts(
        union: com.google.common.collect.Multiset<String>,
        intersection: com.google.common.collect.Multiset<String>,
        sum: com.google.common.collect.Multiset<String>,
        difference: com.google.common.collect.Multiset<String>,
    ): List<Any?> = listOf(
        listOf("a", "b", "c").map(union::count), union.size,
        listOf("a", "b", "c").map(intersection::count), intersection.size,
        listOf("a", "b", "c").map(sum::count), sum.size,
        listOf("a", "b", "c").map(difference::count), difference.size,
    )

    private fun computedCountsKt(
        union: GuavaKtMultiset<String>,
        intersection: GuavaKtMultiset<String>,
        sum: GuavaKtMultiset<String>,
        difference: GuavaKtMultiset<String>,
    ): List<Any?> = listOf(
        listOf("a", "b", "c").map(union::count), union.size,
        listOf("a", "b", "c").map(intersection::count), intersection.size,
        listOf("a", "b", "c").map(sum::count), sum.size,
        listOf("a", "b", "c").map(difference::count), difference.size,
    )

    private fun guavaFilteredTrace(): List<Any?> {
        val source = GuavaLinkedHashMultiset.create(listOf("a", "a", "b"))
        val filtered = GuavaMultisets.filter(source) { it != "b" }
        val elements = filtered.elementSet()
        val trace = mutableListOf<Any?>(filtered.count("a"), filtered.count("b"), elements.toList())
        source.add("c", 3)
        trace.addAll(listOf(filtered.count("c"), elements.toList(), filtered.add("d", 2)))
        trace.add(failureName { filtered.add("b", 1) })
        trace.addAll(listOf(filtered.remove("a", 1), source.count("a"), elements.remove("c"), source.count("c")))
        return trace
    }

    private fun guavaKtFilteredTrace(): List<Any?> {
        val source = GuavaKtLinkedHashMultiset.create(listOf("a", "a", "b"))
        val filtered = GuavaKtMultisets.filter(source) { it != "b" }
        val elements = filtered.elementSet() as MutableSet<String>
        val trace = mutableListOf<Any?>(filtered.count("a"), filtered.count("b"), elements.toList())
        source.add("c", 3)
        trace.addAll(listOf(filtered.count("c"), elements.toList(), filtered.add("d", 2)))
        trace.add(failureName { filtered.add("b", 1) })
        trace.addAll(listOf(filtered.remove("a", 1), source.count("a"), elements.remove("c"), source.count("c")))
        return trace
    }

    private fun guavaKtOccurrenceTrace(): List<Any?> {
        val multiset = GuavaKtLinkedHashMultiset.create<String>()
        val trace = mutableListOf<Any?>(
            failureName { multiset.add("a", -1) },
            failureName { multiset.remove("a", -1) },
            failureName { multiset.setCount("a", -1) },
            failureName { multiset.setCount("a", -1, 0) },
            failureName { multiset.setCount("a", 0, -1) },
            multiset.add("a", Int.MAX_VALUE),
            multiset.size,
            multiset.add("b", 1),
            multiset.size,
            failureName { multiset.add("a", 1) },
        )
        val small = GuavaKtLinkedHashMultiset.create(listOf("x", "x", "x"))
        val iterator = small.iterator()
        trace.add(iterator.next())
        iterator.remove()
        trace.addAll(listOf(small.count("x"), small.size))
        return trace
    }

    private fun entryPairs(entries: Set<com.google.common.collect.Multiset.Entry<String>>) =
        entries.map { it.element to it.count }

    private fun entryPairsKt(entries: Set<GuavaKtMultiset.Entry<String>>) =
        entries.map { it.getElement() to it.getCount() }

    private fun failureName(block: () -> Unit): String? =
        try {
            block()
            null
        } catch (failure: Throwable) {
            failure::class.simpleName
        }
}
