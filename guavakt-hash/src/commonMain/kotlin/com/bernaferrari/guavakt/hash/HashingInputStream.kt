package com.bernaferrari.guavakt.hash

/** Guava HashingInputStream — updates hasher as bytes are read. */
class HashingInputStream(
    private val hasher: Hasher,
    private val input: (ByteArray) -> Int,
) : AutoCloseable {
    private var closed = false
    fun read(buf: ByteArray): Int {
        val n = input(buf)
        if (n > 0) hasher.putBytes(buf, 0, n)
        return n
    }
    fun hash(): HashCode = hasher.hash()
    override fun close() { closed = true }
}
