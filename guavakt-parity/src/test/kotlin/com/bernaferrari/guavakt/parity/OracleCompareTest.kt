package com.bernaferrari.guavakt.parity

import com.google.common.base.Joiner as GuavaJoiner
import com.google.common.base.Optional as GuavaOptional
import com.google.common.base.Preconditions as GuavaPreconditions
import com.google.common.base.Splitter as GuavaSplitter
import com.google.common.collect.ArrayListMultimap as GuavaArrayListMultimap
import com.google.common.collect.HashBiMap as GuavaHashBiMap
import com.google.common.collect.ImmutableList as GuavaImmutableList
import com.google.common.collect.ImmutableMap as GuavaImmutableMap
import com.google.common.collect.ImmutableSet as GuavaImmutableSet
import com.google.common.math.IntMath as GuavaIntMath
import com.google.common.primitives.Ints as GuavaInts
import com.google.common.primitives.Longs as GuavaLongs
import com.bernaferrari.guavakt.base.Joiner
import com.bernaferrari.guavakt.base.Optional
import com.bernaferrari.guavakt.base.Preconditions
import com.bernaferrari.guavakt.base.Splitter
import com.bernaferrari.guavakt.collect.ArrayListMultimap
import com.bernaferrari.guavakt.collect.HashBiMap
import com.bernaferrari.guavakt.collect.ImmutableList
import com.bernaferrari.guavakt.collect.ImmutableMap
import com.bernaferrari.guavakt.collect.ImmutableSet
import com.bernaferrari.guavakt.math.IntMath
import com.bernaferrari.guavakt.primitives.Ints
import com.bernaferrari.guavakt.primitives.Longs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Typed Guava differential checks. Every parity test executes both libraries. */
class OracleCompareTest {
    @Test fun joinerAndSplitterMatchGuava() {
        val inputs = listOf(emptyList(), listOf("only"), listOf("a", "b", "c"))
        for (input in inputs) {
            assertEquals(GuavaJoiner.on(',').join(input), Joiner.on(',').join(input))
        }
        val source = " a, ,b,,c "
        val expected = GuavaSplitter.on(',').trimResults().omitEmptyStrings().splitToList(source)
        val actual = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(source)
        assertEquals(expected, actual)
    }

    @Test fun primitiveComparisonsAndCheckedMathMatchGuava() {
        for ((left, right) in listOf(-1 to 1, 0 to 0, Int.MAX_VALUE to Int.MIN_VALUE)) {
            assertEquals(GuavaInts.compare(left, right), Ints.compare(left, right))
        }
        for ((left, right) in listOf(-1L to 1L, 0L to 0L, Long.MAX_VALUE to Long.MIN_VALUE)) {
            assertEquals(GuavaLongs.compare(left, right), Longs.compare(left, right))
        }
        assertEquals(GuavaIntMath.checkedAdd(100, 23), IntMath.checkedAdd(100, 23))
        assertEquals(
            runCatching { GuavaIntMath.checkedAdd(Int.MAX_VALUE, 1) }.exceptionOrNull()?.javaClass,
            runCatching { IntMath.checkedAdd(Int.MAX_VALUE, 1) }.exceptionOrNull()?.javaClass,
        )
    }

    @Test fun optionalStateAndFallbackMatchGuava() {
        assertEquals(GuavaOptional.of(3).isPresent, Optional.of(3).isPresent())
        assertEquals(GuavaOptional.absent<Int>().isPresent, Optional.absent<Int>().isPresent())
        assertEquals(GuavaOptional.absent<Int>().or(7), Optional.absent<Int>().or(7))
    }

    @Test fun preconditionFailureTypeAndMessageMatchGuava() {
        val guava = assertFailsWith<IndexOutOfBoundsException> {
            GuavaPreconditions.checkElementIndex(5, 3)
        }
        val ours = assertFailsWith<IndexOutOfBoundsException> {
            Preconditions.checkElementIndex(5, 3)
        }
        assertEquals(guava::class, ours::class)
        assertEquals(guava.message, ours.message)
    }

    @Test fun immutableFactoriesMatchGuavaValuesAndRejectMutation() {
        assertEquals(GuavaImmutableList.of(1, 2, 3), ImmutableList.of(1, 2, 3).toList())
        assertEquals(GuavaImmutableSet.of(1, 2), ImmutableSet.of(1, 2).toSet())
        assertEquals(GuavaImmutableMap.of("a", 1, "b", 2), ImmutableMap.of("a", 1, "b", 2).toMap())

        val mutationFailure = runCatching {
            @Suppress("UNCHECKED_CAST")
            (ImmutableList.of(1, 2) as MutableList<Int>).add(3)
        }.exceptionOrNull()
        assertTrue(
            mutationFailure is ClassCastException || mutationFailure is UnsupportedOperationException,
            "Immutable list must be read-only or reject mutation",
        )
    }

    @Test fun multimapLiveViewsMatchGuava() {
        val guava = GuavaArrayListMultimap.create<String, Int>()
        val ours = ArrayListMultimap.create<String, Int>()
        val guavaView = guava["k"]
        val ourView = ours.get("k")

        guavaView.addAll(listOf(1, 2, 3))
        ourView.addAll(listOf(1, 2, 3))
        guavaView.subList(1, 3).clear()
        ourView.subList(1, 3).clear()

        assertEquals(guava.size(), ours.size())
        assertEquals(guava.asMap().mapValues { it.value.toList() }, ours.asMap().mapValues { it.value.toList() })
    }

    @Test fun biMapInverseMutationsMatchGuava() {
        val guava = GuavaHashBiMap.create<String, Int>()
        val ours = HashBiMap.create<String, Int>()
        guava["a"] = 1
        guava["b"] = 2
        ours["a"] = 1
        ours["b"] = 2

        guava.inverse()[2] = "bee"
        ours.inverse()[2] = "bee"
        guava.values.remove(1)
        ours.values.remove(1)

        assertEquals(guava, ours.toMap())
        assertEquals(guava.inverse(), ours.inverse().toMap())
        assertTrue(!ours.containsKey("a"))
    }
}
