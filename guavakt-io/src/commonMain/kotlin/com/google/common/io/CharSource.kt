package dev.guavakt.io

import okio.BufferedSource
import okio.Buffer
import okio.Source
import okio.Timeout
import kotlin.math.min

/**
 * A reusable source of characters.
 *
 * [openStream] is the compatibility hook for early GuavaKt subclasses. Primary operations use
 * [openReader], so UTF-8 file sources and concatenation can stream rather than materialize.
 */
abstract class CharSource {
    abstract fun openStream(): CharReaderLike

    open fun openReader(): CharReaderLike = openStream()

    /** The UTF-16 code-unit length when it can be determined without opening this source. */
    open fun lengthIfKnown(): Long? = null

    fun read(): String {
        val reader = openReader()
        return try {
            val result = StringBuilder()
            val buffer = CharArray(BUFFER_SIZE)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) return result.toString()
                result.appendRange(buffer, 0, read)
            }
            @Suppress("UNREACHABLE_CODE")
            result.toString()
        } finally {
            reader.close()
        }
    }

    fun readFirstLine(): String? {
        val reader = openReader()
        return try {
            LineReader(reader).readLine()
        } finally {
            reader.close()
        }
    }

    fun readLines(): List<String> {
        val reader = openReader()
        return try {
            val lineReader = LineReader(reader)
            buildList {
                while (true) add(lineReader.readLine() ?: break)
            }
        } finally {
            reader.close()
        }
    }

    /** Processes logical CR, LF, and CRLF lines without allocating an intermediate list. */
    fun <T> readLines(processor: LineProcessor<T>): T {
        val reader = openReader()
        return try {
            val lineReader = LineReader(reader)
            while (true) {
                val line = lineReader.readLine() ?: return processor.getResult()
                if (!processor.processLine(line)) return processor.getResult()
            }
            @Suppress("UNREACHABLE_CODE")
            processor.getResult()
        } finally {
            reader.close()
        }
    }

    fun isEmpty(): Boolean {
        lengthIfKnown()?.let { return it == 0L }
        val reader = openReader()
        return try {
            reader.read() < 0
        } finally {
            reader.close()
        }
    }

    fun length(): Long {
        lengthIfKnown()?.let { return it }
        val reader = openReader()
        return try {
            val buffer = CharArray(BUFFER_SIZE)
            var result = 0L
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) return result
                result += read
            }
            @Suppress("UNREACHABLE_CODE")
            result
        } finally {
            reader.close()
        }
    }

    /** Copies this source to [appendable], returning the number of UTF-16 code units copied. */
    fun copyTo(appendable: Appendable): Long {
        val reader = openReader()
        return try {
            val buffer = CharArray(BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) return total
                appendable.append(buffer.concatToString(0, read))
                total += read
            }
            @Suppress("UNREACHABLE_CODE")
            total
        } finally {
            reader.close()
        }
    }

    /** Copies this source to [sink], returning the number of UTF-16 code units copied. */
    fun copyTo(sink: CharSink): Long = sink.writeFrom(this)

    /**
     * Returns a streaming UTF-8 view of this source.
     *
     * Common Kotlin does not expose Java's arbitrary [java.nio.charset.Charset] surface, so this
     * adapter intentionally supports UTF-8 only. It preserves surrogate pairs across source reads
     * and closes the underlying reader when the returned Okio source is closed.
     */
    fun asByteSource(charsetName: String = "UTF-8"): ByteSource {
        require(charsetName.equals("UTF-8", ignoreCase = true) || charsetName.equals("UTF8", ignoreCase = true)) {
            "Only UTF-8 CharSource encoding is supported on common; got $charsetName"
        }
        val source = this
        return ByteSource.fromSource {
            val input = ReaderInputStream(source.openReader())
            object : Source {
                private val transfer = ByteArray(BUFFER_SIZE)

                override fun read(sink: Buffer, byteCount: Long): Long {
                    if (byteCount == 0L) return 0L
                    val read = input.read(transfer, 0, min(byteCount, transfer.size.toLong()).toInt())
                    if (read < 0) return -1L
                    sink.write(transfer, 0, read)
                    return read.toLong()
                }

                override fun timeout(): Timeout = Timeout.NONE

                override fun close() = input.close()
            }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 4_096

        fun wrap(seq: CharSequence): CharSource = object : CharSource() {
            override fun openStream(): CharReaderLike = CharReaderLike(seq.toString())
            override fun lengthIfKnown(): Long = seq.length.toLong()
        }

        /** Builds a reusable character source from an opener that returns a fresh reader each time. */
        fun fromReader(readerFactory: () -> CharReaderLike): CharSource = object : CharSource() {
            override fun openStream(): CharReaderLike = readerFactory()
            override fun openReader(): CharReaderLike = readerFactory()
        }

        fun empty(): CharSource = wrap("")

        /** Concatenates a snapshot of [sources], opening component readers lazily as they are read. */
        fun concat(vararg sources: CharSource): CharSource = concat(sources.asList())

        /** Concatenates an iterable snapshot, avoiding a Kotlin vararg spread at call sites. */
        fun concat(sources: Iterable<CharSource>): CharSource {
            val snapshot = sources.toList()
            return object : CharSource() {
                override fun openStream(): CharReaderLike = ConcatenatedCharReader(snapshot)
                override fun openReader(): CharReaderLike = ConcatenatedCharReader(snapshot)

                override fun lengthIfKnown(): Long? {
                    var total = 0L
                    for (source in snapshot) {
                        val length = source.lengthIfKnown() ?: return null
                        total = saturatedAdd(total, length)
                    }
                    return total
                }
            }
        }

        private fun saturatedAdd(left: Long, right: Long): Long =
            if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
    }

    private class ConcatenatedCharReader(private val sources: List<CharSource>) : CharReaderLike("") {
        private var index = 0
        private var current: CharReaderLike? = null
        private var closed = false

        override fun read(buffer: CharArray): Int {
            check(!closed) { "closed" }
            while (true) {
                val reader = current ?: if (index < sources.size) {
                    sources[index++].openReader().also { current = it }
                } else {
                    return -1
                }
                val read = reader.read(buffer)
                if (read >= 0) return read
                reader.close()
                current = null
            }
        }

        override fun read(): Int {
            val one = CharArray(1)
            return if (read(one) < 0) -1 else one[0].code
        }

        override fun close() {
            if (closed) return
            closed = true
            current?.close()
            current = null
        }
    }
}

