package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewIntegrityTest {
    @Test fun bimapViewMutationsKeepInverseConsistent() {
        val map = HashBiMap.create<String, Int>()
        map["a"] = 1
        map["b"] = 2

        map.keys.remove("a")
        assertFalse(map.inverse().containsKey(1))

        val entry = map.entries.single()
        entry.setValue(3)
        assertFalse(map.inverse().containsKey(2))
        assertEquals("b", map.inverse()[3])

        map.values.remove(3)
        assertTrue(map.isEmpty())
        assertTrue(map.inverse().isEmpty())
    }

    @Test fun listMultimapSubListMutationsMaintainTotalSizeAndKeyLifecycle() {
        val multimap = ArrayListMultimap.create<String, Int>()
        multimap.putAll("k", listOf(1, 2, 3, 4))
        val sub = multimap.get("k").subList(1, 3)
        sub.clear()
        assertEquals(listOf(1, 4), multimap.get("k"))
        assertEquals(2, multimap.size())

        multimap.get("k").subList(0, 2).clear()
        assertFalse(multimap.containsKey("k"))
        assertEquals(0, multimap.size())
    }

    @Test fun valuesAndEntriesIteratorsCanRemoveLastValueForAKey() {
        val multimap = ArrayListMultimap.create<String, Int>()
        multimap.put("a", 1)
        multimap.put("b", 2)
        val values = (multimap.values() as MutableCollection).iterator()
        values.next()
        values.remove()
        assertEquals(1, multimap.size())

        val entries = (multimap.entries() as MutableCollection).iterator()
        entries.next()
        entries.remove()
        assertTrue(multimap.isEmpty())
    }
}
