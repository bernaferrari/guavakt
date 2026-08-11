package dev.guavakt.reflect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReflectPortTest {
    @Test
    fun typeToken_of_and_subtype() {
        val stringToken = TypeToken.of(String::class)
        val otherString = TypeToken.of(String::class)
        val intToken = TypeToken.of(Int::class)
        assertEquals(String::class, stringToken.getRawType())
        assertTrue(stringToken.isSubtypeOf(otherString))
        assertFalse(stringToken.isSubtypeOf(intToken))
        assertEquals("String", stringToken.toString())
    }
}
