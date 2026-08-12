package com.bernaferrari.guavakt.io

import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ByteSourceStreamingTest {
    @Test
    fun iterableConcat_snapshotsTheSourceList() {
        val sources = mutableListOf(ByteSource.wrap(byteArrayOf(1)))
        val concatenated = ByteSource.concat(sources)
        sources += ByteSource.wrap(byteArrayOf(2))

        assertContentEquals(byteArrayOf(1), concatenated.read())
        assertEquals(1L, concatenated.sizeIfKnown())
    }

    @Test
    fun sliceIsLazyAndReadsOnlyThroughItsRequestedEnd() {
        val opened = mutableListOf<TrackingSource>()
        val source = ByteSource.fromSource {
            TrackingSource(ByteArray(10) { it.toByte() }, maxChunk = 2).also(opened::add)
        }

        val slice = source.slice(offset = 2, length = 3)
        assertTrue(opened.isEmpty())
        assertContentEquals(byteArrayOf(2, 3, 4), slice.read())
        assertEquals(1, opened.size)
        assertEquals(5, opened.single().bytesRead)
        assertTrue(opened.single().closed)
    }

    @Test
    fun concatenationOpensEachSourceOnlyWhenReached() {
        val events = mutableListOf<String>()
        fun source(name: String, bytes: ByteArray): ByteSource = ByteSource.fromSource(bytes.size.toLong()) {
            events += "open:$name"
            TrackingSource(bytes, maxChunk = 1, onClose = { events += "close:$name" })
        }
        val concatenated = ByteSource.concat(
            source("first", byteArrayOf(1, 2)),
            source("empty", byteArrayOf()),
            source("last", byteArrayOf(3)),
        )

        assertEquals(3L, concatenated.sizeIfKnown())
        assertTrue(events.isEmpty())
        val opened = concatenated.openSource()
        assertTrue(events.isEmpty())
        val oneByte = Buffer()
        assertEquals(1L, opened.read(oneByte, 1L))
        assertEquals(listOf("open:first"), events)
        assertEquals(1, oneByte.readByte().toInt())
        opened.close()
        assertEquals(listOf("open:first", "close:first"), events)
    }

    @Test
    fun emptyCheckDoesNotCountTheWholeUnknownSource() {
        lateinit var opened: TrackingSource
        val source = ByteSource.fromSource {
            TrackingSource(ByteArray(100) { 7 }, maxChunk = 1).also { opened = it }
        }

        assertFalse(source.isEmpty())
        assertEquals(1, opened.bytesRead)
        assertTrue(opened.closed)
    }

    @Test
    fun copyingToCallerOwnedOkioSinkStreamsAndLeavesSinkOpen() {
        lateinit var opened: TrackingSource
        val source = ByteSource.fromSource {
            TrackingSource(ByteArray(20_000) { (it * 17).toByte() }, maxChunk = 257).also { opened = it }
        }
        val sink = RecordingSink()

        assertEquals(20_000L, source.copyTo(sink))
        assertContentEquals(ByteArray(20_000) { (it * 17).toByte() }, sink.buffer.readByteArray())
        assertFalse(sink.closed)
        assertTrue(opened.closed)
        assertTrue(opened.readCalls > 2)
    }

    @Test
    fun knownSizeAndLegacySubclassesRemainSupported() {
        val known = ByteSource.fromSource(sizeIfKnown = 4L) { error("known size must not open source") }
        assertEquals(4L, known.size())
        assertFalse(known.isEmpty())

        val legacy = object : ByteSource() {
            override fun openStream(): ByteArrayInputLike = ByteArrayInputLike(byteArrayOf(9, 8, 7))
        }
        assertContentEquals(byteArrayOf(9, 8, 7), legacy.read())
        assertContentEquals(byteArrayOf(8), legacy.slice(1, 1).read())
    }

    @Test
    fun byteProcessorStreamsChunksStopsEarlyAndClosesTheSource() {
        lateinit var opened: TrackingSource
        val source = ByteSource.fromSource {
            TrackingSource(ByteArray(20_000) { (it * 13).toByte() }, maxChunk = 257).also { opened = it }
        }
        val processor = object : ByteProcessor<Int> {
            var total = 0
            override fun processBytes(buffer: ByteArray, offset: Int, length: Int): Boolean {
                total += length
                return total < 600
            }
            override fun getResult(): Int = total
        }

        assertEquals(771, source.read(processor))
        assertEquals(771, opened.bytesRead)
        assertEquals(3, opened.readCalls)
        assertTrue(opened.closed)
    }

    @Test
    fun contentEqualsIsStreamingAndIndependentOfUpstreamChunking() {
        lateinit var left: TrackingSource
        lateinit var same: TrackingSource
        lateinit var different: TrackingSource
        val bytes = ByteArray(20_000) { (it * 13).toByte() }
        val first = ByteSource.fromSource { TrackingSource(bytes, maxChunk = 1).also { left = it } }
        val equal = ByteSource.fromSource { TrackingSource(bytes, maxChunk = 509).also { same = it } }
        val unequal = ByteSource.fromSource {
            TrackingSource(bytes.copyOf().also { it[it.lastIndex] = 0 }, maxChunk = 71).also { different = it }
        }

        assertTrue(first.contentEquals(equal))
        assertTrue(left.closed)
        assertTrue(same.closed)
        assertFalse(first.contentEquals(unequal))
        assertTrue(different.closed)
    }

    private class TrackingSource(
        private val bytes: ByteArray,
        private val maxChunk: Int,
        private val onClose: () -> Unit = {},
    ) : Source {
        private var position = 0
        var bytesRead: Int = 0
            private set
        var readCalls: Int = 0
            private set
        var closed: Boolean = false
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            check(!closed)
            readCalls++
            if (position == bytes.size) return -1L
            val count = min(min(byteCount, maxChunk.toLong()).toInt(), bytes.size - position)
            sink.write(bytes, position, count)
            position += count
            bytesRead += count
            return count.toLong()
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() {
            if (closed) return
            closed = true
            onClose()
        }
    }

    private class RecordingSink : Sink {
        val buffer = Buffer()
        var closed = false
            private set

        override fun write(source: Buffer, byteCount: Long) = buffer.write(source, byteCount)
        override fun flush() = Unit
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() { closed = true }
    }
}
