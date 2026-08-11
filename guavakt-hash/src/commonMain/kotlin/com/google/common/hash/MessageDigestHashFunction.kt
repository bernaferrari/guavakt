package dev.guavakt.hash

/**
 * Guava MessageDigestHashFunction — **pure Kotlin** MD5 / SHA-1 / SHA-256 / SHA-512 (FIPS-aligned).
 * Same code path on JVM, JS, Native, and Wasm (no JCA dependency).
 */
class MessageDigestHashFunction(
    private val algorithmName: String,
) : AbstractNonStreamingHashFunction() {
    override fun bits(): Int = when (algorithmName.uppercase()) {
        "MD5" -> 128
        "SHA-1", "SHA1" -> 160
        "SHA-256", "SHA256" -> 256
        "SHA-384", "SHA384" -> 384
        "SHA-512", "SHA512" -> 512
        else -> 256
    }

    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode =
        HashCode.fromBytes(platformDigest(algorithmName, input, off, len))

    /** SHA-256 is the common fixed-memory digest path; other portable digests remain one-shot today. */
    override fun newHasher(): Hasher = when (algorithmName.uppercase()) {
        "SHA-256", "SHA256" -> DigestAlgorithms.sha256Hasher()
        "SHA-384", "SHA384" -> DigestAlgorithms.sha384Hasher()
        "SHA-512", "SHA512" -> DigestAlgorithms.sha512Hasher()
        else -> super.newHasher()
    }

    fun algorithm(): String = algorithmName
}
