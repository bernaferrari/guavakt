package com.bernaferrari.guavakt.io

/**
 * A reader over a [CharSequence], including Guava's mark/reset and skip semantics.
 *
 * Kotlin common code has no checked `IOException`; methods called after [close] therefore throw
 * [IllegalStateException]. The wrapped sequence is retained rather than copied, matching Guava's
 * live-`CharSequence` behavior.
 */
class CharSequenceReader(sequence: CharSequence) : AutoCloseable {
    private var sequence: CharSequence? = sequence
    private var position = 0
    private var mark = 0

    private fun openSequence(): CharSequence = checkNotNull(sequence) { "reader closed" }

    fun read(): Int {
        val source = openSequence()
        return if (position >= source.length) -1 else source[position++].code
    }

    fun read(cbuf: CharArray): Int = read(cbuf, 0, cbuf.size)

    fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || off > cbuf.size - len) throw IndexOutOfBoundsException("invalid offset/length")
        val source = openSequence()
        if (position >= source.length) return -1
        val count = minOf(len, source.length - position)
        for (index in 0 until count) cbuf[off + index] = source[position + index]
        position += count
        return count
    }

    fun skip(n: Long): Long {
        require(n >= 0) { "n ($n) may not be negative" }
        val source = openSequence()
        val skipped = minOf((source.length - position).toLong(), n).toInt()
        position += skipped
        return skipped.toLong()
    }

    /** This in-memory reader never blocks for input. */
    fun ready(): Boolean {
        openSequence()
        return true
    }

    fun markSupported(): Boolean = true

    fun mark(readAheadLimit: Int) {
        require(readAheadLimit >= 0) { "readAheadLimit ($readAheadLimit) may not be negative" }
        openSequence()
        mark = position
    }

    fun reset() {
        openSequence()
        position = mark
    }

    override fun close() {
        sequence = null
    }
}
