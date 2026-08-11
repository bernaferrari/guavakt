package dev.guavakt.collect

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ImmutableClassToInstanceMapContractTest {
    @Test
    fun singletonCopyAndTypedLookupPreserveIdentityAndSnapshots() {
        val singleton: ImmutableClassToInstanceMap<Any> =
            ImmutableClassToInstanceMap.of(String::class, "value")
        assertEquals("value", singleton.getInstance(String::class))
        assertNull(singleton.getInstance(Int::class))
        assertSame(singleton, ImmutableClassToInstanceMap.copyOf(singleton))

        val source = linkedMapOf<KClass<out Any>, Any>(String::class to "before")
        val snapshot = ImmutableClassToInstanceMap.copyOf(source)
        source[String::class] = "after"
        assertEquals("before", snapshot.getInstance(String::class))
    }

    @Test
    fun builderPutAllRejectsForgedValuesAndDuplicateClassKeys() {
        val valid = linkedMapOf<KClass<out Any>, Any>(
            String::class to "text",
            Int::class to 7,
        )
        val built = ImmutableClassToInstanceMap.builder<Any>().putAll(valid).build()
        assertEquals("text", built.getInstance(String::class))
        assertEquals(7, built.getInstance(Int::class))

        val duplicate = ImmutableClassToInstanceMap.builder<Any>()
            .put(String::class, "first")
            .put(String::class, "second")
        assertFailsWith<IllegalArgumentException> { duplicate.build() }

        val invalid = linkedMapOf<KClass<out Any>, Any>(String::class to 1)
        assertFailsWith<ClassCastException> {
            ImmutableClassToInstanceMap.builder<Any>().putAll(invalid)
        }

        val partiallyInvalid = linkedMapOf<KClass<out Any>, Any>(
            String::class to "retained",
            Int::class to "wrong",
        )
        val partialBuilder = ImmutableClassToInstanceMap.builder<Any>()
        assertFailsWith<ClassCastException> { partialBuilder.putAll(partiallyInvalid) }
        assertEquals("retained", partialBuilder.build().getInstance(String::class))
    }

    @Test
    fun reusableBuilderProducesIndependentSnapshots() {
        val builder = ImmutableClassToInstanceMap.builder<Any>().put(String::class, "text")
        val first = builder.build()
        builder.put(Int::class, 9)
        val second = builder.build()
        assertNull(first.getInstance(Int::class))
        assertEquals(9, second.getInstance(Int::class))
        assertEquals(listOf(String::class, Int::class), second.keys.toList())
    }

    @Test
    fun directBulkAndNestedMutationsAlwaysFail() {
        val map: ImmutableClassToInstanceMap<Any> =
            ImmutableClassToInstanceMap.builder<Any>()
                .put(String::class, "text")
                .put(Int::class, 7)
                .build()

        assertFailsWith<UnsupportedOperationException> { map.putInstance(String::class, "other") }
        assertFailsWith<UnsupportedOperationException> { map[String::class] = "other" }
        assertFailsWith<UnsupportedOperationException> { map.putAll(emptyMap()) }
        assertFailsWith<UnsupportedOperationException> { map.remove(Boolean::class) }
        assertFailsWith<UnsupportedOperationException> { map.clear() }
        assertFailsWith<UnsupportedOperationException> { map.keys.remove(Boolean::class) }
        assertFailsWith<UnsupportedOperationException> { map.values.remove(false) }
        assertFailsWith<UnsupportedOperationException> { map.entries.clear() }
        assertFailsWith<UnsupportedOperationException> { map.entries.first().setValue("other") }
    }
}
