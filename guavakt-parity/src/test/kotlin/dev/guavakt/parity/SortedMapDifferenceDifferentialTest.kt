package dev.guavakt.parity

import com.google.common.collect.Maps as GuavaMaps
import dev.guavakt.collect.Maps
import java.util.TreeMap
import kotlin.test.Test
import kotlin.test.assertEquals

class SortedMapDifferenceDifferentialTest {
    @Test
    fun orderedDifferencesMatchGuavaSortedMapSnapshots() {
        val guavaLeft = TreeMap<Int, String>().apply {
            put(3, "same")
            put(1, "left")
            put(2, "old")
        }
        val guavaRight = linkedMapOf(4 to "right", 2 to "new", 3 to "same")
        val guava = GuavaMaps.difference(guavaLeft, guavaRight)
        val kotlin = Maps.difference(
            left = linkedMapOf(3 to "same", 1 to "left", 2 to "old"),
            right = guavaRight,
            comparator = Comparator.naturalOrder<Int>(),
        )

        assertEquals(guava.areEqual(), kotlin.areEqual())
        assertEquals(guava.entriesOnlyOnLeft().keys.toList(), kotlin.entriesOnlyOnLeft().keys.toList())
        assertEquals(guava.entriesOnlyOnRight().keys.toList(), kotlin.entriesOnlyOnRight().keys.toList())
        assertEquals(guava.entriesInCommon().keys.toList(), kotlin.entriesInCommon().keys.toList())
        assertEquals(guava.entriesDiffering().keys.toList(), kotlin.entriesDiffering().keys.toList())
        assertEquals(
            guava.entriesDiffering()[2]?.leftValue(),
            kotlin.entriesDiffering()[2]?.leftValue(),
        )
        assertEquals(
            guava.entriesDiffering()[2]?.rightValue(),
            kotlin.entriesDiffering()[2]?.rightValue(),
        )
    }
}
