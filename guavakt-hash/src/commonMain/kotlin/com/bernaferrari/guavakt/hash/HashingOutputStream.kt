package com.bernaferrari.guavakt.hash

/** Guava HashingOutputStream — updates hasher as bytes are written. */
class HashingOutputStream(
    private val hasher: Hasher,
    private val output: (ByteArray, Int, Int) -> Unit,
) : AutoCloseable {
    fun write(b: Int) {
        val byte = b.toByte()
        hasher.putByte(byte)
        output(byteArrayOf(byte), 0, 1)
    }
    fun write(bytes: ByteArray, off: Int, len: Int) {
        hasher.putBytes(bytes, off, len)
        output(bytes, off, len)
    }
    fun hash(): HashCode = hasher.hash()
    override fun close() {}
}
