package com.bernaferrari.guavakt.hash

interface PrimitiveSink {
    fun putByte(b: Byte): PrimitiveSink
    fun putBytes(bytes: ByteArray): PrimitiveSink
    fun putBytes(bytes: ByteArray, off: Int, len: Int): PrimitiveSink {
        require(off >= 0 && len >= 0 && off <= bytes.size - len) {
            "Invalid byte range off=$off len=$len for ${bytes.size} bytes"
        }
        for (index in off until off + len) putByte(bytes[index])
        return this
    }
    fun putShort(s: Short): PrimitiveSink =
        putByte(s.toByte()).putByte((s.toInt() ushr Byte.SIZE_BITS).toByte())
    fun putInt(i: Int): PrimitiveSink
    fun putLong(l: Long): PrimitiveSink
    fun putFloat(f: Float): PrimitiveSink = putInt(f.toRawBits())
    fun putDouble(d: Double): PrimitiveSink = putLong(d.toRawBits())
    fun putBoolean(b: Boolean): PrimitiveSink = putByte(if (b) 1.toByte() else 0.toByte())
    fun putChar(c: Char): PrimitiveSink =
        putByte(c.code.toByte()).putByte((c.code ushr Byte.SIZE_BITS).toByte())
    fun putUnencodedChars(chars: CharSequence): PrimitiveSink
    fun putString(chars: CharSequence, charsetName: String = "UTF-8"): PrimitiveSink =
        putBytes(encodeString(chars, charsetName))
}
