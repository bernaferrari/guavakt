package dev.guavakt.io

/** Guava ReaderInputStream — encodes chars to bytes (UTF-8). */
class ReaderInputStream(private val reader: CharReaderLike) : AutoCloseable {
    private val pending = ArrayDeque<Byte>()
    private var pendingHighSurrogate: Char? = null

    fun read(buf: ByteArray): Int = read(buf, 0, buf.size)

    fun read(buf: ByteArray, off: Int, len: Int): Int {
        require(off >= 0 && len >= 0 && off <= buf.size - len) { "invalid offset/length" }
        if (len == 0) return 0
        var written = 0
        while (written < len) {
            if (pending.isEmpty() && !refill()) return if (written == 0) -1 else written
            buf[off + written++] = pending.removeFirst()
        }
        return written
    }

    private fun refill(): Boolean {
        while (pending.isEmpty()) {
            val next = reader.read()
            if (next < 0) {
                val high = pendingHighSurrogate ?: return false
                pendingHighSurrogate = null
                enqueue(high.toString())
                continue
            }

            val character = next.toChar()
            val high = pendingHighSurrogate
            if (high != null) {
                pendingHighSurrogate = null
                if (character.isLowSurrogate()) {
                    enqueue(charArrayOf(high, character).concatToString())
                    continue
                }
                enqueue(high.toString())
            }

            if (character.isHighSurrogate()) {
                pendingHighSurrogate = character
            } else {
                enqueue(character.toString())
            }
        }
        return true
    }

    private fun enqueue(value: String) {
        for (byte in value.encodeToByteArray()) pending.addLast(byte)
    }

    override fun close() = reader.close()
}