/** Character reader used by common GuavaKt APIs and streaming Okio UTF-8 sources. */
open class CharReaderLike(private val data: String) : AutoCloseable {
    private var position = 0

    open fun read(buffer: CharArray): Int {
        if (position >= data.length) return -1
        val read = min(buffer.size, data.length - position)
        for (index in 0 until read) buffer[index] = data[position + index]
        position += read
        return read
    }

    open fun read(): Int = if (position >= data.length) -1 else data[position++].code
    override fun close() = Unit

    companion object {
        /** Creates a UTF-8 reader that decodes incrementally from [source]. */
        fun fromUtf8(source: BufferedSource): CharReaderLike = Utf8CharReader(source)
    }

    private class Utf8CharReader(private val source: BufferedSource) : CharReaderLike("") {
        private var pendingLowSurrogate: Char? = null

        override fun read(buffer: CharArray): Int {
            if (buffer.isEmpty()) return 0
            var count = 0
            while (count < buffer.size) {
                val next = read()
                if (next < 0) return if (count == 0) -1 else count
                buffer[count++] = next.toChar()
            }
            return count
        }

        override fun read(): Int {
            pendingLowSurrogate?.let {
                pendingLowSurrogate = null
                return it.code
            }
            if (source.exhausted()) return -1
            val codePoint = source.readUtf8CodePoint()
            if (codePoint <= Char.MAX_VALUE.code) return codePoint
            val adjusted = codePoint - 0x10000
            pendingLowSurrogate = (0xdc00 + (adjusted and 0x3ff)).toChar()
            return (0xd800 + (adjusted ushr 10)).toChar().code
        }

        override fun close() = source.close()
    }
}
