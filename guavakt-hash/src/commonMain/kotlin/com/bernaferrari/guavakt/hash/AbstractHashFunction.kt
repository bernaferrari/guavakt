package com.bernaferrari.guavakt.hash

/** Guava AbstractHashFunction — default implementations for hashInt/Long/String via hashBytes. */
abstract class AbstractHashFunction : HashFunction {
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
    override fun newHasher(): Hasher = AccumulatingHasher(this)
}
