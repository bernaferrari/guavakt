package com.bernaferrari.guavakt.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CharSequenceReaderTest {
    @Test
    fun readMarkResetSkipAndReadyFollowReaderContract() {
        val reader = CharSequenceReader("abcd")
        val buffer = CharArray(4) { '_' }
        assertEquals(0, reader.read(buffer, 1, 0))
        assertTrue(reader.ready())
        assertTrue(reader.markSupported())
        reader.mark(0)
        assertEquals('a'.code, reader.read())
        assertEquals(2, reader.read(buffer, 1, 2))
        assertEquals("_bc_", buffer.concatToString())
        reader.reset()
        assertEquals(3, reader.skip(3))
        assertEquals('d'.code, reader.read())
        assertEquals(-1, reader.read())
        assertEquals(0, reader.skip(1))
    }

    @Test
    fun invalidArgumentsAndClosedUseFailExplicitly() {
        val reader = CharSequenceReader("a")
        assertFailsWith<IndexOutOfBoundsException> { reader.read(CharArray(1), 1, 1) }
        assertFailsWith<IllegalArgumentException> { reader.skip(-1) }
        assertFailsWith<IllegalArgumentException> { reader.mark(-1) }
        reader.close()
        assertFailsWith<IllegalStateException> { reader.read() }
        assertFailsWith<IllegalStateException> { reader.ready() }
        assertFailsWith<IllegalStateException> { reader.reset() }
    }
}
