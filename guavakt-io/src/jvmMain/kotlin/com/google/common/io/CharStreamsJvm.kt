package dev.guavakt.io

import java.io.Reader
import java.io.Writer
import java.nio.CharBuffer

/**
 * JVM [java.io.Reader] overloads for Guava [CharStreams] (not available on commonMain).
 */
fun CharStreams.toString(reader: Reader): String = reader.readText()

fun CharStreams.copy(from: Reader, to: Appendable): Long {
    val buf = CharArray(0x800)
    var total = 0L
    while (true) {
        val n = from.read(buf)
        if (n < 0) break
        to.append(buf.concatToString(0, n))
        total += n
    }
    return total
}

fun CharStreams.copy(from: Reader, to: StringBuilder): Long = copy(from, to as Appendable)

fun CharStreams.asWriter(appendable: Appendable): Writer =
    if (appendable is Writer) appendable
    else object : Writer() {
        override fun write(cbuf: CharArray, off: Int, len: Int) {
            appendable.append(cbuf.concatToString(off, off + len))
        }
        override fun flush() {}
        override fun close() {}
        override fun append(csq: CharSequence?): Writer {
            appendable.append(csq)
            return this
        }
    }

fun CharStreams.readLines(reader: Reader): List<String> = reader.readLines()
