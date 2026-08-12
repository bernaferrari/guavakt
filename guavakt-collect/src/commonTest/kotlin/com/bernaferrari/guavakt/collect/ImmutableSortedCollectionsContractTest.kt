package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImmutableSortedCollectionsContractTest {
    private val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }

    @Test
    fun sortedSetUsesComparatorIdentityAndNavigatesBoundaries() {
        val set = ImmutableSortedSet.orderedBy(byLength)
            .add("quick", "a", "in", "the", "over", "jumped", "fox").build()
        assertEquals(listOf("a", "in", "the", "over", "quick", "jumped"), set.toList())
        assertSame(set.asList(), set.asList())
        assertTrue("cat" in set)
        assertEquals("over", set.floor("xxxx"))
        assertEquals("over", set.ceiling("xxxx"))
        assertEquals(listOf("in", "the", "over"), set.subSet("xx", true, "xxxxx", false).toList())
        assertSame(set, set.descendingSet().descendingSet())
    }

    @Test
    fun sortedSetSnapshotsRejectNullsAndEveryMutationRoute() {
        val source = mutableListOf(3, 1, 2)
        val set = ImmutableSortedSet.copyOf(source)
        source.clear()
        assertEquals(listOf(1, 2, 3), set.toList())
        assertSame(set, ImmutableSortedSet.copyOf(set))
        assertFailsWith<UnsupportedOperationException> { set.add(4) }
        assertFailsWith<UnsupportedOperationException> { set.iterator().apply { next(); remove() } }
        assertFailsWith<UnsupportedOperationException> { set.pollFirst() }
        assertFailsWith<NullPointerException> {
            ImmutableSortedSet.orderedBy<String?>(Comparator { a, b -> (a ?: "").compareTo(b ?: "") })
                .add(null).build()
        }
        assertFailsWith<IllegalArgumentException> { set.subSet(3, 1) }
    }

    @Test
    fun sortedMapUsesComparatorIdentityAndProvidesImmutableRanges() {
        val map = ImmutableSortedMap.orderedBy<String, Int>(byLength)
            .put("a", 1).put("the", 3).put("quick", 5).put("jumped", 6).build()
        assertEquals(listOf("a", "the", "quick", "jumped"), map.keys.toList())
        assertEquals(3, map["cat"])
        assertEquals("the", map.floorKey("xxxx"))
        assertEquals("quick", map.ceilingKey("xxxx"))
        assertEquals(listOf("the", "quick"), map.subMap("xx", true, "xxxxxx", false).keys.toList())
        assertSame(map, map.descendingMap().descendingMap())
    }

    @Test
    fun sortedMapRejectsComparatorDuplicatesNullsAndDeepMutation() {
        val builder = ImmutableSortedMap.orderedBy<String, Int>(byLength).put("cat", 1).put("dog", 2)
        assertFailsWith<IllegalArgumentException> { builder.buildOrThrow() }
        assertFailsWith<UnsupportedOperationException> { builder.buildKeepingLast() }
        val map = ImmutableSortedMap.of("a", 1, "b", 2)
        assertSame(map, ImmutableSortedMap.copyOf(map))
        assertFailsWith<UnsupportedOperationException> { map["c"] = 3 }
        assertFailsWith<UnsupportedOperationException> { map.keys.remove("a") }
        assertFailsWith<UnsupportedOperationException> { map.values.remove(1) }
        assertFailsWith<UnsupportedOperationException> { map.entries.first().setValue(4) }
        assertFailsWith<UnsupportedOperationException> { map.pollFirstEntry() }
        assertFailsWith<NullPointerException> {
            ImmutableSortedMap.orderedBy<String?, Int>(Comparator { a, b -> (a ?: "").compareTo(b ?: "") })
                .put(null, 1).build()
        }
        assertFailsWith<NullPointerException> { ImmutableSortedMap.of<String, Int?>("a", null) }
    }
}
