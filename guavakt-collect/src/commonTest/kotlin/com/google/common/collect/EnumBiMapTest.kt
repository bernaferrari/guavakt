package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNull

class EnumBiMapTest {
    @Test
    fun inverse_andForcePut() {
        val m = EnumBiMap.create<String, Int>()
        m.put("a", 1)
        assertEquals("a", m.inverse()[1])
        assertFailsWith<IllegalArgumentException> { m.put("b", 1) }
        m.forcePut("b", 1)
        assertEquals("b", m.inverse()[1])
        assertTrue(!m.containsKey("a"))
    }

    @Test
    fun abstractCoreHandlesNullableMappingsAndEntryMutations() {
        val map = AbstractBiMap.create<String?, Int?>()
        map.put(null, null)
        map.put("one", 1)
        assertTrue(map.containsKey(null))
        assertTrue(map.containsValue(null))
        assertTrue(map.inverse().containsKey(null))

        val nullEntry = map.entries.first { it.key == null }
        assertNull(nullEntry.setValue(2))
        assertEquals(null, map.inverse()[2])
        assertFailsWith<IllegalArgumentException> { nullEntry.setValue(1) }

        val iterator = map.entries.iterator()
        val removed = iterator.next()
        val removedValue = removed.value
        iterator.remove()
        assertTrue(!map.inverse().containsKey(removedValue))
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            map.entries.add(Maps.immutableEntry("two", 2) as MutableMap.MutableEntry<String?, Int?>)
        }
    }
}
