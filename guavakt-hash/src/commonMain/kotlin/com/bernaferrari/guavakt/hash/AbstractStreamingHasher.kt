package com.bernaferrari.guavakt.hash

/**
 * Guava AbstractStreamingHasher — processes input in chunks via [process] / [processRemaining].
 */
abstract class AbstractStreamingHasher(
    private val bufferSize: Int,
    private val chunkSize: Int = bufferSize,
) : Hasher {
    private val buffer = ByteArray(bufferSize + 7)
    private var bufferCount = 0
    private var processAtLeast = chunkSize

    protected abstract fun process(bb: ByteArray, off: Int)
    protected abstract fun processRemaining(bb: ByteArray, off: Int, len: Int)
    protected abstract fun makeHash(): HashCode

    override fun putByte(b: Byte): Hasher {
        buffer[bufferCount++] = b
        if (bufferCount >= processAtLeast) {
            process(buffer, 0)
            bufferCount -= chunkSize
            // shift remaining
            for (i in 0 until bufferCount) buffer[i] = buffer[chunkSize + i]
        }
        return this
    }
    override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher {
        for (i in off until off + len) putByte(bytes[i])
        return this
    }
    override fun putInt(i: Int): Hasher =
        putBytes(byteArrayOf(i.toByte(), (i ushr 8).toByte(), (i ushr 16).toByte(), (i ushr 24).toByte()))
    override fun putLong(l: Long): Hasher {
        for (s in 0 until 8) putByte((l ushr (s * 8)).toByte())
        return this
    }
    override fun putUnencodedChars(chars: CharSequence): Hasher {
        for (c in chars) { putByte(c.code.toByte()); putByte((c.code ushr 8).toByte()) }
        return this
    }
    override fun hash(): HashCode {
        processRemaining(buffer, 0, bufferCount)
        return makeHash()
    }
}
