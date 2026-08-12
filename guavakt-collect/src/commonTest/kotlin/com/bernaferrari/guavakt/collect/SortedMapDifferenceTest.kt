package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SortedMapDifferenceTest {
    @Test
    fun comparatorOverloadSortsAllSnapshotMapsAndSeparatesDifferences() {
        val difference = Maps.difference(
            left = linkedMapOf(3 to "same", 1 to "left", 2 to "old"),
            right = linkedMapOf(4 to "right", 2 to "new", 3 to "same"),
            comparator = Comparator { left, right -> left.compareTo(right) },
        )

        assertFalse(difference.areEqual())
        assertEquals(listOf(1), difference.entriesOnlyOnLeft().keys.toList())
        assertEquals(listOf(4), difference.entriesOnlyOnRight().keys.toList())
        assertEquals(listOf(3), difference.entriesInCommon().keys.toList())
        assertEquals(listOf(2), difference.entriesDiffering().keys.toList())
        assertEquals("old", difference.entriesDiffering().getValue(2).leftValue())
        assertEquals("new", difference.entriesDiffering().getValue(2).rightValue())
        assertTrue(difference.entriesOnlyOnLeft() !is MutableMap<*, *>)
    }

    @Test
    fun comparatorEquivalenceUsesTheRightMapValueLikeGuavaTreeMap() {
        val byFinalDigit = Comparator<Int> { left, right -> (left % 10).compareTo(right % 10) }
        val difference = Maps.difference(
            left = mapOf(1 to 1),
            right = linkedMapOf(11 to 2, 21 to 3),
            comparator = byFinalDigit,
        )

        assertEquals(3, difference.entriesDiffering().getValue(1).rightValue())
    }
}
