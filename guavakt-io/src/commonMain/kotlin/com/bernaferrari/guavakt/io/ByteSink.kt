package com.bernaferrari.guavakt.io

import okio.BufferedSink
import okio.Sink
import okio.Source
import okio.buffer

/**
 * A reusable destination for bytes.
 *
 * Guava's `ByteSink` is shaped around `OutputStream`. GuavaKt exposes Okio [Sink] instead so the
 * same contract works in common Kotlin on JVM, JS, Wasm, and Native. [openStream] is retained as a
 * migration adapter for GuavaKt's early in-memory stream API; new code should use [openSink].
 */
abstract class ByteSink : ByteSinkLike {
    /** Opens a new sink. Each call must return an independently closeable sink. */
    abstract fun openSink(): Sink

    open fun openBufferedSink(): BufferedSink = openSink().buffer()

    fun write(bytes: ByteArray) {
        val sink = openBufferedSink()
        try {
            sink.write(bytes)
        } finally {
            sink.close()
        }
    }

    /** Copies all remaining bytes from [source] without closing it. */
    fun writeFrom(source: Source): Long {
        val sink = openBufferedSink()
        return try {
            sink.writeAll(source)
        } finally {
            sink.close()
        }
    }

    fun asCharSink(): CharSink = CharSink.from(this)

    /** Compatibility adapter for the pre-Okio GuavaKt API. */
    final override fun openStream(): ByteArrayOutputLike = object : ByteArrayOutputLike() {
        private var closed = false

        override fun close() {
            if (closed) return
            closed = true
            this@ByteSink.write(toByteArray())
        }
    }
}
