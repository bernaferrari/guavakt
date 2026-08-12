package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImmutableMultimapContractTest {
    @Test
    fun listMultimapOrdersSnapshotsAndCachesInverse() {
        val source = ArrayListMultimap.create<String, Int>().apply {
            put("a", 1); put("b", 2); put("a", 3); put("a", 1)
        }
        val multimap = ImmutableListMultimap.copyOf(source)
        source.clear()
        assertEquals(listOf("a" to 1, "a" to 3, "a" to 1, "b" to 2), multimap.entries().map { it.key to it.value })
        assertSame(multimap, ImmutableListMultimap.copyOf(multimap))
        assertSame(multimap.inverse(), multimap.inverse())
        assertSame(multimap, multimap.inverse().inverse())
        assertEquals(3, multimap.keys().count("a"))
    }

    @Test
    fun listMultimapRejectsNullAndNestedMutation() {
        val multimap = ImmutableListMultimap.of("a", 1, "a", 2)
        assertFailsWith<UnsupportedOperationException> { multimap["a"].add(3) }
        assertFailsWith<UnsupportedOperationException> { (multimap.asMap()["a"] as MutableList<Int>).add(3) }
        assertFailsWith<UnsupportedOperationException> {
            (multimap.entries().first() as MutableMap.MutableEntry<String, Int>).setValue(3)
        }
        assertFailsWith<UnsupportedOperationException> { multimap.keys().remove("a", 1) }
        assertFailsWith<UnsupportedOperationException> { (multimap.values() as MutableCollection<Int>).remove(1) }
        assertFailsWith<UnsupportedOperationException> { (multimap.asMap() as MutableMap<String, List<Int>>).remove("a") }
        assertFailsWith<UnsupportedOperationException> { (multimap.keySet() as MutableSet<String>).remove("a") }
        assertFailsWith<NullPointerException> { ImmutableListMultimap.builder<String?, Int>().put(null, 1) }
        assertFailsWith<NullPointerException> { ImmutableListMultimap.builder<String, Int?>().put("a", null) }
    }

    @Test
    fun setMultimapDeduplicatesAndUsesComparatorOrderedValueSets() {
        val descending = Comparator<Int> { first, second -> second.compareTo(first) }
        val multimap = ImmutableSetMultimap.builder<String, Int>()
            .put("b", 3).put("a", 2).put("b", 6).put("b", 6)
            .orderKeysBy(reverseOrder()).orderValuesBy(descending).build()
        assertEquals(3, multimap.size())
        assertEquals(listOf("b", "a"), multimap.keySet().toList())
        assertEquals(listOf(6, 3), multimap["b"].toList())
        assertTrue(multimap["missing"] is ImmutableSortedSet)
        assertSame(multimap, multimap.inverse().inverse())
        assertEquals(setOf(Maps.immutableEntry("b", 3), Maps.immutableEntry("b", 6), Maps.immutableEntry("a", 2)), multimap.entries())
    }

    @Test
    fun setMultimapRejectsNullAndNestedMutation() {
        val multimap = ImmutableSetMultimap.of("a", 1, "a", 2)
        assertFailsWith<UnsupportedOperationException> { multimap["a"].remove(1) }
        assertFailsWith<UnsupportedOperationException> { (multimap.asMap()["a"] as MutableSet<Int>).add(3) }
        assertFailsWith<UnsupportedOperationException> {
            (multimap.entries().first() as MutableMap.MutableEntry<String, Int>).setValue(3)
        }
        assertFailsWith<UnsupportedOperationException> { multimap.keys().remove("a", 1) }
        assertFailsWith<UnsupportedOperationException> { (multimap.entries() as MutableSet<Map.Entry<String, Int>>).clear() }
        assertFailsWith<UnsupportedOperationException> { (multimap.asMap() as MutableMap<String, Set<Int>>).clear() }
        assertFailsWith<NullPointerException> { ImmutableSetMultimap.builder<String?, Int>().put(null, 1) }
        assertFailsWith<NullPointerException> { ImmutableSetMultimap.builder<String, Int?>().put("a", null) }
    }

    @Test
    fun buildersAreReusableAndComparatorsKeepStableEquivalentElements() {
        val keyLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val listBuilder = ImmutableListMultimap.builder<String, Int>()
            .put("bb", 1).put("c", 2).put("aa", 3).put("d", 4)
            .orderKeysBy(keyLength)
        val firstList = listBuilder.build()
        listBuilder.put("eee", 5)
        assertEquals(listOf("c", "d", "bb", "aa"), firstList.keySet().toList())
        assertEquals(listOf("c", "d", "bb", "aa", "eee"), listBuilder.build().keySet().toList())

        val valueLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val setBuilder = ImmutableSetMultimap.builder<String, String>()
            .putAll("k", "aa", "bb", "c", "d")
            .orderValuesBy(valueLength)
        val firstSet = setBuilder.build()
        setBuilder.put("k", "eee")
        assertEquals(listOf("c", "aa"), firstSet["k"].toList())
        assertEquals(listOf("c", "aa", "eee"), setBuilder.build()["k"].toList())
    }

    @Test
    fun copyGroupsLinkedListEntriesAndBaseFactoriesRemainListBacked() {
        val source = LinkedListMultimap.create<String, Int>().apply {
            put("a", 1); put("b", 2); put("a", 3)
        }
        val copied = ImmutableListMultimap.copyOf(source)
        assertEquals(listOf("a" to 1, "a" to 3, "b" to 2), copied.entries().map { it.key to it.value })

        val fromBaseBuilder = ImmutableMultimap.builder<String, Int>().put("a", 1).put("a", 1).build()
        assertTrue(fromBaseBuilder is ImmutableListMultimap)
        assertEquals(listOf(1, 1), fromBaseBuilder["a"].toList())
        assertSame(fromBaseBuilder, ImmutableMultimap.copyOf(fromBaseBuilder))
        val empty = ImmutableMultimap.of<String, Int>()
        assertTrue((empty as Any) === (empty.inverse() as Any))
    }
}
