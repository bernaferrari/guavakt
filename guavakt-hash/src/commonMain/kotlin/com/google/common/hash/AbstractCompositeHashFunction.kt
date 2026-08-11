package dev.guavakt.hash

/**
 * Guava AbstractCompositeHashFunction — concatenates hashes from multiple functions.
 */
abstract class AbstractCompositeHashFunction(
    private vararg val functions: HashFunction,
) : HashFunction {
    override fun bits(): Int = functions.sumOf { it.bits() }
    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        val parts = functions.map { it.hashBytes(input, off, len).asBytes() }
        val total = parts.sumOf { it.size }
        val out = ByteArray(total)
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return HashCode.fromBytes(out)
    }
    override fun hashInt(input: Int): HashCode = hashBytes(
        byteArrayOf(input.toByte(), (input ushr 8).toByte(), (input ushr 16).toByte(), (input ushr 24).toByte())
    )
    override fun hashLong(input: Long): HashCode =
        hashBytes(ByteArray(8) { (input ushr (it * 8)).toByte() })
    override fun hashUnencodedChars(input: CharSequence): HashCode {
        val bytes = ByteArray(input.length * 2)
        for (index in input.indices) {
            val code = input[index].code
            bytes[index * 2] = code.toByte()
            bytes[index * 2 + 1] = (code ushr 8).toByte()
        }
        return hashBytes(bytes)
    }
    override fun hashString(input: CharSequence, charsetName: String): HashCode =
        hashBytes(encodeString(input, charsetName))
    /**
     * Feeds each child hasher as bytes arrive instead of retaining a full input
     * merely to calculate a concatenated result at the end.
     */
    override fun newHasher(): Hasher = CompositeHasher(functions.map { it.newHasher() })

    private class CompositeHasher(private val hashers: List<Hasher>) : Hasher {
        override fun putByte(b: Byte): Hasher = apply { hashers.forEach { it.putByte(b) } }
        override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher = apply {
            hashers.forEach { it.putBytes(bytes, off, len) }
        }
        override fun putInt(i: Int): Hasher = apply { hashers.forEach { it.putInt(i) } }
        override fun putLong(l: Long): Hasher = apply { hashers.forEach { it.putLong(l) } }
        override fun putUnencodedChars(chars: CharSequence): Hasher = apply {
            hashers.forEach { it.putUnencodedChars(chars) }
        }
        override fun hash(): HashCode = concatenateHashCodes(hashers.map { it.hash() })
    }
}

internal fun concatenateHashCodes(codes: Iterable<HashCode>): HashCode {
    val parts = codes.map { it.asBytes() }
    require(parts.isNotEmpty()) { "At least one hash function is required." }
    val result = ByteArray(parts.sumOf { it.size })
    var offset = 0
    for (part in parts) {
        part.copyInto(result, offset)
        offset += part.size
    }
    return HashCode.fromBytes(result)
}
