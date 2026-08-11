package dev.guavakt.parity

import com.google.common.base.Equivalence as GuavaEquivalence
import com.google.common.base.Function as GuavaFunction
import dev.guavakt.base.Equivalence as GuavaKtEquivalence
import dev.guavakt.base.Function as GuavaKtFunction
import kotlin.test.Test
import kotlin.test.assertEquals

class EquivalenceDifferentialTest {
    @Test
    fun pairwiseValueAndReferenceSemanticsMatchGuava() {
        val guavaEquals = GuavaEquivalence.equals().pairwise<String>()
        val kotlinEquals = GuavaKtEquivalence.equals<String>().pairwise()
        val guavaIdentity = GuavaEquivalence.identity().pairwise<Token>()
        val kotlinIdentity = GuavaKtEquivalence.identity<Token>().pairwise()

        assertEquals(guavaEquals.equivalent(listOf("a", "b"), listOf("a", "b")), kotlinEquals.equivalent(listOf("a", "b"), listOf("a", "b")))
        assertEquals(guavaEquals.equivalent(listOf("a"), listOf("a", "b")), kotlinEquals.equivalent(listOf("a"), listOf("a", "b")))
        assertEquals(guavaEquals.hash(listOf("a", "b")), kotlinEquals.hash(listOf("a", "b")))
        assertEquals(guavaEquals == GuavaEquivalence.equals().pairwise<String>(), kotlinEquals == GuavaKtEquivalence.equals<String>().pairwise())

        val first = Token(1)
        val equalButDistinct = Token(1)
        assertEquals(
            guavaIdentity.equivalent(listOf(first), listOf(equalButDistinct)),
            kotlinIdentity.equivalent(listOf(first), listOf(equalButDistinct)),
        )
    }

    @Test
    fun functionalAndWrapperSemanticsMatchGuava() {
        val guavaFunction = GuavaFunction<String, Int> { it.length }
        val kotlinFunction = GuavaKtFunction<String, Int> { it.length }
        val guava = GuavaEquivalence.equals().onResultOf(guavaFunction)
        val kotlin = GuavaKtEquivalence.equals<Int>().onResultOf(kotlinFunction)

        assertEquals(guava.equivalent("one", "two"), kotlin.equivalent("one", "two"))
        assertEquals(guava.equivalent("one", "four"), kotlin.equivalent("one", "four"))
        assertEquals(guava.hash("three"), kotlin.hash("three"))
        assertEquals(guava == GuavaEquivalence.equals().onResultOf(guavaFunction), kotlin == GuavaKtEquivalence.equals<Int>().onResultOf(kotlinFunction))

        val token = Token(1)
        assertEquals(
            GuavaEquivalence.equals().wrap(token) == GuavaEquivalence.identity().wrap(token),
            GuavaKtEquivalence.equals<Token>().wrap(token) == GuavaKtEquivalence.identity<Token>().wrap(token),
        )
        assertEquals(GuavaEquivalence.equals().equivalentTo("x").test("x"), GuavaKtEquivalence.equals<String>().equivalentTo("x")("x"))
        assertEquals(GuavaEquivalence.equals().equivalentTo("x").test(null), GuavaKtEquivalence.equals<String>().equivalentTo("x")(null))
    }

    private data class Token(val value: Int)
}
