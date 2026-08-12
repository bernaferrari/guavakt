package com.bernaferrari.guavakt.hash

/**
 * Guava MacHashFunction — **pure Kotlin HMAC** over MD5/SHA-1/SHA-256/SHA-512.
 */
class MacHashFunction(
    private val key: ByteArray,
    private val algorithmName: String = "HmacSHA256",
) : AbstractNonStreamingHashFunction() {
    init {
        // Guava's byte-array HMAC factories delegate to SecretKeySpec, which rejects an empty key.
        require(key.isNotEmpty()) { "HMAC key must not be empty." }
    }
    override fun bits(): Int = when {
        algorithmName.contains("MD5", ignoreCase = true) -> 128
        algorithmName.contains("SHA1") || algorithmName.contains("SHA-1") -> 160
        algorithmName.contains("384") -> 384
        algorithmName.contains("512") -> 512
        else -> 256
    }

    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        val out = platformHmac(algorithmName, key, input, off, len)
            ?: DigestAlgorithms.hmac(key, input, off, len, DigestAlgorithms::sha256, 64)
        return HashCode.fromBytes(out)
    }

    /** SHA-2 HMACs stream input; legacy MD5/SHA-1 HMACs retain it for now. */
    override fun newHasher(): Hasher =
        DigestAlgorithms.hmacHasher(key, algorithmName) ?: super.newHasher()
}
