package dev.guavakt.io

import okio.Buffer
import okio.Sink
import okio.Timeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharSourceStreamingTest {
    @Test
    fun legacyPathCharSourcesRejectUnsupportedCharsetsBeforeOpeningAPlatformFile() {
        Files.asCharSource("unused", "UTF8")
        assertFailsWith<IllegalArgumentException> {
            Files.asCharSource("unused", "UTF-16LE")
        }
    }

    @Test
    fun lineReadersHonorCrLfCrAndLfBoundaries() {
        val source = CharSource.wrap("first\rsecond\r\nthird\nfourth")
        assertEquals("first", source.readFirstLine())
        assertEquals(listOf("first", "second", "third", "fourth"), source.readLines())
    }

    @Test
    fun concatenationAndEmptyProbeOpenReadersOnlyAsNeeded() {
        val events = mutableListOf<String>()
        fun source(name: String, value: String): CharSource = CharSource.fromReader {
            events += "open:$name"
            TrackingReader(value) { events += "close:$name" }
        }
        val concatenated = CharSource.concat(source("first", "ab"), source("last", "c"))

        assertTrue(events.isEmpty())
        val reader = concatenated.openReader()
        assertTrue(events.isEmpty())
        assertEquals('a'.code, reader.read())
        assertEquals(listOf("open:first"), events)
        reader.close()
        assertEquals(listOf("open:first", "close:first"), events)

        lateinit var opened: TrackingReader
        val nonEmpty = CharSource.fromReader { TrackingReader("content").also { opened = it } }
        assertFalse(nonEmpty.isEmpty())
        assertEquals(1, opened.charactersRead)
        assertTrue(opened.closed)
    }

    @Test
    fun iterableConcat_snapshotsTheSourceList() {
        val sources = mutableListOf(CharSource.wrap("a"))
        val concatenated = CharSource.concat(sources)
        sources += CharSource.wrap("b")

        assertEquals("a", concatenated.read())
        assertEquals(1L, concatenated.lengthIfKnown())
    }

    @Test
    fun writeFromStreamsAcrossSurrogatePairChunkBoundaries() {
        val value = "a".repeat(4_095) + "😀" + "z"
        val buffer = Buffer()
        val sink = object : CharSink() {
            override fun openBufferedSink() = buffer
        }

        assertEquals(value.length.toLong(), sink.writeFrom(CharSource.wrap(value)))
        assertEquals(value, buffer.readUtf8())
    }

    @Test
    fun knownLengthsAvoidOpeningReaders_andComposeForConcatenation() {
        var opened = false
        val empty = object : CharSource() {
            override fun openStream(): CharReaderLike {
                opened = true
                error("known length should avoid opening")
            }

            override fun lengthIfKnown(): Long = 0L
        }
        val combined = CharSource.concat(CharSource.wrap("ab"), CharSource.wrap("😀"))

        assertTrue(empty.isEmpty())
        assertEquals(0L, empty.length())
        assertFalse(opened)
        assertEquals(4L, combined.lengthIfKnown())
        assertEquals(4L, combined.length())
    }

    @Test
    fun lineProcessorAndCopyOperationsStreamAndCloseTheReader() {
        lateinit var reader: TrackingReader
        val source = CharSource.fromReader { TrackingReader("one\ntwo\nthree").also { reader = it } }
        val processor = object : LineProcessor<List<String>> {
            private val seen = ArrayList<String>()
            override fun processLine(line: String): Boolean {
                seen += line
                return line != "two"
            }

            override fun getResult(): List<String> = seen
        }

        assertEquals(listOf("one", "two"), source.readLines(processor))
        assertTrue(reader.closed)

        val appended = StringBuilder()
        assertEquals(13L, source.copyTo(appended))
        assertEquals("one\ntwo\nthree", appended.toString())

        val buffer = Buffer()
        val sink = object : CharSink() {
            override fun openBufferedSink() = buffer
        }
        assertEquals(13L, source.copyTo(sink))
        assertEquals("one\ntwo\nthree", buffer.readUtf8())
    }

    private class TrackingReader(
        private val value: String,
        private val onClose: () -> Unit = {},
    ) : CharReaderLike("") {
        private var position = 0
        var charactersRead = 0
            private set
        var closed = false
            private set

        override fun read(buffer: CharArray): Int {
            if (position == value.length) return -1
            val count = minOf(buffer.size, value.length - position)
            value.toCharArray(position, position + count).copyInto(buffer, 0)
            position += count
            charactersRead += count
            return count
        }

        override fun read(): Int {
            if (position == value.length) return -1
            charactersRead++
            return value[position++].code
        }

        override fun close() {
            if (closed) return
            closed = true
            onClose()
        }
    }
}
