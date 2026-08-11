package dev.guavakt.hash

/**
 * Guava ChecksumHashFunction — 32-bit IEEE CRC32 over bytes (KMP pure implementation).
 */
class ChecksumHashFunction : HashFunction {
    override fun bits(): Int = 32
    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        var crc = 0xffffffffL
        for (i in off until off + len) {
            val idx = ((crc xor (input[i].toLong() and 0xffL)) and 0xffL).toInt()
            crc = (crc ushr 8) xor CRC32_TABLE[idx]
        }
        return HashCode.fromInt((crc xor 0xffffffffL).toInt())
    }
    override fun hashInt(input: Int): HashCode = hashBytes(
        byteArrayOf(input.toByte(), (input ushr 8).toByte(), (input ushr 16).toByte(), (input ushr 24).toByte())
    )
    override fun hashLong(input: Long): HashCode =
        hashBytes(ByteArray(8) { (input ushr (it * 8)).toByte() })
    override fun hashUnencodedChars(input: CharSequence): HashCode {
        val bytes = ByteArray(input.length * 2)
        for (i in input.indices) {
            val c = input[i].code
            bytes[i * 2] = c.toByte(); bytes[i * 2 + 1] = (c ushr 8).toByte()
        }
        return hashBytes(bytes)
    }
    override fun hashString(input: CharSequence, charsetName: String): HashCode =
        hashBytes(encodeString(input, charsetName))
    /** Incremental CRC32 state; input is never retained. */
    override fun newHasher(): Hasher = Crc32Hasher()

}

private class Crc32Hasher : AbstractByteHasher() {
    private var crc = 0xffffffffL

    override fun update(b: Byte) {
        val index = ((crc xor (b.toLong() and 0xffL)) and 0xffL).toInt()
        crc = (crc ushr 8) xor CRC32_TABLE[index]
    }

    override fun hash(): HashCode = HashCode.fromInt((crc xor 0xffffffffL).toInt())
}

private val CRC32_TABLE: LongArray = LongArray(256) { n ->
    var c = n.toLong()
    repeat(8) {
        c = if (c and 1L != 0L) 0xedb88320L xor (c ushr 1) else c ushr 1
    }
    c
}
