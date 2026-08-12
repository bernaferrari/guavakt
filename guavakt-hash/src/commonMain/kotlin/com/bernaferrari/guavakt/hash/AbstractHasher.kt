package com.bernaferrari.guavakt.hash

/** Guava AbstractHasher — default multi-byte puts via putByte. */
abstract class AbstractHasher : Hasher {
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
}
