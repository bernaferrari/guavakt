package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkedListMultimapLiveViewsTest {
    @Test fun retainedViewsReflectLaterMutationsAndCanRemove() {
        val multimap = LinkedListMultimap.create<String, Int>()
        val keys = multimap.keySet()
        val keyOccurrences = multimap.keys()
        val asMap = multimap.asMap()

        multimap.put("a", 1)
        multimap.put("a", 2)
        multimap.put("b", 3)
        assertEquals(setOf("a", "b"), keys)
        assertEquals(2, keyOccurrences.count("a"))
        assertEquals(listOf(1, 2), asMap["a"])

        (keys as MutableSet).remove("a")
        assertFalse(multimap.containsKey("a"))
        assertNull(asMap["a"])
        assertEquals(1, multimap.size())

        assertEquals(listOf(3), (asMap as MutableMap).remove("b"))
        assertTrue(multimap.isEmpty())
    }
}
