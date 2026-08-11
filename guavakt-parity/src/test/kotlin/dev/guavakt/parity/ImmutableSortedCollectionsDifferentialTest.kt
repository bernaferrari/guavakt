package dev.guavakt.parity

import com.google.common.collect.ImmutableSortedMap as GuavaImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet as GuavaImmutableSortedSet
import dev.guavakt.collect.ImmutableSortedMap as GuavaKtImmutableSortedMap
import dev.guavakt.collect.ImmutableSortedSet as GuavaKtImmutableSortedSet
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableSortedCollectionsDifferentialTest {
    private val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }

    @Test
    fun sortedSetComparatorIdentityRangesNavigationAndFailuresMatchGuava() {
        val guava = GuavaImmutableSortedSet.orderedBy(byLength)
            .add("quick", "a", "in", "the", "over", "jumped", "fox")
            .build()
        val guavaKt = GuavaKtImmutableSortedSet.orderedBy(byLength)
            .add("quick", "a", "in", "the", "over", "jumped", "fox")
            .build()

        assertEquals(
            listOf(
                guava.toList(), guava.asList().toList(), guava.asList() === guava.asList(),
                guava.contains("cat"), guava.contains("dog"),
                guava.lower("xxxx"), guava.floor("xxxx"), guava.ceiling("xxxx"), guava.higher("xxxx"),
                guava.headSet("xxxx", true).toList(), guava.tailSet("xxxx", false).toList(),
                guava.subSet("xx", true, "xxxxx", false).toList(), guava.descendingSet().toList(),
                guava.descendingSet().descendingSet() === guava,
                failureName { guava.subSet("xxxxx", "xx") },
                failureName { guava.add("new") }, failureName { guava.iterator().apply { next(); remove() } },
                failureName { guava.pollFirst() },
            ),
            listOf(
                guavaKt.toList(), guavaKt.asList().toList(), guavaKt.asList() === guavaKt.asList(),
                guavaKt.contains("cat"), guavaKt.contains("dog"),
                guavaKt.lower("xxxx"), guavaKt.floor("xxxx"), guavaKt.ceiling("xxxx"), guavaKt.higher("xxxx"),
                guavaKt.headSet("xxxx", true).toList(), guavaKt.tailSet("xxxx", false).toList(),
                guavaKt.subSet("xx", true, "xxxxx", false).toList(), guavaKt.descendingSet().toList(),
                guavaKt.descendingSet().descendingSet() === guavaKt,
                failureName { guavaKt.subSet("xxxxx", "xx") },
                failureName { guavaKt.add("new") }, failureName { guavaKt.iterator().apply { next(); remove() } },
                failureName { guavaKt.pollFirst() },
            ),
        )
    }

    @Test
    fun sortedSetCopyEmptyIdentityAndNullFailureMatchGuava() {
        val guava = GuavaImmutableSortedSet.copyOf(listOf(3, 1, 2))
        val guavaKt = GuavaKtImmutableSortedSet.copyOf(listOf(3, 1, 2))
        assertEquals(
            listOf(
                GuavaImmutableSortedSet.copyOf(guava) === guava,
                guava.headSet(1) === GuavaImmutableSortedSet.of<Int>(),
                GuavaImmutableNullHarness.sortedSetNullFailure(),
            ),
            listOf(
                GuavaKtImmutableSortedSet.copyOf(guavaKt) === guavaKt,
                guavaKt.headSet(1) === GuavaKtImmutableSortedSet.of<Int>(),
                failureName {
                    GuavaKtImmutableSortedSet.orderedBy<String?>(Comparator { a, b -> (a ?: "").compareTo(b ?: "") })
                        .add(null).build()
                },
            ),
        )
    }

    @Test
    fun sortedMapOrderingComparatorEquivalenceRangesAndNavigationMatchGuava() {
        val guava = GuavaImmutableSortedMap.orderedBy<String, Int>(byLength)
            .put("a", 1).put("the", 3).put("quick", 5).put("jumped", 6).build()
        val guavaKt = GuavaKtImmutableSortedMap.orderedBy<String, Int>(byLength)
            .put("a", 1).put("the", 3).put("quick", 5).put("jumped", 6).build()

        assertEquals(
            listOf(
                guava.entries.map { it.key to it.value }, guava["cat"], guava.containsKey("fox"),
                guava.firstKey(), guava.lastKey(), guava.lowerKey("xxxx"), guava.floorKey("xxxx"),
                guava.ceilingKey("xxxx"), guava.higherKey("xxxx"),
                guava.headMap("xxxx", true).entries.map { it.key to it.value },
                guava.tailMap("xxxx", false).entries.map { it.key to it.value },
                guava.subMap("xx", true, "xxxxxx", false).entries.map { it.key to it.value },
                guava.descendingMap().entries.map { it.key to it.value },
                guava.descendingMap().descendingMap() === guava,
            ),
            listOf(
                guavaKt.entries.map { it.key to it.value }, guavaKt["cat"], guavaKt.containsKey("fox"),
                guavaKt.firstKey(), guavaKt.lastKey(), guavaKt.lowerKey("xxxx"), guavaKt.floorKey("xxxx"),
                guavaKt.ceilingKey("xxxx"), guavaKt.higherKey("xxxx"),
                guavaKt.headMap("xxxx", true).entries.map { it.key to it.value },
                guavaKt.tailMap("xxxx", false).entries.map { it.key to it.value },
                guavaKt.subMap("xx", true, "xxxxxx", false).entries.map { it.key to it.value },
                guavaKt.descendingMap().entries.map { it.key to it.value },
                guavaKt.descendingMap().descendingMap() === guavaKt,
            ),
        )
    }

    @Test
    fun sortedMapDuplicateCopyNullAndDeepMutationFailuresMatchGuava() {
        val guavaBuilder = GuavaImmutableSortedMap.orderedBy<String, Int>(byLength)
            .put("cat", 1).put("dog", 2)
        val guavaKtBuilder = GuavaKtImmutableSortedMap.orderedBy<String, Int>(byLength)
            .put("cat", 1).put("dog", 2)
        assertEquals(failureName { guavaBuilder.buildOrThrow() }, failureName { guavaKtBuilder.buildOrThrow() })
        assertEquals(failureName { guavaBuilder.buildKeepingLast() }, failureName { guavaKtBuilder.buildKeepingLast() })

        val guava = GuavaImmutableSortedMap.of("a", 1, "b", 2)
        val guavaKt = GuavaKtImmutableSortedMap.of("a", 1, "b", 2)
        val guavaEmpty = GuavaImmutableSortedMap.of<String, Int>()
        val guavaKtEmpty = GuavaKtImmutableSortedMap.of<String, Int>()
        assertEquals(
            listOf(
                GuavaImmutableSortedMap.copyOf(guava) === guava,
                guavaEmpty.headMap("z") === guavaEmpty,
                failureName { guava.put("c", 3) }, failureName { guava.keys.remove("a") },
                failureName { guava.values.remove(1) }, failureName { guava.entries.first().setValue(4) },
                failureName { guava.pollFirstEntry() },
                GuavaImmutableNullHarness.sortedMapNullKeyFailure(),
                GuavaImmutableNullHarness.sortedMapNullValueFailure(),
            ),
            listOf(
                GuavaKtImmutableSortedMap.copyOf(guavaKt) === guavaKt,
                guavaKtEmpty.headMap("z") === guavaKtEmpty,
                failureName { guavaKt.put("c", 3) }, failureName { guavaKt.keys.remove("a") },
                failureName { guavaKt.values.remove(1) }, failureName { guavaKt.entries.first().setValue(4) },
                failureName { guavaKt.pollFirstEntry() },
                failureName {
                    GuavaKtImmutableSortedMap.orderedBy<String?, Int>(Comparator { a, b -> (a ?: "").compareTo(b ?: "") })
                        .put(null, 1).build()
                },
                failureName { GuavaKtImmutableSortedMap.of<String, Int?>("key", null) },
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
