package dev.guavakt.reflect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MutableTypeToInstanceMapTest {
    @Test
    fun tokenAndKClassOperationsShareTypedMappings() {
        val map: TypeToInstanceMap<Any> = MutableTypeToInstanceMap()
        val mutable = map as MutableTypeToInstanceMap<Any>
        val stringToken = TypeToken.of(String::class)

        assertNull(mutable.putInstance(stringToken, "first"))
        assertEquals("first", mutable.putInstance(String::class, "second"))
        assertEquals("second", map.getInstance(stringToken))
        assertEquals("second", map.getInstance(String::class))
    }

    @Suppress("DEPRECATION")
    @Test
    fun directPutPutAllAndEntrySetValueAlwaysThrow() {
        val map = MutableTypeToInstanceMap<Any>()
        val token = TypeToken.of(String::class)
        map.putInstance(token, "value")

        assertFailsWith<UnsupportedOperationException> { map.put(token, "replacement") }
        assertFailsWith<UnsupportedOperationException> { map.putAll(emptyMap()) }
        assertFailsWith<UnsupportedOperationException> { map.entries.first().setValue("replacement") }
        assertEquals("value", map.getInstance(token))
    }

    @Test
    fun iteratorAndKeyRemovalRemainSupported() {
        val map = MutableTypeToInstanceMap<Any>()
        map.putInstance(String::class, "value")
        map.putInstance(Int::class, 2)

        assertTrue(map.keys.remove(TypeToken.of(String::class)))
        val iterator = map.entries.iterator()
        iterator.next()
        iterator.remove()
        assertTrue(map.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun forgedTokensAreRuntimeCheckedByMutableAndImmutableMaps() {
        val stringAsAny = TypeToken.of(String::class) as TypeToken<Any>
        val mutable = MutableTypeToInstanceMap<Any>()
        val forged = mapOf<TypeToken<out Any>, Any>(stringAsAny to 4)

        assertFailsWith<ClassCastException> { mutable.putInstance(stringAsAny, 4) }
        assertFailsWith<ClassCastException> { ImmutableTypeToInstanceMap.copyOf(forged) }
    }
}
