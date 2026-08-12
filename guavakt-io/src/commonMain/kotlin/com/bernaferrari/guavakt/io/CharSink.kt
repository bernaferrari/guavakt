package com.bernaferrari.guavakt.io

import okio.BufferedSink

/**
 * A reusable UTF-8 character destination backed by an Okio sink.
 *
 * UTF-8 is the portable default. JVM-only charset bridges may be offered separately; common APIs
 * do not accept charset names that other targets cannot implement faithfully.
 */
abstract class CharSink {
    abstract fun openBufferedSink(): BufferedSink

    fun write(charSequence: CharSequence) {
        val sink = openBufferedSink()
        try {
            sink.writeUtf8(charSequence.toString())
        } finally {
            sink.close()
        }
    }

    fun writeLines(lines: Iterable<CharSequence>, lineSeparator: String = "\n") {
        val sink = openBufferedSink()
        try {
            for (line in lines) {
                sink.writeUtf8(line.toString())
                sink.writeUtf8(lineSeparator)
            }
        } finally {
            sink.close()
        }
    }

    /** Writes all characters from [source], returning the number of UTF-16 code units written. */
    fun writeFrom(source: CharSource): Long {
        val reader = source.openReader()
        val sink = openBufferedSink()
        return try {
            val buffer = CharArray(4_096)
            var pendingHighSurrogate: Char? = null
            var total = 0L
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                total += read
                var start = 0
                var end = read
                pendingHighSurrogate?.let { high ->
                    if (end > 0 && buffer[0].isLowSurrogate()) {
                        sink.writeUtf8("$high${buffer[0]}")
                        start = 1
                    } else {
                        sink.writeUtf8(high.toString())
                    }
                    pendingHighSurrogate = null
                }
                if (end > 0 && buffer[end - 1].isHighSurrogate()) {
                    pendingHighSurrogate = buffer[--end]
                }
                if (start < end) sink.writeUtf8(buffer.concatToString(start, end))
            }
            pendingHighSurrogate?.let { sink.writeUtf8(it.toString()) }
            total
        } finally {
            try {
                reader.close()
            } finally {
                sink.close()
            }
        }
    }

    companion object {
        internal fun from(byteSink: ByteSink): CharSink = object : CharSink() {
            override fun openBufferedSink(): BufferedSink = byteSink.openBufferedSink()
        }
    }
}
