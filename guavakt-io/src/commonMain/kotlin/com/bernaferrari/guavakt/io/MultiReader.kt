package com.bernaferrari.guavakt.io

/** Guava MultiReader — concatenates multiple readers. */
class MultiReader(private val readers: Iterator<CharReaderLike>) : AutoCloseable {
    private var current: CharReaderLike? = if (readers.hasNext()) readers.next() else null
    private fun advance() {
        current?.close()
        current = if (readers.hasNext()) readers.next() else null
    }
    fun read(buf: CharArray): Int {
        while (current != null) {
            val n = current!!.read(buf)
            if (n >= 0) return n
            advance()
        }
        return -1
    }
    override fun close() { while (current != null) advance() }
}
