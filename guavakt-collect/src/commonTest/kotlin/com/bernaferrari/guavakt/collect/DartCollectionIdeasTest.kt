package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DartCollectionIdeasTest {
    @Test
    fun canonicalizedMap_normalizesLookupAndRetainsLatestKey() {
        val map = CanonicalizedMap<String, String, Int> { it.lowercase() }

        assertNull(map.put("Content-Type", 1))
        assertEquals(1, map["content-type"])
        assertEquals(1, map.put("CONTENT-TYPE", 2))
        assertEquals(1, map.size)
        assertEquals("CONTENT-TYPE", map.keys.single())
        assertTrue(map.keys.remove("content-Type"))
        assertTrue(map.isEmpty())
    }

    @Test
    fun equalityCollections_useProvidedEqualityAndHandleHashCollisions() {
        val caseInsensitive = object : Equality<String> {
            override fun equivalent(left: String, right: String): Boolean = left.equals(right, ignoreCase = true)
            override fun hash(value: String): Int = 0
        }
        val map = EqualityMap<String, Int>(caseInsensitive)
        val set = EqualitySet(caseInsensitive)

        map["one"] = 1
        map["ONE"] = 2
        set += "one"
        assertFalse(set.add("ONE"))

        assertEquals(1, map.size)
        assertEquals(2, map["oNe"])
        assertTrue(map.keys.remove("ONE"))
        assertTrue(map.isEmpty())
        assertEquals(setOf("one"), set.toSet())
    }

    @Test
    fun equalityStrategies_coverAsciiAndDeepCollections() {
        assertTrue(CaseInsensitiveAsciiEquality.equivalent("CONTENT-TYPE", "content-type"))
        assertFalse(CaseInsensitiveAsciiEquality.equivalent("Å", "å"))
        assertTrue(CaseInsensitiveAsciiEquality.equivalent("Å", "Å"))

        val equality = DeepCollectionEquality()
        assertTrue(equality.equivalent(mapOf("tags" to setOf(listOf(1, 2))), mapOf("tags" to setOf(listOf(1, 2)))))
        assertFalse(equality.equivalent(listOf(1, 2), listOf(2, 1)))
    }

    @Test
    fun nAryZip_stopsAtTheShortestInput() {
        val zipped = Streams.zip(listOf(listOf(1, 2), listOf(3, 4), listOf(5))).toList()

        assertEquals(listOf(listOf(1, 3, 5)), zipped)
    }

    @Test
    fun predicateSplits_areLazyAndKeepAllElements() {
        assertEquals(
            listOf(listOf(1, 2), listOf(3, 4), listOf(5)),
            Iterables.splitAfter(listOf(1, 2, 3, 4, 5)) { it % 2 == 0 }.toList(),
        )
        assertEquals(
            listOf(listOf(1, 2), listOf(3, 4), listOf(5)),
            Iterables.splitBefore(listOf(1, 2, 3, 4, 5)) { it % 2 != 0 }.toList(),
        )
        assertEquals(
            listOf(listOf(1, 2), listOf(3, 4), listOf(5)),
            Iterables.splitBetween(listOf(1, 2, 3, 4, 5)) { left, right -> left % 2 == 0 && right % 2 != 0 }.toList(),
        )
    }

    @Test
    fun queueList_supportsBothListAndDequeOperations() {
        val queue = QueueList(listOf(2, 3))
        queue.addFirst(1)
        queue.addLast(4)
        queue[1] = 20

        assertEquals(listOf(1, 20, 3, 4), queue)
        assertEquals(1, queue.removeFirstOrNull())
        assertEquals(4, queue.removeLastOrNull())
        assertEquals(listOf(20, 3), queue)
    }

    @Test
    fun identityHashSet_keepsEqualButDistinctReferences() {
        val first = Box("same")
        val second = Box("same")
        val set = Sets.newIdentityHashSet<Box>()

        assertTrue(set.add(first))
        assertTrue(set.add(second))
        assertEquals(2, set.size)
        assertTrue(set.contains(first))
        assertTrue(set.contains(second))
    }

    private data class Box(val value: String)
}
