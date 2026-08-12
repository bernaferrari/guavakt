package com.bernaferrari.guavakt.parity

import com.google.common.collect.ArrayListMultimap as GuavaArrayListMultimap
import com.google.common.collect.Multimaps as GuavaMultimaps
import com.bernaferrari.guavakt.collect.ArrayListMultimap as GuavaKtArrayListMultimap
import com.bernaferrari.guavakt.collect.Multimaps as GuavaKtMultimaps
import kotlin.test.Test
import kotlin.test.assertEquals

class MultimapsViewDifferentialTest {
    @Test
    fun filteredEntryViewMutationTraceMatchesGuava() {
        assertEquals(guavaFilterTrace(), guavaKtFilterTrace())
    }

    @Test
    fun nestedFilterAndAtomicValidationTraceMatchesGuava() {
        assertEquals(guavaNestedFilterTrace(), guavaKtNestedFilterTrace())
    }

    @Test
    fun transformedValueViewMutationAndLazinessTraceMatchesGuava() {
        assertEquals(guavaTransformTrace(), guavaKtTransformTrace())
    }

    @Test
    fun keyAwareTransformTraceMatchesGuava() {
        val guavaSource = GuavaArrayListMultimap.create<String, Int>()
        guavaSource.putAll("a", listOf(1, 2))
        guavaSource.put("b", 3)
        val guava = GuavaMultimaps.transformEntries(guavaSource) { key, value -> "$key$value" }

        val guavaKtSource = GuavaKtArrayListMultimap.create<String, Int>()
        guavaKtSource.putAll("a", listOf(1, 2))
        guavaKtSource.put("b", 3)
        val guavaKt = GuavaKtMultimaps.transformEntries(guavaKtSource) { key, value -> "$key$value" }

        val guavaTrace = mutableListOf<Any?>()
        guavaTrace.add(guava.entries().map { it.key to it.value })
        guavaTrace.add(guava.removeAll("a").toList())
        guavaTrace.add(guavaSource.entries().map { it.key to it.value })

        val guavaKtTrace = mutableListOf<Any?>()
        guavaKtTrace.add(guavaKt.entries().map { it.key to it.value })
        guavaKtTrace.add(guavaKt.removeAll("a").toList())
        guavaKtTrace.add(guavaKtSource.entries().map { it.key to it.value })

        assertEquals(guavaTrace, guavaKtTrace)
    }

    @Test
    fun filteredSetNullableKeyAndMapEqualityMatchGuava() {
        val guavaSource = com.google.common.collect.HashMultimap.create<String?, Int>()
        guavaSource.put(null, 2)
        guavaSource.put(null, 3)
        guavaSource.put("a", 4)
        val guava = GuavaMultimaps.filterValues(guavaSource) { it % 2 == 0 }

        val guavaKtSource = com.bernaferrari.guavakt.collect.HashMultimap.create<String?, Int>()
        guavaKtSource.put(null, 2)
        guavaKtSource.put(null, 3)
        guavaKtSource.put("a", 4)
        val guavaKt = GuavaKtMultimaps.filterValues(guavaKtSource) { it % 2 == 0 }

        val guavaTrace = listOf(
            guava.get(null) is Set<*>,
            guava.get(null).toSet(),
            guava.asMap().mapValues { it.value.toSet() },
            guava.removeAll(null).toSet(),
            guavaSource.get(null).toSet(),
        )
        val guavaKtTrace = listOf(
            guavaKt.get(null) is Set<*>,
            guavaKt.get(null).toSet(),
            guavaKt.asMap().mapValues { it.value.toSet() },
            guavaKt.removeAll(null).toSet(),
            guavaKtSource.get(null).toSet(),
        )
        assertEquals(guavaTrace, guavaKtTrace)
    }

