package com.bernaferrari.guavakt.parity

import com.google.common.collect.ImmutableMultiset as GuavaImmutableMultiset
import com.google.common.collect.LinkedHashMultiset as GuavaLinkedHashMultiset
import com.bernaferrari.guavakt.collect.ImmutableMultiset as GuavaKtImmutableMultiset
import com.bernaferrari.guavakt.collect.LinkedHashMultiset as GuavaKtLinkedHashMultiset
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableMultisetDifferentialTest {
    @Test
    fun factoriesOrderingCountsStringAndIdentityMatchGuava() {
        val guava = GuavaImmutableMultiset.of("b", "a", "b", "c", "a", "b")
        val guavaKt = GuavaKtImmutableMultiset.of("b", "a", "b", "c", "a", "b")
        assertEquals(
            listOf(
                guava.elementSet().toList(), guava.asList().toList(),
                guava.entrySet().map { it.element to it.count }, guava.toString(),
                GuavaImmutableMultiset.copyOf(guava) === guava,
                guava.asList() === guava.asList(),
            ),
            listOf(
                guavaKt.elementSet().toList(), guavaKt.asList().toList(),
                guavaKt.entrySet().map { it.getElement() to it.getCount() }, guavaKt.toString(),
                GuavaKtImmutableMultiset.copyOf(guavaKt) === guavaKt,
                guavaKt.asList() === guavaKt.asList(),
            ),
        )
    }

    @Test
    fun multisetCopiesAndReusableBuilderCountsMatchGuava() {
        val guavaSource = GuavaLinkedHashMultiset.create<String>().apply {
            add("first", 4); add("second", 2)
        }
        val guavaKtSource = GuavaKtLinkedHashMultiset.create<String>().apply {
            add("first", 4); add("second", 2)
        }
        val guavaCopy = GuavaImmutableMultiset.copyOf(guavaSource)
        val guavaKtCopy = GuavaKtImmutableMultiset.copyOf(guavaKtSource)
        guavaSource.setCount("first", 1)
        guavaKtSource.setCount("first", 1)

        val guavaBuilder = GuavaImmutableMultiset.builder<String>()
            .add("a", "b", "a").addCopies("b", 2).setCount("a", 4)
        val guavaKtBuilder = GuavaKtImmutableMultiset.builder<String>()
            .add("a", "b", "a").addCopies("b", 2).setCount("a", 4)
        val guavaFirst = guavaBuilder.build()
        val guavaKtFirst = guavaKtBuilder.build()
        guavaBuilder.setCount("a", 0).add("d")
        guavaKtBuilder.setCount("a", 0).add("d")

        assertEquals(
            listOf(entries(guavaCopy), entries(guavaFirst), entries(guavaBuilder.build())),
            listOf(entriesKt(guavaKtCopy), entriesKt(guavaKtFirst), entriesKt(guavaKtBuilder.build())),
        )
    }

    @Test
    fun nullAndCountValidationMatchGuava() {
        assertEquals(
            listOf(
                GuavaImmutableNullHarness.multisetNullFactoryFailure(),
                GuavaImmutableNullHarness.multisetNullZeroCopiesFailure(),
                failureName { GuavaImmutableMultiset.builder<String>().addCopies("x", -1) },
                failureName { GuavaImmutableMultiset.builder<String>().setCount("x", -1) },
            ),
            listOf(
                failureName { GuavaKtImmutableMultiset.of<String?>(null) },
                failureName { GuavaKtImmutableMultiset.builder<String?>().addCopies(null, 0) },
                failureName { GuavaKtImmutableMultiset.builder<String>().addCopies("x", -1) },
                failureName { GuavaKtImmutableMultiset.builder<String>().setCount("x", -1) },
            ),
        )
    }

    @Test
    fun directAndNestedMutationFailuresMatchGuava() {
        val guava = GuavaImmutableMultiset.of("a", "a", "b")
        val guavaKt = GuavaKtImmutableMultiset.of("a", "a", "b")
        assertEquals(
            listOf(
                failureName { guava.add("x", 0) },
                failureName { guava.remove("missing", 0) },
                failureName { guava.setCount("missing", 7, 0) },
                failureName { guava.addAll(emptyList()) },
                failureName { guava.removeAll(emptyList()) },
                failureName { guava.elementSet().remove("missing") },
                failureName { guava.entrySet().clear() },
                failureName { guava.asList().remove("missing") },
            ),
            listOf(
                failureName { guavaKt.add("x", 0) },
                failureName { guavaKt.remove("missing", 0) },
                failureName { guavaKt.setCount("missing", 7, 0) },
                failureName { guavaKt.addAll(emptyList()) },
                failureName { guavaKt.removeAll(emptyList()) },
                failureName { (guavaKt.elementSet() as MutableSet<String>).remove("missing") },
                failureName { (guavaKt.entrySet() as MutableSet<*>).clear() },
                failureName { (guavaKt.asList() as MutableList<String>).remove("missing") },
            ),
        )
    }

    private fun <E : Any> entries(multiset: GuavaImmutableMultiset<E>): List<Pair<E, Int>> =
        multiset.entrySet().map { it.element to it.count }

    private fun <E> entriesKt(multiset: GuavaKtImmutableMultiset<E>): List<Pair<E, Int>> =
        multiset.entrySet().map { it.getElement() to it.getCount() }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
