package dev.guavakt.collect

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MutableClassToInstanceMapTest {
    @Test
    fun typedOperationsReturnTypedPreviousAndCurrentValues() {
        val map: ClassToInstanceMap<Any> = MutableClassToInstanceMap.create()
        val mutable = map as MutableClassToInstanceMap<Any>

        assertNull(mutable.putInstance(String::class, "first"))
        assertEquals("first", mutable.putInstance(String::class, "second"))
        assertNull(mutable.putInstance(Int::class, 7))
        assertEquals("second", map.getInstance(String::class))
        assertEquals(7, map.getInstance(Int::class))
    }

    @Test
    fun normalPutAndEntrySetValueEnforceTheirKeyClass() {
        val map = MutableClassToInstanceMap.create<Any>()
        map[String::class] = "valid"

        assertFailsWith<ClassCastException> { map[String::class] = 42 }
        val entry = map.entries.single()
        assertEquals("valid", entry.setValue("updated"))
        assertEquals("updated", map.getInstance(String::class))
        assertFailsWith<ClassCastException> { entry.setValue(42) }
        assertEquals("updated", map.getInstance(String::class))
    }

    @Test
    fun putAllValidatesEveryMappingBeforeChangingAnything() {
        val map = MutableClassToInstanceMap.create<Any>()
        map.putInstance(Boolean::class, true)
        val invalid = linkedMapOf<KClass<out Any>, Any>(
            String::class to "would-be-valid",
            Int::class to "wrong",
        )

        assertFailsWith<ClassCastException> { map.putAll(invalid) }
        assertEquals(mapOf<KClass<out Any>, Any>(Boolean::class to true), map.toMap())
    }

    @Test
    fun callerOwnedBackingMapRemainsLiveAndRemovalViewsWork() {
        val backing = linkedMapOf<KClass<out Any>, Any>()
        val map = MutableClassToInstanceMap.create(backing)

        map.putInstance(String::class, "value")
        assertEquals("value", backing[String::class])
        assertTrue(map.keys.remove(String::class))
        assertFalse(backing.containsKey(String::class))

        map.putInstance(Int::class, 1)
        val iterator = map.entries.iterator()
        iterator.next()
        iterator.remove()
        assertTrue(map.isEmpty())
    }

    @Test
    fun createAndImmutableCopyRejectPreexistingForgedMappings() {
        val invalid = linkedMapOf<KClass<out Any>, Any>(String::class to 1)

        assertFailsWith<ClassCastException> { MutableClassToInstanceMap.create(invalid) }
        assertFailsWith<ClassCastException> { ImmutableClassToInstanceMap.copyOf(invalid) }
    }
}
