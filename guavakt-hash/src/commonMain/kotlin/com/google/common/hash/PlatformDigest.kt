package dev.guavakt.hash

/**
 * Digest entrypoints — **pure Kotlin** [DigestAlgorithms] on every target (including JVM).
 * Guava uses JCA on JVM; we match FIPS/RFC vectors in Kotlin for one implementation everywhere.
 */
internal fun platformDigest(algorithm: String, input: ByteArray, off: Int, len: Int): ByteArray =
    when (algorithm.uppercase()) {
        "MD5" -> DigestAlgorithms.md5(input, off, len)
        "SHA-1", "SHA1" -> DigestAlgorithms.sha1(input, off, len)
        "SHA-256", "SHA256" -> DigestAlgorithms.sha256(input, off, len)
        "SHA-384", "SHA384" -> DigestAlgorithms.sha384(input, off, len)
        "SHA-512", "SHA512" -> DigestAlgorithms.sha512(input, off, len)
        else -> DigestAlgorithms.sha256(input, off, len)
    }

/**
 * HMAC in pure Kotlin (same construction Guava uses over a digest PRF).
 * Returns null only if algorithm is unknown (caller should not hit this).
 */
internal fun platformHmac(algorithm: String, key: ByteArray, input: ByteArray, off: Int, len: Int): ByteArray? {
    val (digestFn, blockSize) = when {
        algorithm.contains("MD5", ignoreCase = true) ->
            (DigestAlgorithms::md5) to 64
        algorithm.contains("SHA1") || algorithm.contains("SHA-1") ->
            (DigestAlgorithms::sha1) to 64
        algorithm.contains("384") ->
            (DigestAlgorithms::sha384) to 128
        algorithm.contains("512") ->
            (DigestAlgorithms::sha512) to 128
        else ->
            (DigestAlgorithms::sha256) to 64
    }
    return DigestAlgorithms.hmac(key, input, off, len, digestFn, blockSize)
}
