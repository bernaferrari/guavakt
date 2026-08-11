package dev.guavakt.parity

import com.google.common.hash.HashFunction as GuavaHashFunction
import com.google.common.hash.Hashing as GuavaHashing
import dev.guavakt.hash.HashFunction as GuavaKtHashFunction
import dev.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class Adler32DifferentialTest {
    @Test
    fun adler32ChunkedHasherMatchesGuava() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        for (chunkSize in 1..17) {
            assertEquals(
                chunkedGuavaHash(GuavaHashing.adler32(), payload, chunkSize),
                chunkedGuavaKtHash(GuavaKtHashing.adler32(), payload, chunkSize),
                "chunkSize=$chunkSize",
            )
        }
    }

    private fun chunkedGuavaHash(function: GuavaHashFunction, bytes: ByteArray, chunkSize: Int): String {
        val hasher = function.newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }

    private fun chunkedGuavaKtHash(function: GuavaKtHashFunction, bytes: ByteArray, chunkSize: Int): String {
        val hasher = function.newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }
}
