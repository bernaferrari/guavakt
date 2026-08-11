package dev.guavakt.hash

/**
 * Guava AbstractByteHasher — [Hasher] that implements multi-byte puts via [update].
 */
abstract class AbstractByteHasher : Hasher {
    protected abstract fun update(b: Byte)
    protected open fun update(bytes: ByteArray, off: Int, len: Int) {
        for (i in off until off + len) update(bytes[i])
    }
    override fun putByte(b: Byte): Hasher { update(b); return this }
    override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher { update(bytes, off, len); return this }
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
}
