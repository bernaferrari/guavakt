package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImmutableContractTest {
    @Test fun immutableList_isReadOnlyView() {
        val list = ImmutableList.of(1, 2, 3)
        assertEquals(listOf(1, 2, 3), list)
        assertEquals(listOf(2, 3), list.subList(1, 3))
        assertEquals(listOf(3, 2, 1), list.reverse())
    }

    @Test fun immutableMap_copyOf_snapshot() {
        val mutable = linkedMapOf("a" to 1, "b" to 2)
        val imm = ImmutableMap.copyOf(mutable)
        mutable["c"] = 3
        assertFalse(imm.containsKey("c"))
        assertEquals(2, imm.size)
    }

    @Test fun immutableSet_dedupes() {
        assertEquals(setOf(1, 2), ImmutableSet.copyOf(listOf(1, 1, 2)))
    }

    @Test fun immutableSortedMap_ordersKeys() {
        val m = ImmutableSortedMap.copyOf(mapOf("c" to 3, "a" to 1, "b" to 2))
        assertEquals(listOf("a", "b", "c"), m.keys.toList())
        assertEquals(1, m["a"])
    }

    @Test fun immutableSortedMap_builder() {
        val m = ImmutableSortedMap.naturalOrder<String, Int>().put("z", 1).put("a", 2).build()
        assertEquals(listOf("a", "z"), m.keys.toList())
    }

    @Test fun immutableEnumSet_copyOf() {
        val s = ImmutableEnumSet.copyOf(listOf("x", "y", "x"))
        assertEquals(2, s.size)
        assertTrue("x" in s)
    }

    @Test fun immutableAsList_copyOf() {
        assertEquals(listOf(1, 2), ImmutableAsList.copyOf(listOf(1, 2)))
    }

    @Test fun immutableSortedAsList_sorts() {
        assertEquals(listOf(1, 2, 3), ImmutableSortedAsList.copyOf(listOf(3, 1, 2)))
    }

    @Test fun immutableMapEntrySet_fromMap() {
        val entries = ImmutableMapEntrySet.copyOf(mapOf("a" to 1, "b" to 2))
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.key == "a" && it.value == 1 })
    }

    @Test fun immutableMultiset_rejectsMutation() {
        val m = ImmutableMultiset.copyOf(listOf("a", "a", "b"))
        assertEquals(2, m.count("a"))
        assertFailsWith<UnsupportedOperationException> { m.add("c") }
        assertFailsWith<UnsupportedOperationException> { m.clear() }
        assertFailsWith<UnsupportedOperationException> { m.remove("a", 1) }
    }

    @Test fun immutableMultiset_builder() {
        val m = ImmutableMultiset.builder<String>().add("a").addCopies("b", 3).build()
        assertEquals(3, m.count("b"))
        assertFailsWith<UnsupportedOperationException> { m.add("z") }
    }

    @Test fun immutableClassToInstanceMap_getInstance() {
        val m = ImmutableClassToInstanceMap.builder<Any>()
            .put(String::class, "hi")
            .put(Int::class, 7)
            .build()
        assertEquals("hi", m.getInstance(String::class))
        assertEquals(7, m.getInstance(Int::class))
    }

    @Test fun immutableEnumMap_copyOf() {
        val m = ImmutableEnumMap.copyOf(mapOf("k" to 1))
        assertEquals(1, m["k"])
    }

    @Test fun identityHashMap_usesReferenceEquality() {
        data class Box(val n: Int)
        val a = Box(1)
        val b = Box(1)
        val m = Maps.newIdentityHashMap<Box, String>()
        m[a] = "a"
        m[b] = "b"
        assertEquals(2, m.size)
        assertEquals("a", m[a])
        assertEquals("b", m[b])
        assertEquals(null, m[Box(1)])
    }

    @Test fun mapDifference_basic() {
        val d = Maps.difference(mapOf("a" to 1, "b" to 2), mapOf("b" to 3, "c" to 4))
        assertFalse(d.areEqual())
        assertEquals(mapOf("a" to 1), d.entriesOnlyOnLeft())
        assertEquals(mapOf("c" to 4), d.entriesOnlyOnRight())
        assertTrue(d.entriesDiffering().containsKey("b"))
    }

    @Test fun uniqueIndex_and_asMap() {
        val idx = Maps.uniqueIndex(listOf("aa", "b")) { it.length }
        assertEquals("aa", idx[2])
        val am = Maps.asMap(setOf(1, 2)) { it * 10 }
        assertEquals(20, am[2])
    }

    @Test fun filterKeys_snapshot() {
        val f = Maps.filterKeys(mapOf(1 to "a", 2 to "b")) { it % 2 == 0 }
        assertEquals(mapOf(2 to "b"), f)
    }
}
