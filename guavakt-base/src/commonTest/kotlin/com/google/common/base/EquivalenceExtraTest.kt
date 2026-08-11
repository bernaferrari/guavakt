package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquivalenceExtraTest {
    @Test
    fun wrap_and_equivalentTo() {
        val eq = Equivalence.equals<String>()
        val w1 = eq.wrap("a")
        val w2 = eq.wrap("a")
        assertEquals(w1, w2)
        assertTrue(eq.equivalentTo("x")("x"))
    }

    @Test
    fun pairwiseUsesGuavaSeedMultiplierAndDerivedEquality() {
        val equalsPairwise = Equivalence.equals<String>().pairwise()
        val samePairwise = Equivalence.equals<String>().pairwise()
        val identityPairwise = Equivalence.identity<String>().pairwise()

        assertEquals(78_721, equalsPairwise.hash(emptyList()))
        assertEquals(78_721 * 24_943 + "a".hashCode(), equalsPairwise.hash(listOf("a")))
        assertEquals(equalsPairwise, samePairwise)
        assertEquals(equalsPairwise.hashCode(), samePairwise.hashCode())
        assertFalse(equalsPairwise == identityPairwise)
        assertEquals("${Equivalence.equals<String>()}.pairwise()", equalsPairwise.toString())
    }

    @Test
    fun functionalAndWrapperEquivalencesHonorTheirStrategies() {
        val length = Function<String, Int> { it.length }
        val byLength = Equivalence.equals<Int>().onResultOf(length)
        val sameByLength = Equivalence.equals<Int>().onResultOf(length)
        assertTrue(byLength.equivalent("one", "two"))
        assertEquals(3, byLength.hash("one"))
        assertEquals(byLength, sameByLength)
        assertEquals(byLength.hashCode(), sameByLength.hashCode())

        val token = Token(1)
        assertFalse(Equivalence.equals<Token>().wrap(token) == Equivalence.identity<Token>().wrap(token))
    }

    private data class Token(val value: Int)
}
