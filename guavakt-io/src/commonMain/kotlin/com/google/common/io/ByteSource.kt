package dev.guavakt.io

import okio.Buffer
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import kotlin.math.min

/**
 * A reusable source of bytes.
 *
 * Guava's `ByteSource` is shaped around `InputStream`. GuavaKt uses Okio [Source] for common
 * Kotlin. [openStream] remains the abstract compatibility hook used by early GuavaKt subclasses;
 * primary operations go through [openSource], whose default adapts that hook without reading the
 * complete source. New source-backed implementations should normally use [fromSource].
 */
abstract class ByteSource {
    /** Compatibility hook for the pre-Okio GuavaKt API. */
    abstract fun openStream(): ByteArrayInputLike

    /** Opens a new independently closeable source. */
    open fun openSource(): Source = InputLikeSource(openStream())

    open fun openBufferedSource(): BufferedSource = openSource().buffer()

    open fun sizeIfKnown(): Long? = null

    open fun size(): Long {
        sizeIfKnown()?.let { return it }
        val source = openSource()
        return try {
            val discarded = Buffer()
            var total = 0L
            while (true) {
                val read = source.read(discarded, SEGMENT_SIZE)
                if (read < 0L) return total
                total += read
                discarded.clear()
            }
            @Suppress("UNREACHABLE_CODE")
            total
        } finally {
            source.close()
        }
    }

    fun isEmpty(): Boolean {
        sizeIfKnown()?.let { return it == 0L }
        val source = openBufferedSource()
        return try {
            source.exhausted()
        } finally {
            source.close()
        }
    }

    fun read(): ByteArray {
        val source = openBufferedSource()
        return try {
            source.readByteArray()
        } finally {
            source.close()
        }
    }

    /**
     * Streams this source through [processor], honoring its early-stop signal.
     *
     * The source is still closed when processing stops early. Only the current 8 KiB chunk is
     * materialized, so this is appropriate for large sources unlike [read].
     */
    open fun <T> read(processor: ByteProcessor<T>): T {
        val source = openSource()
        return try {
            val buffer = Buffer()
            while (true) {
                val read = source.read(buffer, SEGMENT_SIZE)
                if (read < 0L) return processor.getResult()
                val chunk = buffer.readByteArray(read)
                if (!processor.processBytes(chunk, 0, chunk.size)) return processor.getResult()
            }
            @Suppress("UNREACHABLE_CODE")
            processor.getResult()
        } finally {
            source.close()
        }
    }

    /**
     * Compares bytes without materializing either complete source.
     *
     * Each source is opened and closed independently. The comparison accepts arbitrary upstream
     * chunk boundaries, which matters for Okio filesystem, network, and test sources.
     */
    fun contentEquals(other: ByteSource): Boolean {
        val left = openBufferedSource()
        val right = other.openBufferedSource()
        return try {
            while (true) {
                val leftExhausted = left.exhausted()
                val rightExhausted = right.exhausted()
                if (leftExhausted || rightExhausted) return leftExhausted && rightExhausted

                val count = minOf(left.buffer.size, right.buffer.size, SEGMENT_SIZE)
                if (!left.readByteArray(count).contentEquals(right.readByteArray(count))) return false
            }
            @Suppress("UNREACHABLE_CODE")
            false
        } finally {
            try {
                left.close()
            } finally {
                right.close()
            }
        }
    }

    /** Copies this source to [sink], closing the source but leaving the caller-owned sink open. */
    fun copyTo(sink: Sink): Long {
        val source = openSource()
        return try {
            val buffer = Buffer()
            var total = 0L
            while (true) {
                val read = source.read(buffer, SEGMENT_SIZE)
                if (read < 0L) return total
                sink.write(buffer, read)
                total += read
            }
            @Suppress("UNREACHABLE_CODE")
            total
        } finally {
            source.close()
        }
    }

    /** Copies this source to a reusable [ByteSink], closing both opened resources. */
    fun copyTo(sink: ByteSink): Long {
        val source = openSource()
        return try {
            sink.writeFrom(source)
        } finally {
            source.close()
        }
    }

