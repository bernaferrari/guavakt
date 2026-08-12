package com.bernaferrari.guavakt.parity

import com.google.common.collect.ImmutableListMultimap as GuavaImmutableListMultimap
import com.google.common.collect.ImmutableSetMultimap as GuavaImmutableSetMultimap
import com.google.common.collect.ImmutableSortedSet as GuavaImmutableSortedSet
import com.google.common.collect.LinkedListMultimap as GuavaLinkedListMultimap
import com.bernaferrari.guavakt.collect.LinkedListMultimap as GuavaKtLinkedListMultimap
import com.bernaferrari.guavakt.collect.ImmutableListMultimap as GuavaKtImmutableListMultimap
import com.bernaferrari.guavakt.collect.ImmutableSetMultimap as GuavaKtImmutableSetMultimap
import com.bernaferrari.guavakt.collect.ImmutableSortedSet as GuavaKtImmutableSortedSet
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableMultimapDifferentialTest {
    @Test
    fun listMultimapOrderingCopiesEntriesAndInverseIdentityMatchGuava() {
        val guava = GuavaImmutableListMultimap.builder<String, Int>()
            .put("b", 3).put("d", 2).put("a", 5)
            .orderKeysBy(reverseOrder()).orderValuesBy(reverseOrder())
            .put("c", 4).put("a", 2).put("b", 6).put("b", 6).build()
        val guavaKt = GuavaKtImmutableListMultimap.builder<String, Int>()
            .put("b", 3).put("d", 2).put("a", 5)
            .orderKeysBy(reverseOrder()).orderValuesBy(reverseOrder())
            .put("c", 4).put("a", 2).put("b", 6).put("b", 6).build()

        assertEquals(
            listOf(
                guava.keySet().toList(), guava.values().toList(), guava["b"].toList(),
                guava.entries().map { it.key to it.value }, guava.keys().entrySet().map { it.element to it.count },
                guava["b"] === guava.asMap()["b"], GuavaImmutableListMultimap.copyOf(guava) === guava,
                guava.inverse().entries().map { it.key to it.value }, guava.inverse() === guava.inverse(),
                guava.inverse().inverse() === guava, guava.toString(),
            ),
            listOf(
                guavaKt.keySet().toList(), guavaKt.values().toList(), guavaKt["b"].toList(),
                guavaKt.entries().map { it.key to it.value }, guavaKt.keys().entrySet().map { it.getElement() to it.getCount() },
                guavaKt["b"] === guavaKt.asMap()["b"], GuavaKtImmutableListMultimap.copyOf(guavaKt) === guavaKt,
                guavaKt.inverse().entries().map { it.key to it.value }, guavaKt.inverse() === guavaKt.inverse(),
                guavaKt.inverse().inverse() === guavaKt, guavaKt.toString(),
            ),
        )
    }

    @Test
    fun listMultimapNullAndDeepMutationFailuresMatchGuava() {
        val guava = GuavaImmutableListMultimap.of("a", 1, "a", 2)
        val guavaKt = GuavaKtImmutableListMultimap.of("a", 1, "a", 2)
        assertEquals(
            listOf(
                failureName { guava["a"].add(3) }, failureName { guava.keys().remove("a", 1) },
                failureName { guava.values().remove(1) }, failureName { guava.entries().iterator().apply { next(); remove() } },
                failureName { (guava.asMap() as MutableMap<String, Collection<Int>>).remove("a") },
                failureName { (guava.asMap()["a"] as MutableList<Int>).add(3) },
                failureName { (guava.entries().first() as MutableMap.MutableEntry<String, Int>).setValue(3) },
                failureName { guava.keySet().remove("a") },
                GuavaImmutableNullHarness.listMultimapNullKeyFailure(),
                GuavaImmutableNullHarness.listMultimapNullValueFailure(),
            ),
            listOf(
                failureName { guavaKt["a"].add(3) }, failureName { guavaKt.keys().remove("a", 1) },
                failureName { (guavaKt.values() as MutableCollection<Int>).remove(1) },
                failureName { (guavaKt.entries() as MutableCollection<Map.Entry<String, Int>>).iterator().apply { next(); remove() } },
                failureName { (guavaKt.asMap() as MutableMap<String, List<Int>>).remove("a") },
                failureName { (guavaKt.asMap()["a"] as MutableList<Int>).add(3) },
                failureName { (guavaKt.entries().first() as MutableMap.MutableEntry<String, Int>).setValue(3) },
                failureName { (guavaKt.keySet() as MutableSet<String>).remove("a") },
                failureName { GuavaKtImmutableListMultimap.builder<String?, Int>().put(null, 1) },
                failureName { GuavaKtImmutableListMultimap.builder<String, Int?>().put("key", null) },
            ),
        )
    }

    @Test
    fun setMultimapDeduplicationSortedValuesAndInverseMatchGuava() {
        val descending = Comparator<Int> { first, second -> second.compareTo(first) }
        val guava = GuavaImmutableSetMultimap.builder<String, Int>()
            .put("b", 3).put("d", 2).put("a", 5)
            .orderKeysBy(reverseOrder()).orderValuesBy(descending)
            .put("c", 4).put("a", 2).put("b", 6).put("b", 6).build()
        val guavaKt = GuavaKtImmutableSetMultimap.builder<String, Int>()
            .put("b", 3).put("d", 2).put("a", 5)
            .orderKeysBy(reverseOrder()).orderValuesBy(descending)
            .put("c", 4).put("a", 2).put("b", 6).put("b", 6).build()

        assertEquals(
            listOf(
                guava.size(), guava.keySet().toList(), guava.values().toList(), guava["b"].toList(),
                guava.entries().map { it.key to it.value }, guava["missing"] is GuavaImmutableSortedSet,
                guava["missing"].toList(), GuavaImmutableSetMultimap.copyOf(guava) === guava,
                guava.inverse().entries().map { it.key to it.value }, guava.inverse() === guava.inverse(),
                guava.inverse().inverse() === guava,
            ),
            listOf(
                guavaKt.size(), guavaKt.keySet().toList(), guavaKt.values().toList(), guavaKt["b"].toList(),
                guavaKt.entries().map { it.key to it.value }, guavaKt["missing"] is GuavaKtImmutableSortedSet,
                guavaKt["missing"].toList(), GuavaKtImmutableSetMultimap.copyOf(guavaKt) === guavaKt,
                guavaKt.inverse().entries().map { it.key to it.value }, guavaKt.inverse() === guavaKt.inverse(),
                guavaKt.inverse().inverse() === guavaKt,
            ),
        )
    }

    @Test
    fun setMultimapNullAndDeepMutationFailuresMatchGuava() {
        val guava = GuavaImmutableSetMultimap.of("a", 1, "a", 2)
        val guavaKt = GuavaKtImmutableSetMultimap.of("a", 1, "a", 2)
        assertEquals(
            listOf(
                failureName { guava["a"].add(3) }, failureName { guava.keys().remove("a", 1) },
                failureName { guava.values().remove(1) }, failureName { guava.entries().remove(guava.entries().first()) },
                failureName { (guava.asMap() as MutableMap<String, Collection<Int>>).remove("a") },
                failureName { (guava.asMap()["a"] as MutableSet<Int>).add(3) },
                failureName { (guava.entries().first() as MutableMap.MutableEntry<String, Int>).setValue(3) },
                GuavaImmutableNullHarness.setMultimapNullKeyFailure(),
                GuavaImmutableNullHarness.setMultimapNullValueFailure(),
            ),
            listOf(
                failureName { guavaKt["a"].add(3) }, failureName { guavaKt.keys().remove("a", 1) },
                failureName { (guavaKt.values() as MutableCollection<Int>).remove(1) },
                failureName { (guavaKt.entries() as MutableSet<Map.Entry<String, Int>>).remove(guavaKt.entries().first()) },
                failureName { (guavaKt.asMap() as MutableMap<String, Set<Int>>).remove("a") },
                failureName { (guavaKt.asMap()["a"] as MutableSet<Int>).add(3) },
                failureName { (guavaKt.entries().first() as MutableMap.MutableEntry<String, Int>).setValue(3) },
                failureName { GuavaKtImmutableSetMultimap.builder<String?, Int>().put(null, 1) },
                failureName { GuavaKtImmutableSetMultimap.builder<String, Int?>().put("key", null) },
            ),
        )
    }

    @Test
    fun groupedCopiesBuilderReuseAndComparatorEquivalenceMatchGuava() {
        val guavaSource = GuavaLinkedListMultimap.create<String, Int>().apply {
            put("a", 1); put("b", 2); put("a", 3)
        }
        val guavaKtSource = GuavaKtLinkedListMultimap.create<String, Int>().apply {
            put("a", 1); put("b", 2); put("a", 3)
        }
        val keyLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val valueLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }

        val guavaListBuilder = GuavaImmutableListMultimap.builder<String, Int>()
            .put("bb", 1).put("c", 2).put("aa", 3).put("d", 4).orderKeysBy(keyLength)
        val guavaKtListBuilder = GuavaKtImmutableListMultimap.builder<String, Int>()
            .put("bb", 1).put("c", 2).put("aa", 3).put("d", 4).orderKeysBy(keyLength)
        val guavaListFirst = guavaListBuilder.build()
        val guavaKtListFirst = guavaKtListBuilder.build()
        guavaListBuilder.put("eee", 5)
        guavaKtListBuilder.put("eee", 5)

        val guavaSetBuilder = GuavaImmutableSetMultimap.builder<String, String>()
            .putAll("k", "aa", "bb", "c", "d").orderValuesBy(valueLength)
        val guavaKtSetBuilder = GuavaKtImmutableSetMultimap.builder<String, String>()
            .putAll("k", "aa", "bb", "c", "d").orderValuesBy(valueLength)
        val guavaSetFirst = guavaSetBuilder.build()
        val guavaKtSetFirst = guavaKtSetBuilder.build()
        guavaSetBuilder.put("k", "eee")
        guavaKtSetBuilder.put("k", "eee")

        assertEquals(
            listOf(
                GuavaImmutableListMultimap.copyOf(guavaSource).entries().map { it.key to it.value },
                guavaListFirst.keySet().toList(), guavaListBuilder.build().keySet().toList(),
                guavaSetFirst["k"].toList(), guavaSetBuilder.build()["k"].toList(),
            ),
            listOf(
                GuavaKtImmutableListMultimap.copyOf(guavaKtSource).entries().map { it.key to it.value },
                guavaKtListFirst.keySet().toList(), guavaKtListBuilder.build().keySet().toList(),
                guavaKtSetFirst["k"].toList(), guavaKtSetBuilder.build()["k"].toList(),
            ),
        )
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
