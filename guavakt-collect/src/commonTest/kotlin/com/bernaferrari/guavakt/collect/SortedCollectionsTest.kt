package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals

class SortedCollectionsTest {
    @Test
    fun newTreeMap_iterationOrder() {
        val m = Maps.newTreeMap<String, Int>()
        m["c"] = 3
        m["a"] = 1
        m["b"] = 2
        assertEquals(listOf("a", "b", "c"), m.keys.toList())
    }

    @Test
    fun newTreeSet_iterationOrder() {
        val s = Sets.newTreeSet<Int>()
        s.addAll(listOf(3, 1, 2))
        assertEquals(listOf(1, 2, 3), s.toList())
    }

    @Test
    fun treeMultimap_keyAndValueOrder() {
        val mm = TreeMultimap.create<String, Int>()
        mm.put("b", 2)
        mm.put("a", 9)
        mm.put("a", 1)
        assertEquals(listOf("a", "b"), mm.keySet().toList())
        assertEquals(listOf(1, 9), mm.get("a").toList())
    }

    @Test
    fun treeMultiset_elementOrder() {
        val ms = TreeMultiset.create(listOf("c", "a", "b", "a"))
        assertEquals(listOf("a", "a", "b", "c"), ms.toList())
    }
}
