package dev.guavakt.hash

interface Hasher : PrimitiveSink {
    override fun putByte(b: Byte): Hasher
    override fun putBytes(bytes: ByteArray): Hasher = putBytes(bytes, 0, bytes.size)
    override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher
    override fun putShort(s: Short): Hasher =
        putByte(s.toByte()).putByte((s.toInt() ushr Byte.SIZE_BITS).toByte())
    override fun putInt(i: Int): Hasher
    override fun putLong(l: Long): Hasher
    override fun putFloat(f: Float): Hasher = putInt(f.toRawBits())
    override fun putDouble(d: Double): Hasher = putLong(d.toRawBits())
    override fun putBoolean(b: Boolean): Hasher = putByte(if (b) 1.toByte() else 0.toByte())
    override fun putChar(c: Char): Hasher =
        putByte(c.code.toByte()).putByte((c.code ushr Byte.SIZE_BITS).toByte())
    override fun putUnencodedChars(chars: CharSequence): Hasher
    override fun putString(chars: CharSequence, charsetName: String): Hasher =
        putBytes(encodeString(chars, charsetName))
    fun hash(): HashCode
}
