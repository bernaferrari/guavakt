package dev.guavakt.io

/** Guava MultiInputStream — concatenates multiple byte streams. */
class MultiInputStream(private val streams: Iterator<ByteArrayInputLike>) : AutoCloseable {
    private var current: ByteArrayInputLike? = if (streams.hasNext()) streams.next() else null
    private fun advance() {
        current?.close()
        current = if (streams.hasNext()) streams.next() else null
    }
    fun read(buf: ByteArray): Int {
        while (current != null) {
            val n = current!!.read(buf)
            if (n >= 0) return n
            advance()
        }
        return -1
    }
    fun read(): Int {
        while (current != null) {
            val b = current!!.read()
            if (b >= 0) return b
            advance()
        }
        return -1
    }
    override fun close() { while (current != null) advance() }
}