    /** Compatibility overload for the pre-Okio GuavaKt sink API. */
    fun copyTo(sink: ByteSinkLike): Long {
        if (sink is ByteSink) return copyTo(sink)
        val input = openStream()
        try {
            val output = sink.openStream()
            try {
                var total = 0L
                val buffer = ByteArray(SEGMENT_SIZE.toInt())
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) return total
                    output.write(buffer, 0, read)
                    total += read
                }
                @Suppress("UNREACHABLE_CODE")
                return total
            } finally {
                output.close()
            }
        } finally {
            input.close()
        }
    }

    /** Returns a lazy bounded view; creating the slice does not read this source. */
    fun slice(offset: Long, length: Long): ByteSource {
        require(offset >= 0L) { "offset ($offset) may not be negative" }
        require(length >= 0L) { "length ($length) may not be negative" }
        val upstream = this
        return fromSource(
            sizeIfKnown = upstream.sizeIfKnown()?.let { known -> min(length, (known - min(offset, known))) },
        ) {
            SlicedSource(upstream.openSource(), offset, length)
        }
    }

    companion object {
        private const val SEGMENT_SIZE = 8_192L

        fun wrap(bytes: ByteArray): ByteSource = object : ByteSource() {
            override fun openStream(): ByteArrayInputLike = ByteArrayInputLike(bytes)
            override fun openSource(): Source = Buffer().write(bytes)
            override fun size(): Long = bytes.size.toLong()
            override fun sizeIfKnown(): Long = bytes.size.toLong()
            override fun <T> read(processor: ByteProcessor<T>): T {
                processor.processBytes(bytes, 0, bytes.size)
                return processor.getResult()
            }
        }

        /** Builds a reusable source from an opener that must return a fresh source on every call. */
        fun fromSource(sizeIfKnown: Long? = null, sourceFactory: () -> Source): ByteSource {
            require(sizeIfKnown == null || sizeIfKnown >= 0L) { "sizeIfKnown may not be negative" }
            return object : ByteSource() {
                override fun openSource(): Source = sourceFactory()
                override fun openStream(): ByteArrayInputLike {
                    val source = openBufferedSource()
                    return try {
                        ByteArrayInputLike(source.readByteArray())
                    } finally {
                        source.close()
                    }
                }
                override fun sizeIfKnown(): Long? = sizeIfKnown
            }
        }

        /** Concatenates a snapshot of [sources], opening components lazily as they are read. */
        fun concat(vararg sources: ByteSource): ByteSource = concat(sources.asList())

        /** Concatenates an iterable snapshot, avoiding a Kotlin vararg spread at call sites. */
        fun concat(sources: Iterable<ByteSource>): ByteSource {
            val snapshot = sources.toList()
            val knownSize = snapshot.fold<ByteSource, Long?>(0L) { total, source ->
                val next = source.sizeIfKnown()
                if (total == null || next == null) null else saturatedAdd(total, next)
            }
            return fromSource(knownSize) { ConcatenatedSource(snapshot) }
        }

        fun empty(): ByteSource = wrap(ByteArray(0))

        private fun saturatedAdd(left: Long, right: Long): Long =
            if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
    }

    private class InputLikeSource(private val input: ByteArrayInputLike) : Source {
        private val scratch = ByteArray(SEGMENT_SIZE.toInt())
        override fun read(sink: Buffer, byteCount: Long): Long {
            require(byteCount >= 0L) { "byteCount ($byteCount) may not be negative" }
            if (byteCount == 0L) return 0L
            val read = input.read(scratch, 0, min(byteCount, scratch.size.toLong()).toInt())
            if (read < 0) return -1L
            sink.write(scratch, 0, read)
            return read.toLong()
        }
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() = input.close()
    }

    private class ConcatenatedSource(private val sources: List<ByteSource>) : Source {
        private var index = 0
        private var current: Source? = null
        private var closed = false

        override fun read(sink: Buffer, byteCount: Long): Long {
            check(!closed) { "closed" }
            require(byteCount >= 0L) { "byteCount ($byteCount) may not be negative" }
            if (byteCount == 0L) return 0L
            while (true) {
                val source = current ?: if (index < sources.size) {
                    sources[index++].openSource().also { current = it }
                } else {
                    return -1L
                }
                val read = source.read(sink, byteCount)
                if (read >= 0L) return read
                source.close()
                current = null
            }
        }

        override fun timeout(): Timeout = current?.timeout() ?: Timeout.NONE

        override fun close() {
            if (closed) return
            closed = true
            current?.close()
            current = null
        }
    }

    private class SlicedSource(
        private val upstream: Source,
        private var offset: Long,
        private var remaining: Long,
    ) : Source {
        private var closed = false
        private var prepared = false

        override fun read(sink: Buffer, byteCount: Long): Long {
            check(!closed) { "closed" }
            require(byteCount >= 0L) { "byteCount ($byteCount) may not be negative" }
            if (byteCount == 0L) return 0L
            prepare()
            if (remaining == 0L) return -1L
            val read = upstream.read(sink, min(byteCount, remaining))
            if (read < 0L) {
                remaining = 0L
                return -1L
            }
            remaining -= read
            return read
        }

        private fun prepare() {
            if (prepared) return
            prepared = true
            val discarded = Buffer()
            while (offset > 0L) {
                val read = upstream.read(discarded, min(offset, SEGMENT_SIZE))
                if (read < 0L) {
                    offset = 0L
                    remaining = 0L
                    return
                }
                offset -= read
                discarded.clear()
            }
        }

        override fun timeout(): Timeout = upstream.timeout()

        override fun close() {
            if (closed) return
            closed = true
            upstream.close()
        }
    }
}

/** Compatibility destination from GuavaKt's early in-memory I/O API. */
interface ByteSinkLike {
    fun openStream(): ByteArrayOutputLike
}

class ByteArrayInputLike(private val data: ByteArray) : AutoCloseable {
    private var position = 0

    fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

    fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length) { "invalid offset/length" }
        if (length == 0) return 0
        if (position >= data.size) return -1
        val read = min(length, data.size - position)
        data.copyInto(buffer, offset, position, position + read)
        position += read
        return read
    }

    fun read(): Int = if (position >= data.size) -1 else data[position++].toInt() and 0xff
    override fun close() = Unit
}

open class ByteArrayOutputLike : AutoCloseable {
    private val buffer = ArrayList<Byte>()
    fun write(byte: Int) { buffer.add(byte.toByte()) }
    fun write(bytes: ByteArray, offset: Int, length: Int) {
        require(offset >= 0 && length >= 0 && offset <= bytes.size - length) { "invalid offset/length" }
        for (index in offset until offset + length) buffer.add(bytes[index])
    }
    fun toByteArray(): ByteArray = buffer.toByteArray()
    override fun close() = Unit
}
