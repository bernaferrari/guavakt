package dev.guavakt.hash

object Hashing {
    private val MURMUR3_32 = Murmur3_32HashFunction(0)
    private val MURMUR3_128 = Murmur3_128HashFunction(0)
    private val SIP_HASH_24 = SipHashFunction()
    private val CRC32C = Crc32cHashFunction()
    private val CRC32 = ChecksumHashFunction()
    private val FARM = FarmHashFingerprint64()
    private val FINGERPRINT_2011 = Fingerprint2011
    private val MD5 = MessageDigestHashFunction("MD5")
    private val SHA1 = MessageDigestHashFunction("SHA-1")
    private val SHA256 = MessageDigestHashFunction("SHA-256")
    private val SHA384 = MessageDigestHashFunction("SHA-384")
    private val SHA512 = MessageDigestHashFunction("SHA-512")

    fun murmur3_32(): HashFunction = MURMUR3_32
    fun murmur3_32(seed: Int): HashFunction = Murmur3_32HashFunction(seed)
    fun murmur3_32_fixed(): HashFunction = MURMUR3_32
    /**
     * Returns the corrected Murmur3 32-bit variant with [seed].
     *
     * Kotlin strings are encoded normally, including supplementary Unicode code
     * points, so both 32-bit entry points use the corrected Guava 33.x semantics.
     */
    fun murmur3_32_fixed(seed: Int): HashFunction = Murmur3_32HashFunction(seed)
    fun murmur3_128(): HashFunction = MURMUR3_128
    fun murmur3_128(seed: Int): HashFunction = Murmur3_128HashFunction(seed)
    fun sipHash24(): HashFunction = SIP_HASH_24
    fun sipHash24(k0: Long, k1: Long): HashFunction = SipHashFunction(k0, k1)
    fun crc32c(): HashFunction = CRC32C
    fun crc32(): HashFunction = CRC32
    fun adler32(): HashFunction = Adler32HashFunction()
    fun farmHashFingerprint64(): HashFunction = FARM
    fun fingerprint2011(): HashFunction = FINGERPRINT_2011
    fun md5(): HashFunction = MD5
    fun sha1(): HashFunction = SHA1
    fun sha256(): HashFunction = SHA256
    fun sha384(): HashFunction = SHA384
    fun sha512(): HashFunction = SHA512
    fun hmacMd5(key: ByteArray): HashFunction = MacHashFunction(key, "HmacMD5")
    fun hmacSha1(key: ByteArray): HashFunction = MacHashFunction(key, "HmacSHA1")
    fun hmacSha256(key: ByteArray): HashFunction = MacHashFunction(key, "HmacSHA256")
    fun hmacSha512(key: ByteArray): HashFunction = MacHashFunction(key, "HmacSHA512")
    fun goodFastHash(minimumBits: Int): HashFunction {
        require(minimumBits > 0)
        val bits = ((minimumBits + 31) / 32) * 32
        if (bits == 32) return murmur3_32()
        if (bits <= 128) return murmur3_128()
        val functions = Array((bits + 127) / 128) { index ->
            Murmur3_128HashFunction(index * 1_500_450_271)
        }
        return object : AbstractCompositeHashFunction(*functions) {}
    }

    fun consistentHash(input: Long, buckets: Int): Int {
        require(buckets > 0)
        var k = input
        var b = -1L
        var j = 0L
        while (j < buckets) {
            b = j
            k = k * 2862933555777941757L + 1L
            j = ((b + 1L) * (1L shl 31) / ((k ushr 33) + 1L))
        }
        return b.toInt()
    }
    fun consistentHash(hashCode: HashCode, buckets: Int): Int =
        consistentHash(hashCode.padToLong(), buckets)

    /** Combines equally sized hash codes using Guava's order-sensitive recurrence. */
    fun combineOrdered(hashCodes: Iterable<HashCode>): HashCode {
        val codes = hashCodes.toList()
        require(codes.isNotEmpty()) { "Must be at least 1 hash code to combine." }
        val result = ByteArray(codes.first().asBytes().size)
        for (code in codes) {
            val bytes = code.asBytes()
            require(bytes.size == result.size) { "All hashcodes must have the same bit length." }
            for (index in result.indices) {
                result[index] = (result[index].toInt() * 37 xor bytes[index].toInt()).toByte()
            }
        }
        return HashCode.fromBytes(result)
    }

    /** Combines equally sized hash codes without regard to their iteration order. */
    fun combineUnordered(hashCodes: Iterable<HashCode>): HashCode {
        val codes = hashCodes.toList()
        require(codes.isNotEmpty()) { "Must be at least 1 hash code to combine." }
        val result = ByteArray(codes.first().asBytes().size)
        for (code in codes) {
            val bytes = code.asBytes()
            require(bytes.size == result.size) { "All hashcodes must have the same bit length." }
            for (index in result.indices) result[index] = (result[index] + bytes[index]).toByte()
        }
        return HashCode.fromBytes(result)
    }

    /** Creates a hash function that concatenates the output of at least two functions. */
    fun concatenating(
        first: HashFunction,
        second: HashFunction,
        vararg rest: HashFunction,
    ): HashFunction = ConcatenatedHashFunction(arrayOf(first, second, *rest))

    /** Creates a hash function that concatenates the output of one or more functions. */
    fun concatenating(functions: Iterable<HashFunction>): HashFunction {
        val copy = functions.toList()
        require(copy.isNotEmpty()) { "number of hash functions must be > 0" }
        return ConcatenatedHashFunction(copy.toTypedArray())
    }

    private class ConcatenatedHashFunction(functions: Array<out HashFunction>) :
        AbstractCompositeHashFunction(*functions)
}
