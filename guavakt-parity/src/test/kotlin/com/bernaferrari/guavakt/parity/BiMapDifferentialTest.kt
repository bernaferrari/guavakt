package com.bernaferrari.guavakt.parity

import com.google.common.collect.HashBiMap as GuavaHashBiMap
import com.google.common.collect.ImmutableBiMap as GuavaImmutableBiMap
import com.bernaferrari.guavakt.collect.HashBiMap as GuavaKtHashBiMap
import com.bernaferrari.guavakt.collect.ImmutableBiMap as GuavaKtImmutableBiMap
import kotlin.test.Test
import kotlin.test.assertEquals

class BiMapDifferentialTest {
    @Test
    fun forcePutInverseEntryMutationAndLiveViewsMatchGuava() {
        assertEquals(guavaMutableTrace(), guavaKtMutableTrace())
    }

    @Test
    fun immutableViewsAndReciprocalInverseMatchGuava() {
        val guava = GuavaImmutableBiMap.builder<String, Int>().put("a", 1).put("b", 2).build()
        val guavaKt = GuavaKtImmutableBiMap.builder<String, Int>().put("a", 1).put("b", 2).build()
        assertEquals(
            listOf(
                guava.entries.map { it.key to it.value },
                guava.values.toList(),
                guava.inverse().entries.map { it.key to it.value },
                guava.inverse().inverse() === guava,
                failureName { guava.values.remove(1) },
                failureName { guava.entries.first().setValue(3) },
            ),
            listOf(
                guavaKt.entries.map { it.key to it.value },
                guavaKt.values.toList(),
                guavaKt.inverse().entries.map { it.key to it.value },
                guavaKt.inverse().inverse() === guavaKt,
                failureName { (guavaKt.values as MutableSet).remove(1) },
                failureName { (guavaKt.entries.first() as MutableMap.MutableEntry).setValue(3) },
            ),
        )
    }

    @Test
    fun immutableNullRejectionMatchesGuava() {
        assertEquals(
            GuavaBiMapNullHarness.nullKeyFailure(),
            failureName { GuavaKtImmutableBiMap.builder<String?, Int>().put(null, 1) },
        )
        assertEquals(
            GuavaBiMapNullHarness.nullValueFailure(),
            failureName { GuavaKtImmutableBiMap.builder<String, Int?>().put("key", null) },
        )
    }

    private fun guavaMutableTrace(): List<Any?> {
        val map = GuavaHashBiMap.create<String, Int>()
        val values = map.values
        val inverse = map.inverse()
        val trace = mutableListOf<Any?>(
            map.put("a", 1),
            map.put("b", 2),
            failureName { map.put("c", 2) },
            map.forcePut("c", 2),
            inverse.put(3, "d"),
            map.entries.first { it.key == "a" }.setValue(4),
            failureName { map.entries.first { it.key == "c" }.setValue(3) },
            values.remove(2),
            inverse.inverse() === map,
        )
        val keys = map.keys.iterator()
        trace.add(keys.next())
        keys.remove()
        trace.addAll(listOf(
            map.entries.map { it.key to it.value },
            inverse.entries.map { it.key to it.value },
            values.toList(),
        ))
        return trace
    }

    private fun guavaKtMutableTrace(): List<Any?> {
        val map = GuavaKtHashBiMap.create<String, Int>()
        val values = map.values
        val inverse = map.inverse()
        val trace = mutableListOf<Any?>(
            map.put("a", 1),
            map.put("b", 2),
            failureName { map.put("c", 2) },
            map.forcePut("c", 2),
            inverse.put(3, "d"),
            map.entries.first { it.key == "a" }.setValue(4),
            failureName { map.entries.first { it.key == "c" }.setValue(3) },
            values.remove(2),
            inverse.inverse() === map,
        )
        val keys = map.keys.iterator()
        trace.add(keys.next())
        keys.remove()
        trace.addAll(listOf(
            map.entries.map { it.key to it.value },
            inverse.entries.map { it.key to it.value },
            values.toList(),
        ))
        return trace
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
