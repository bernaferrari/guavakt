package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HashBiMapTest {
    @Test
    fun put_and_inverse() {
        val b = HashBiMap.create<String, Int>()
        assertNull(b.put("a", 1))
        assertEquals(1, b["a"])
        assertEquals("a", b.inverse()[1])
        b.forcePut("b", 1)
        assertEquals("b", b.inverse()[1])
        assertNull(b["a"])
    }

    @Test
    fun duplicateEntryAndViewMutationsKeepBothDirectionsConsistent() {
        val b: BiMap<String, Int> = HashBiMap.create<String, Int>().apply {
            put("a", 1)
            put("b", 2)
        }
        assertTrue(b.values is Set<Int>)
        assertFailsWith<IllegalArgumentException> { (b as MutableMap<String, Int>)["c"] = 2 }

        val mutable = b as HashBiMap<String, Int>
        mutable.entries.first { it.key == "a" }.setValue(3)
        assertEquals("a", mutable.inverse()[3])
        assertFailsWith<IllegalArgumentException> {
            mutable.entries.first { it.key == "a" }.setValue(2)
        }
        mutable.values.remove(2)
        assertNull(mutable["b"])
        assertSame(mutable, mutable.inverse().inverse())
    }

    @Test
    fun nullableMappingsAreDistinguishedFromAbsentMappings() {
        val b = HashBiMap.create<String?, Int?>()
        b[null] = null
        b["one"] = 1
        assertTrue(b.containsKey(null))
        assertTrue(b.containsValue(null))
        assertNull(b.inverse()[null])
        assertTrue(b.inverse().containsKey(null))
        assertNull(b.forcePut("replacement", null))
        assertTrue(!b.containsKey(null))
        assertEquals("replacement", b.inverse()[null])
    }

    @Test
    fun immutableBiMapRejectsNullAndDeepMutation() {
        assertFailsWith<NullPointerException> {
            ImmutableBiMap.builder<String?, Int>().put(null, 1)
        }
        val map = ImmutableBiMap.of("a", 1)
        assertSame(map, map.inverse().inverse())
        assertFailsWith<UnsupportedOperationException> {
            (map.values as MutableSet<Int>).remove(1)
        }
        assertFailsWith<UnsupportedOperationException> {
            (map.entries.first() as MutableMap.MutableEntry<String, Int>).setValue(2)
        }
    }
}
