package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MultimapsLiveTransformFilterTest {
    @Test
    fun filterEntriesIsLiveAndWritesRemovalsThroughEveryPrimaryView() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2, 4))
        source.putAll("b", listOf(2, 3))
        val filtered = Multimaps.filterEntries(source) { it.key == "a" && it.value % 2 == 0 }

        assertEquals(listOf(2, 4), filtered.get("a").toList())
        source.put("a", 6)
        assertEquals(listOf(2, 4, 6), filtered.get("a").toList())
        assertTrue(filtered.get("a").remove(4))
        assertFalse(source.containsEntry("a", 4))
        assertTrue((filtered.values() as MutableCollection<Int>).remove(2))
        assertFalse(source.containsEntry("a", 2))

        filtered.get("a").clear()
        assertEquals(listOf(1), source.get("a").toList())
        assertEquals(listOf(2, 3), source.get("b").toList())
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filteredAddsValidateAtomicallyAndNestedFiltersStayRemovalCapable() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2, 3, 4))
        val even = Multimaps.filterValues(source) { it % 2 == 0 }
        val aboveTwo = Multimaps.filterEntries(even) { it.value > 2 }

        assertEquals(listOf(4), aboveTwo.values().toList())
        assertFailsWith<IllegalArgumentException> { even.putAll("a", listOf(6, 7, 8)) }
        assertEquals(listOf(1, 2, 3, 4), source.get("a").toList())
        assertTrue(aboveTwo.remove("a", 4))
        assertEquals(listOf(1, 2, 3), source.get("a").toList())

        val iterator = even.get("a").iterator()
        assertEquals(2, iterator.next())
        assertFailsWith<UnsupportedOperationException> { iterator.remove() }
    }

    @Test
    fun filteredAsMapIsLiveAndHidesKeysWithoutMatchingValues() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2))
        source.put("b", 1)
        val even = Multimaps.filterValues(source) { it % 2 == 0 }
        val asMap = even.asMap()

        assertEquals(listOf(2), asMap["a"]?.toList())
        assertNull(asMap["b"])
        source.put("b", 4)
        assertEquals(listOf(4), asMap["b"]?.toList())
        assertEquals(listOf(4), (asMap as MutableMap<String, Collection<Int>>).remove("b")?.toList())
        assertEquals(listOf(1), source.get("b").toList())
    }

    @Test
    fun filteredSetAndMapViewsPreserveEqualityAndNullableKeys() {
        val setSource = HashMultimap.create<String?, Int>()
        setSource.put(null, 2)
        setSource.put(null, 3)
        setSource.put("a", 4)
        val even = Multimaps.filterValues(setSource) { it % 2 == 0 }

        assertTrue(even.get(null) is Set<*>)
        assertEquals(setOf(2), even.get(null).toSet())
        assertEquals(mapOf(null to setOf(2), "a" to setOf(4)), even.asMap().mapValues { it.value.toSet() })
        assertEquals(setOf(null, "a"), even.asMap().entries.map { it.key }.toSet())
        assertEquals(setOf(2), even.removeAll(null).toSet())
        assertEquals(setOf(3), setSource.get(null).toSet())
    }

    @Test
    fun filterKeysPreservesListMultimapAndIndexedLiveMutation() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("allowed", listOf(1, 3))
        source.put("hidden", 9)
        val filtered: ListMultimap<String, Int> =
            Multimaps.filterKeys(source) { it == "allowed" }

        filtered.get("allowed").add(1, 2)
        assertEquals(listOf(1, 2, 3), source.get("allowed"))
        val iterator = filtered.get("allowed").iterator()
        assertEquals(1, iterator.next())
        iterator.remove()
        assertEquals(listOf(2, 3), source.get("allowed"))
        assertEquals(mapOf("allowed" to listOf(2, 3)), filtered.asMap())

        assertFailsWith<IllegalArgumentException> { filtered.get("hidden").add(8) }
        assertFailsWith<IllegalArgumentException> { filtered.get("hidden").addAll(emptyList()) }
        assertFailsWith<IndexOutOfBoundsException> { filtered.get("hidden").add(1, 8) }
        assertEquals(emptyList(), filtered.replaceValues("hidden", emptyList()))
        assertEquals(listOf(9), source.get("hidden"))
    }

    @Test
    fun transformValuesIsLazyLiveAndRemovalCapable() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2))
        source.put("b", 3)
        var calls = 0
        val transformed = Multimaps.transformValues(source) {
            calls++
            it * 10
        }

        assertEquals(0, calls)
        assertEquals(3, transformed.size())
        assertEquals(0, calls)
        assertEquals(listOf(10, 20), transformed.get("a").toList())
        assertEquals(2, calls)

        source.put("a", 4)
        assertEquals(listOf(10, 20, 40), transformed.get("a").toList())
        assertTrue(transformed.get("a").remove(20))
        assertEquals(listOf(1, 4), source.get("a").toList())

        val iterator = transformed.get("a").iterator()
        assertEquals(10, iterator.next())
        iterator.remove()
        assertEquals(listOf(4), source.get("a").toList())
        assertFailsWith<UnsupportedOperationException> { transformed.put("a", 50) }
    }

    @Test
    fun transformEntriesUsesKeysAndRemovedResultsRemainLazySnapshots() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2))
        var calls = 0
        val transformed = Multimaps.transformEntries(source) { key, value ->
            calls++
            "$key$value"
        }

        val removed = transformed.removeAll("a")
        assertEquals(0, calls)
        assertTrue(source.isEmpty())
        assertEquals(listOf("a1", "a2"), removed)
        assertEquals(2, calls)
        source.put("a", 9)
        assertEquals(listOf("a1", "a2"), removed)
        assertEquals(listOf("a9"), transformed.get("a").toList())
    }

    @Test
    fun transformedMapEntriesHaveMapEqualityAndNegativeIndicesFailWithoutMutation() {
        val source = ArrayListMultimap.create<String, Int>()
        source.putAll("a", listOf(1, 2))
        val transformed = Multimaps.transformValues(source) { it * 10 }

        assertEquals(mapOf("a" to listOf(10, 20)), transformed.asMap())
        assertFailsWith<IndexOutOfBoundsException> { transformed.get("a").removeAt(-1) }
        assertEquals(listOf(1, 2), source.get("a").toList())
    }
}
