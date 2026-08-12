package com.bernaferrari.guavakt.parity

import com.google.common.collect.ImmutableList as GuavaImmutableList
import com.google.common.collect.ImmutableMap as GuavaImmutableMap
import com.google.common.collect.ImmutableSet as GuavaImmutableSet
import com.bernaferrari.guavakt.collect.ImmutableList as GuavaKtImmutableList
import com.bernaferrari.guavakt.collect.ImmutableMap as GuavaKtImmutableMap
import com.bernaferrari.guavakt.collect.ImmutableSet as GuavaKtImmutableSet
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableCollectionsDifferentialTest {
    @Test
    fun listOrderingViewsIdentityAndMutationFailuresMatchGuava() {
        val guava = GuavaImmutableList.copyOf(listOf("a", "b", "c"))
        val guavaKt = GuavaKtImmutableList.copyOf(listOf("a", "b", "c"))
        assertEquals(
            listOf(
                guava.toList(), guava.reverse().toList(), guava.reverse().reverse() === guava,
                guava.subList(0, guava.size) === guava, GuavaImmutableList.copyOf(guava) === guava,
                failureName { guava.add("d") }, failureName { guava.removeAt(0) },
                failureName { guava.listIterator().apply { next(); set("x") } },
            ),
            listOf(
                guavaKt.toList(), guavaKt.reverse().toList(), guavaKt.reverse().reverse() === guavaKt,
                guavaKt.subList(0, guavaKt.size) === guavaKt, GuavaKtImmutableList.copyOf(guavaKt) === guavaKt,
                failureName { guavaKt.add("d") }, failureName { guavaKt.removeAt(0) },
                failureName { guavaKt.listIterator().apply { next(); set("x") } },
            ),
        )
    }

    @Test
    fun setDeduplicationOrderAndMutationFailuresMatchGuava() {
        val guava = GuavaImmutableSet.copyOf(listOf("b", "a", "b", "c"))
        val guavaKt = GuavaKtImmutableSet.copyOf(listOf("b", "a", "b", "c"))
        assertEquals(
            listOf(guava.toList(), guava.asList(), failureName { guava.add("d") }, failureName { guava.remove("a") }),
            listOf(guavaKt.toList(), guavaKt.asList(), failureName { guavaKt.add("d") }, failureName { guavaKt.remove("a") }),
        )
    }

    @Test
    fun mapDuplicateBuildersCopiesAndDeepMutationFailuresMatchGuava() {
        val guavaBuilder = GuavaImmutableMap.builder<String, Int>().put("a", 1).put("b", 2).put("a", 3)
        val guavaKtBuilder = GuavaKtImmutableMap.builder<String, Int>().put("a", 1).put("b", 2).put("a", 3)
        assertEquals(failureName { guavaBuilder.buildOrThrow() }, failureName { guavaKtBuilder.buildOrThrow() })
        assertEquals(guavaBuilder.buildKeepingLast().entries.map { it.key to it.value },
            guavaKtBuilder.buildKeepingLast().entries.map { it.key to it.value })

        val guava = GuavaImmutableMap.of("a", 1, "b", 2)
        val guavaKt = GuavaKtImmutableMap.of("a", 1, "b", 2)
        assertEquals(
            listOf(
                GuavaImmutableMap.copyOf(guava) === guava,
                failureName { guava.put("c", 3) }, failureName { guava.keys.remove("a") },
                failureName { guava.values.remove(1) }, failureName { guava.entries.first().setValue(4) },
                failureName { GuavaImmutableMap.of("a", 1, "a", 2) },
            ),
            listOf(
                GuavaKtImmutableMap.copyOf(guavaKt) === guavaKt,
                failureName { guavaKt.put("c", 3) }, failureName { guavaKt.keys.remove("a") },
                failureName { guavaKt.values.remove(1) }, failureName { guavaKt.entries.first().setValue(4) },
                failureName { GuavaKtImmutableMap.of("a", 1, "a", 2) },
            ),
        )
    }

    @Test
    fun nullRejectionMatchesGuava() {
        assertEquals(GuavaImmutableNullHarness.listNullFailure(), failureName { GuavaKtImmutableList.of<String?>(null) })
        assertEquals(GuavaImmutableNullHarness.setNullFailure(), failureName { GuavaKtImmutableSet.of<String?>(null) })
        assertEquals(GuavaImmutableNullHarness.mapNullKeyFailure(), failureName { GuavaKtImmutableMap.of<String?, Int>(null, 1) })
        assertEquals(GuavaImmutableNullHarness.mapNullValueFailure(), failureName { GuavaKtImmutableMap.of<String, Int?>("key", null) })
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
