package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ImmutableCollectionsContractTest {
    @Test
    fun listSnapshotsRejectMutationAndRetainReciprocalReverseIdentity() {
        val source = mutableListOf("a", "b", "c")
        val list = ImmutableList.copyOf(source)
        source.clear()
        assertEquals(listOf("a", "b", "c"), list)
        assertSame(list, list.reverse().reverse())
        assertSame(list, list.subList(0, list.size))
        assertFailsWith<UnsupportedOperationException> { list.add("d") }
        assertFailsWith<UnsupportedOperationException> { list.listIterator().apply { next(); set("x") } }
        assertFailsWith<NullPointerException> { ImmutableList.of<String?>(null) }
    }

    @Test
    fun setSnapshotsDeduplicateInOrderAndRejectMutation() {
        val set = ImmutableSet.copyOf(listOf("b", "a", "b", "c"))
        assertEquals(listOf("b", "a", "c"), set.toList())
        assertFailsWith<UnsupportedOperationException> { set.remove("a") }
        assertFailsWith<UnsupportedOperationException> { set.iterator().remove() }
        assertFailsWith<NullPointerException> { ImmutableSet.of<String?>(null) }
    }

    @Test
    fun mapBuildersDistinguishStrictAndKeepingLastAndCloseDeepMutation() {
        val builder = ImmutableMap.builder<String, Int>().put("a", 1).put("b", 2).put("a", 3)
        assertFailsWith<IllegalArgumentException> { builder.buildOrThrow() }
        assertEquals(mapOf("a" to 3, "b" to 2), builder.buildKeepingLast())

        val map = ImmutableMap.of("a", 1, "b", 2)
        assertFailsWith<UnsupportedOperationException> { map["c"] = 3 }
        assertFailsWith<UnsupportedOperationException> { map.keys.remove("a") }
        assertFailsWith<UnsupportedOperationException> { map.values.remove(1) }
        assertFailsWith<UnsupportedOperationException> { map.entries.first().setValue(4) }
        assertFailsWith<NullPointerException> { ImmutableMap.of<String?, Int>(null, 1) }
        assertFailsWith<NullPointerException> { ImmutableMap.of<String, Int?>("a", null) }
    }
}
