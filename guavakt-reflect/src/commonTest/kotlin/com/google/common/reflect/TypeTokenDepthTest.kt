package dev.guavakt.reflect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeTokenDepthTest {
    open class Animal
    class Dog : Animal()

    @Test fun of_reified_and_kclass() {
        assertEquals(String::class, TypeToken.of<String>().getRawType())
        assertEquals(Int::class, TypeToken.of(Int::class).getRawType())
    }

    @Test fun getTypes_includesSelf() {
        val types = TypeToken.of(Dog::class).getTypes()
        assertTrue(types.any { it.getRawType() == Dog::class })
    }

    @Test fun immutableTypeToInstanceMap() {
        val map = ImmutableTypeToInstanceMap.builder<Any>()
            .put(String::class, "x")
            .build()
        assertEquals("x", map.getInstance(String::class))
        assertEquals(1, map.size)
        assertFalse(map.isEmpty())
    }

    @Test fun equals_hashCode() {
        assertEquals(TypeToken.of(String::class), TypeToken.of(String::class))
        assertEquals(TypeToken.of(String::class).hashCode(), TypeToken.of(String::class).hashCode())
    }
}
