package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThrowablesCharsetsTest {
    @Test
    fun throwables_rootCause_and_chain() {
        val root = IllegalStateException("root")
        val mid = RuntimeException("mid", root)
        val top = IllegalArgumentException("top", mid)
        assertEquals(root, Throwables.getRootCause(top))
        val chain = Throwables.getCausalChain(top)
        assertEquals(3, chain.size)
        assertEquals(top, chain[0])
        assertTrue(Throwables.getStackTraceAsString(top).contains("top"))
    }

    @Test
    fun charsets_utf8_name() {
        assertEquals("UTF-8", Charsets.UTF_8)
        assertTrue(Charsets.isUtf8Name("utf-8"))
        assertFalse(Charsets.isUtf8Name("ISO-8859-1"))
    }

    @Test
    fun utf8_wellFormed_ascii() {
        assertTrue(Utf8.isWellFormed("hello".encodeToByteArray()))
        assertEquals(5, Utf8.encodedLength("hello"))
    }

    @Test
    fun defaults_primitives() {
        assertEquals(0, Defaults.defaultValueForInt())
        assertEquals(false, Defaults.defaultValueForBoolean())
        assertEquals(null, Defaults.defaultValue("java.lang.String"))
    }
}
