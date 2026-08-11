package dev.guavakt.hash

/** Portable, incremental Adler-32 checksum compatible with [Hashing.adler32]. */
class Adler32HashFunction : HashFunction {
    override fun bits(): Int = 32
    override fun hashInt(input: Int): HashCode = newHasher().putInt(input).hash()
    override fun hashLong(input: Long): HashCode = newHasher().putLong(input).hash()
    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode =
        newHasher().putBytes(input, off, len).hash()
    override fun hashUnencodedChars(input: CharSequence): HashCode = newHasher().putUnencodedChars(input).hash()
    override fun hashString(input: CharSequence, charsetName: String): HashCode =
        hashBytes(encodeString(input, charsetName))
    override fun newHasher(): Hasher = Adler32Hasher()
}

private class Adler32Hasher : AbstractByteHasher() {
    private var a = 1
    private var b = 0

    override fun update(b: Byte) {
        a = (a + (b.toInt() and 0xff)) % MOD_ADLER
        this.b = (this.b + a) % MOD_ADLER
    }

    override fun hash(): HashCode = HashCode.fromInt((b shl 16) or a)
}

private const val MOD_ADLER = 65_521
