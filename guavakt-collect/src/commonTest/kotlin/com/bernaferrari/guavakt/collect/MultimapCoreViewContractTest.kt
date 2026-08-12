package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultimapCoreViewContractTest {
    @Test
    fun absentListReadsAndIteratorCreationDoNotMaterializeAKey() {
        val multimap = ArrayListMultimap.create<String, Int>()
        val values = multimap.get("a")

        values.listIterator()
        runCatching { values[0] }
        values.subList(0, 0)
        assertFalse(multimap.containsKey("a"))

        val iterator = values.listIterator()
        iterator.add(1)
        iterator.add(2)
        assertEquals(listOf(1, 2), values)
        assertEquals(2, multimap.size())

        values.clear()
        assertFalse(multimap.containsKey("a"))
    }

    @Test
    fun setEntriesAreLiveValueEntriesWithRemovalWriteThrough() {
        val multimap = HashMultimap.create<String, Int>()
        multimap.put("a", 1)
        val entries = multimap.entries()

        multimap.put("b", 2)
        assertEquals(setOf(entry("a", 1), entry("b", 2)), entries)
        assertTrue((entries as MutableSet).remove(entry("a", 1)))
        assertFalse(multimap.containsEntry("a", 1))

        val iterator = entries.iterator() as MutableIterator<Map.Entry<String, Int>>
        iterator.next()
        iterator.remove()
        assertTrue(multimap.isEmpty())

        multimap.put("c", 3)
        assertEquals(setOf(entry("c", 3)), entries)
    }

    @Test
    fun linkedListMultimapsUseMappingEqualityAndMutableEntryValues() {
        val first = LinkedListMultimap.create<String, Int>().apply {
            put("a", 1)
            put("b", 2)
            put("a", 3)
        }
        val second = LinkedListMultimap.create(first)

        assertEquals(second, first)
        assertEquals(second.hashCode(), first.hashCode())
        assertEquals("{a=[1, 3], b=[2]}", first.toString())
        assertTrue(first.entries().contains(entry("a", 1)))

        val mutableEntry = first.entries().first() as MutableMap.MutableEntry<String, Int>
        assertEquals(1, mutableEntry.setValue(10))
        assertEquals(listOf(10, 3), first.get("a"))
    }

    @Test
    fun nullableKeysCanBeRemovedThroughCoreViews() {
        val array = ArrayListMultimap.create<String?, Int>()
        array.put(null, 1)
        array.put(null, 2)
        val keys = array.keys().iterator() as MutableIterator<String?>
        assertEquals(null, keys.next())
        keys.remove()
        assertEquals(1, array.keys().count(null))
        assertEquals(listOf(2), array.removeAll(null))
        assertTrue(array.isEmpty())

        val linked = LinkedListMultimap.create<String?, Int>()
        linked.put(null, 1)
        assertTrue(linked.containsEntry(null, 1))
        assertTrue(linked.remove(null, 1))
        linked.put(null, 2)
        assertEquals(listOf(2), linked.removeAll(null))
    }

    @Test
    fun keysMultisetEntriesHaveCountBasedValueSemantics() {
        val multimap = ArrayListMultimap.create<String, Int>().apply {
            put("a", 1)
            put("a", 2)
            put("b", 3)
        }
        val peer = ArrayListMultimap.create(multimap)

        assertTrue(multimap.keys().entrySet().contains(Multisets.immutableEntry("a", 2)))
        assertEquals(peer.keys(), multimap.keys())
        assertEquals(peer.keys().hashCode(), multimap.keys().hashCode())
        assertEquals("[a x 2, b]", multimap.keys().toString())
    }

    private fun entry(key: String, value: Int): Map.Entry<String, Int> =
        object : Map.Entry<String, Int> {
            override val key: String = key
            override val value: Int = value
            override fun equals(other: Any?): Boolean =
                other is Map.Entry<*, *> && key == other.key && value == other.value
            override fun hashCode(): Int = key.hashCode() xor value.hashCode()
            override fun toString(): String = "$key=$value"
        }
}
