package dev.guavakt.hash

/** Guava CRC32C (Castagnoli) — non-cryptographic checksum. */
class Crc32cHashFunction : HashFunction {
    override fun bits(): Int = 32
    override fun hashInt(input: Int): HashCode = newHasher().putInt(input).hash()
    override fun hashLong(input: Long): HashCode = newHasher().putLong(input).hash()
    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode =
        newHasher().putBytes(input, off, len).hash()
    override fun hashUnencodedChars(input: CharSequence): HashCode {
        val h = newHasher()
        for (c in input) {
            h.putByte(c.code.toByte())
            h.putByte((c.code ushr 8).toByte())
        }
        return h.hash()
    }
    override fun hashString(input: CharSequence, charsetName: String): HashCode =
        hashBytes(encodeString(input, charsetName))
    override fun newHasher(): Hasher = Crc32cHasher()
}

private class Crc32cHasher : Hasher {
    private var crc = 0.inv()
    override fun putByte(b: Byte): Hasher {
        crc = CRC32C_TABLE[(crc xor (b.toInt() and 0xff)) and 0xff] xor (crc ushr 8)
        return this
    }
    override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher {
        for (i in off until off + len) putByte(bytes[i])
        return this
    }
    override fun putInt(i: Int): Hasher {
        putByte(i.toByte()); putByte((i ushr 8).toByte())
        putByte((i ushr 16).toByte()); putByte((i ushr 24).toByte())
        return this
    }
    override fun putLong(l: Long): Hasher {
        for (s in 0 until 8) putByte((l ushr (s * 8)).toByte())
        return this
    }
    override fun putUnencodedChars(chars: CharSequence): Hasher {
        for (c in chars) {
            putByte(c.code.toByte())
            putByte((c.code ushr 8).toByte())
        }
        return this
    }
    override fun hash(): HashCode = HashCode.fromInt(crc.inv())
}

// Precomputed Castagnoli table
private val CRC32C_TABLE: IntArray = IntArray(256).also { table ->
    val poly = 0x82f63b78.toInt()
    for (i in 0 until 256) {
        var crc = i
        repeat(8) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor poly else crc ushr 1
        }
        table[i] = crc
    }
}
