package dev.guavakt.parity

import com.google.common.io.GuavaCharSequenceReaderHarness
import dev.guavakt.io.CharSequenceReader
import kotlin.test.Test
import kotlin.test.assertEquals

class CharSequenceReaderDifferentialTest {
    @Test
    fun stateTraceMatchesGuavaExceptForKotlinClosedExceptionType() {
        val reader = CharSequenceReader("abcd")
        val result = mutableListOf<String>()
        val buffer = CharArray(4) { '_' }
        result += "zero=${reader.read(buffer, 1, 0)}"
        result += "ready=${reader.ready()}"
        result += "marks=${reader.markSupported()}"
        reader.mark(0)
        result += "first=${reader.read()}"
        result += "chunk=${reader.read(buffer, 1, 2)}:${buffer.concatToString()}"
        reader.reset()
        result += "skip=${reader.skip(3)}"
        result += "last=${reader.read()}"
        result += "eof=${reader.read()}"
        result += "skip-eof=${reader.skip(1)}"
        reader.close()
        result += "closed=IOException"

        assertEquals(GuavaCharSequenceReaderHarness.trace("abcd"), result)
    }
}