    @Test
    fun transformedNegativeIndexFailureAndMapEqualityMatchGuava() {
        val guavaSource = GuavaArrayListMultimap.create<String, Int>()
        guavaSource.putAll("a", listOf(1, 2))
        val guava = GuavaMultimaps.transformValues(guavaSource) { it * 10 }
        val guavaTrace = listOf(
            guava.asMap() == mapOf("a" to listOf(10, 20)),
            failureName { (guava.get("a") as MutableList<Int>).removeAt(-1) },
            guavaSource.get("a").toList(),
        )

        val guavaKtSource = GuavaKtArrayListMultimap.create<String, Int>()
        guavaKtSource.putAll("a", listOf(1, 2))
        val guavaKt = GuavaKtMultimaps.transformValues(guavaKtSource) { it * 10 }
        val guavaKtTrace = listOf(
            guavaKt.asMap() == mapOf("a" to listOf(10, 20)),
            failureName { guavaKt.get("a").removeAt(-1) },
            guavaKtSource.get("a").toList(),
        )
        assertEquals(guavaTrace, guavaKtTrace)
    }

    @Test
    fun keyFilteredListMultimapIndexedMutationAndFailuresMatchGuava() {
        val guavaSource = GuavaArrayListMultimap.create<String, Int>()
        guavaSource.putAll("allowed", listOf(1, 3))
        guavaSource.put("hidden", 9)
        val guava: com.google.common.collect.ListMultimap<String, Int> =
            GuavaMultimaps.filterKeys(guavaSource) { it == "allowed" }

        val guavaKtSource = GuavaKtArrayListMultimap.create<String, Int>()
        guavaKtSource.putAll("allowed", listOf(1, 3))
        guavaKtSource.put("hidden", 9)
        val guavaKt: com.bernaferrari.guavakt.collect.ListMultimap<String, Int> =
            GuavaKtMultimaps.filterKeys(guavaKtSource) { it == "allowed" }

        assertEquals(
            keyFilteredListTrace(guava, guavaSource),
            keyFilteredListTrace(guavaKt, guavaKtSource),
        )
    }

    private fun keyFilteredListTrace(
        view: com.google.common.collect.ListMultimap<String, Int>,
        source: com.google.common.collect.ListMultimap<String, Int>,
    ): List<Any?> {
        val trace = mutableListOf<Any?>()
        view.get("allowed").add(1, 2)
        trace.add(source.get("allowed").toList())
        val iterator = view.get("allowed").iterator()
        trace.add(iterator.next())
        iterator.remove()
        trace.add(source.get("allowed").toList())
        trace.add(view.asMap().mapValues { it.value.toList() })
        trace.add(failureName { view.get("hidden").add(8) })
        trace.add(failureName { view.get("hidden").addAll(emptyList()) })
        trace.add(failureName { view.get("hidden").add(1, 8) })
        trace.add(failureName { view.replaceValues("hidden", emptyList()) })
        trace.add(source.get("hidden").toList())
        return trace
    }

    private fun keyFilteredListTrace(
        view: com.bernaferrari.guavakt.collect.ListMultimap<String, Int>,
        source: com.bernaferrari.guavakt.collect.ListMultimap<String, Int>,
    ): List<Any?> {
        val trace = mutableListOf<Any?>()
        view.get("allowed").add(1, 2)
        trace.add(source.get("allowed").toList())
        val iterator = view.get("allowed").iterator()
        trace.add(iterator.next())
        iterator.remove()
        trace.add(source.get("allowed").toList())
        trace.add(view.asMap().mapValues { it.value.toList() })
        trace.add(failureName { view.get("hidden").add(8) })
        trace.add(failureName { view.get("hidden").addAll(emptyList()) })
        trace.add(failureName { view.get("hidden").add(1, 8) })
        trace.add(failureName { view.replaceValues("hidden", emptyList()) })
        trace.add(source.get("hidden").toList())
        return trace
    }

