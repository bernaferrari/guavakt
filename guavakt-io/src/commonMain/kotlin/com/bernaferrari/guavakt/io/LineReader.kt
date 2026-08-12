package com.bernaferrari.guavakt.io

/** Guava LineReader — reads lines from Readable. */
class LineReader(private val readable: CharReaderLike) {
    private val lines = ArrayDeque<String>()
    private val buf = CharArray(4096)
    private val lineBuf = object : LineBuffer() {
        override fun handleLine(line: String, end: String) { lines.addLast(line) }
    }

    fun readLine(): String? {
        while (lines.isEmpty()) {
            val n = readable.read(buf)
            if (n < 0) {
                lineBuf.finish()
                break
            }
            lineBuf.add(buf, 0, n)
        }
        return if (lines.isEmpty()) null else lines.removeFirst()
    }
}
