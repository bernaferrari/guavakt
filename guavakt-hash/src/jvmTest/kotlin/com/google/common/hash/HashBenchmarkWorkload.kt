package dev.guavakt.hash

/** JVM-only deterministic workload batches called by the JMH hash harness. */
class HashBenchmarkWorkload {
    private val payload = ByteArray(PAYLOAD_SIZE) { index -> (index * 31 + 17).toByte() }
    private val murmur = Hashing.murmur3_128()
    private val sha256 = Hashing.sha256()

    /** Measures the non-streaming hashBytes path for a moderate, fixed payload. */
    fun murmur3OneShotBatch(): Int {
        var result = 0
        repeat(MURMUR_OPERATIONS) { result = result xor murmur.hashBytes(payload).asInt() }
        return result
    }

    /** Measures the streaming state machine with deliberately awkward chunk boundaries. */
    fun murmur3StreamingBatch(): Int {
        var result = 0
        repeat(MURMUR_OPERATIONS) {
            val hasher = murmur.newHasher()
            var offset = 0
            while (offset < payload.size) {
                val length = minOf(STREAM_CHUNK_SIZE, payload.size - offset)
                hasher.putBytes(payload, offset, length)
                offset += length
            }
            result = result xor hasher.hash().asInt()
        }
        return result
    }

    /** Keeps a cryptographic streaming trace separate from the non-cryptographic Murmur traces. */
    fun sha256StreamingBatch(): Int {
        var result = 0
        repeat(SHA_OPERATIONS) {
            val hasher = sha256.newHasher()
            var offset = 0
            while (offset < payload.size) {
                val length = minOf(STREAM_CHUNK_SIZE, payload.size - offset)
                hasher.putBytes(payload, offset, length)
                offset += length
            }
            result = result xor hasher.hash().asInt()
        }
        return result
    }

    private companion object {
        const val PAYLOAD_SIZE = 4_096
        const val STREAM_CHUNK_SIZE = 17
        const val MURMUR_OPERATIONS = 32
        const val SHA_OPERATIONS = 8
    }
}
