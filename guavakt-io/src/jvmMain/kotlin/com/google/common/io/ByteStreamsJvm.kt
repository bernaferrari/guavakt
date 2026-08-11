package dev.guavakt.io

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * JVM [java.io.InputStream]/[OutputStream] overloads for Guava [ByteStreams].
 */
fun ByteStreams.toByteArray(input: InputStream): ByteArray = input.readBytes()

fun ByteStreams.copy(from: InputStream, to: OutputStream): Long {
    val buf = ByteArray(8192)
    var total = 0L
    while (true) {
        val n = from.read(buf)
        if (n < 0) break
        to.write(buf, 0, n)
        total += n
    }
    return total
}

fun ByteStreams.limit(input: InputStream, limit: Long): InputStream =
    object : InputStream() {
        private var remaining = limit
        override fun read(): Int {
            if (remaining <= 0) return -1
            val b = input.read()
            if (b >= 0) remaining--
            return b
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val toRead = minOf(len.toLong(), remaining).toInt()
            val n = input.read(b, off, toRead)
            if (n > 0) remaining -= n
            return n
        }
        override fun close() = input.close()
    }

fun ByteStreams.nullOutputStream(): OutputStream =
    object : OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray, off: Int, len: Int) {}
    }

fun ByteStreams.exhaust(input: InputStream): Long {
    val buf = ByteArray(8192)
    var total = 0L
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total += n
    }
    return total
}
