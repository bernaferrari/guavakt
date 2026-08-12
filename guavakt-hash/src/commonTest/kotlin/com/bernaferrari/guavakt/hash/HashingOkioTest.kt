package com.bernaferrari.guavakt.hash

import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HashingOkioTest {
    @Test
    fun sourceHashesOnlyReturnedBytesAndClosesItsUpstream() {
        val bytes = ByteArray(2_000) { (it * 37).toByte() }
        val upstream = TrackingSource(bytes, maxChunk = 257)
        val source = upstream.hashing(Hashing.sha512())
        val destination = Buffer().write(byteArrayOf(99))

        repeat(4) { assertEquals(257L, source.read(destination, 1_024L)) }
        assertEquals(99, destination.readByte().toInt())
        assertEquals(bytes.copyOf(1_028).toList(), destination.readByteArray().toList())
        assertEquals(Hashing.sha512().hashBytes(bytes, 0, 1_028), source.hash())
        source.close()
        assertTrue(upstream.closed)
    }

    @Test
    fun sinkHashesWrittenBytesClosesItsDownstreamAndPreservesThePayload() {
        val bytes = ByteArray(2_000) { (it * 37).toByte() }
        val downstream = TrackingSink()
        val sink = downstream.hashing(Hashing.sha256())

        sink.write(Buffer().write(bytes, 0, 700), 700L)
        sink.write(Buffer().write(bytes, 700, 1_300), 1_300L)
        assertEquals(Hashing.sha256().hashBytes(bytes), sink.hash())
        sink.close()

        assertEquals(bytes.toList(), downstream.bytes.readByteArray().toList())
        assertTrue(downstream.closed)
    }

    @Test
    fun sinkAccountsForBytesBeforeADownstreamWriteFailure() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val sink = HashingSink(FailingSink(), Hashing.sha256())

        assertFailsWith<IllegalStateException> {
            sink.write(Buffer().write(bytes), bytes.size.toLong())
        }
        assertEquals(Hashing.sha256().hashBytes(bytes), sink.hash())
    }

    private class TrackingSource(
        private val bytes: ByteArray,
        private val maxChunk: Int,
    ) : Source {
        private var position = 0
        var closed = false
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            check(!closed)
            if (position == bytes.size) return -1L
            val count = min(min(byteCount, maxChunk.toLong()).toInt(), bytes.size - position)
            sink.write(bytes, position, count)
            position += count
            return count.toLong()
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() { closed = true }
    }

    private class TrackingSink : Sink {
        val bytes = Buffer()
        var closed = false
            private set

        override fun write(source: Buffer, byteCount: Long) = bytes.write(source, byteCount)
        override fun flush() = Unit
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() { closed = true }
    }

    private class FailingSink : Sink {
        override fun write(source: Buffer, byteCount: Long): Unit =
            throw IllegalStateException("downstream failed")
        override fun flush() = Unit
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() = Unit
    }
}