    private fun guavaFilterTrace(): List<Any?> {
        val source = GuavaArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2, 4))
        source.putAll("b", listOf(2, 3))
        val view = GuavaMultimaps.filterEntries(source) { it.key == "a" && it.value % 2 == 0 }
        val trace = mutableListOf<Any?>()
        trace.add(view.entries().map { it.key to it.value })
        source.put("a", 6)
        trace.add(view.get("a").toList())
        trace.add(view.get("a").remove(4))
        trace.add(source.get("a").toList())
        trace.add(view.asMap().remove("a")?.toList())
        trace.add(source.entries().map { it.key to it.value })
        trace.add(failureName {
            val iterator = view.entries().iterator()
            iterator.next()
            iterator.remove()
        })
        return trace
    }

    private fun guavaKtFilterTrace(): List<Any?> {
        val source = GuavaKtArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2, 4))
        source.putAll("b", listOf(2, 3))
        val view = GuavaKtMultimaps.filterEntries(source) { it.key == "a" && it.value % 2 == 0 }
        val trace = mutableListOf<Any?>()
        trace.add(view.entries().map { it.key to it.value })
        source.put("a", 6)
        trace.add(view.get("a").toList())
        trace.add(view.get("a").remove(4))
        trace.add(source.get("a").toList())
        @Suppress("UNCHECKED_CAST")
        trace.add((view.asMap() as MutableMap<String, Collection<Int>>).remove("a")?.toList())
        trace.add(source.entries().map { it.key to it.value })
        trace.add(failureName {
            val iterator = view.entries().iterator() as MutableIterator<*>
            iterator.next()
            iterator.remove()
        })
        return trace
    }

    private fun guavaNestedFilterTrace(): List<Any?> {
        val source = GuavaArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2, 3, 4))
        val even = GuavaMultimaps.filterValues(source) { it % 2 == 0 }
        val aboveTwo = GuavaMultimaps.filterEntries(even) { it.value > 2 }
        val trace = mutableListOf<Any?>()
        trace.add(aboveTwo.values().toList())
        trace.add(failureName { even.putAll("a", listOf(6, 7, 8)) })
        trace.add(source.get("a").toList())
        trace.add(aboveTwo.remove("a", 4))
        trace.add(source.get("a").toList())
        return trace
    }

    private fun guavaKtNestedFilterTrace(): List<Any?> {
        val source = GuavaKtArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2, 3, 4))
        val even = GuavaKtMultimaps.filterValues(source) { it % 2 == 0 }
        val aboveTwo = GuavaKtMultimaps.filterEntries(even) { it.value > 2 }
        val trace = mutableListOf<Any?>()
        trace.add(aboveTwo.values().toList())
        trace.add(failureName { even.putAll("a", listOf(6, 7, 8)) })
        trace.add(source.get("a").toList())
        trace.add(aboveTwo.remove("a", 4))
        trace.add(source.get("a").toList())
        return trace
    }

    private fun guavaTransformTrace(): List<Any?> {
        val source = GuavaArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2))
        source.put("b", 3)
        var calls = 0
        val view = GuavaMultimaps.transformValues(source) {
            calls++
            it * 10
        }
        val trace = mutableListOf<Any?>()
        trace.add(calls)
        trace.add(view.size())
        trace.add(calls)
        trace.add(view.get("a").toList())
        trace.add(calls)
        source.put("a", 4)
        trace.add(view.get("a").remove(20))
        trace.add(source.get("a").toList())
        val iterator = view.get("a").iterator()
        trace.add(iterator.next())
        iterator.remove()
        trace.add(source.get("a").toList())
        trace.add(failureName { view.put("a", 50) })
        trace.add(view.removeAll("b").toList())
        trace.add(source.entries().map { it.key to it.value })
        return trace
    }

    private fun guavaKtTransformTrace(): List<Any?> {
        val source = GuavaKtArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2))
        source.put("b", 3)
        var calls = 0
        val view = GuavaKtMultimaps.transformValues(source) {
            calls++
            it * 10
        }
        val trace = mutableListOf<Any?>()
        trace.add(calls)
        trace.add(view.size())
        trace.add(calls)
        trace.add(view.get("a").toList())
        trace.add(calls)
        source.put("a", 4)
        trace.add(view.get("a").remove(20))
        trace.add(source.get("a").toList())
        val iterator = view.get("a").iterator()
        trace.add(iterator.next())
        iterator.remove()
        trace.add(source.get("a").toList())
        trace.add(failureName { view.put("a", 50) })
        trace.add(view.removeAll("b").toList())
        trace.add(source.entries().map { it.key to it.value })
        return trace
    }

    private fun failureName(block: () -> Unit): String =
        try {
            block()
            "none"
        } catch (failure: Throwable) {
            failure::class.simpleName ?: "unknown"
        }
}
