package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Bit-level vectors aligned with Guava / FIPS / RFC — shelter-critical correctness.
 */
class GuavaVectorFidelityTest {
    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { b ->
            val v = b.toInt() and 0xff
            ((v ushr 4).toString(16)) + (v and 0xf).toString(16)
        }

    @Test fun md5_empty() =
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex(Hashing.md5().hashBytes(ByteArray(0)).asBytes()))

    @Test fun md5_hello() =
        assertEquals("5d41402abc4b2a76b9719d911017c592", hex(Hashing.md5().hashBytes("hello".encodeToByteArray()).asBytes()))

    @Test fun sha1_empty() =
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex(Hashing.sha1().hashBytes(ByteArray(0)).asBytes()))

    @Test fun sha256_empty() =
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(Hashing.sha256().hashBytes(ByteArray(0)).asBytes()),
        )

    @Test fun sha256_abc() =
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(Hashing.sha256().hashBytes("abc".encodeToByteArray()).asBytes()),
        )

    @Test fun sha512_empty() =
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            hex(Hashing.sha512().hashBytes(ByteArray(0)).asBytes()),
        )

    @Test fun murmur3_32_bits() = assertEquals(32, Hashing.murmur3_32().bits())

    @Test fun murmur3_128_bits_and_length() {
        assertEquals(128, Hashing.murmur3_128().bits())
        assertEquals(16, Hashing.murmur3_128().hashBytes(byteArrayOf(1)).asBytes().size)
    }

    @Test fun sipHash_deterministic() {
        val a = Hashing.sipHash24().hashBytes("cat".encodeToByteArray())
        val b = Hashing.sipHash24().hashBytes("cat".encodeToByteArray())
        assertEquals(a, b)
        assertEquals(64, Hashing.sipHash24().bits())
    }

    @Test
    fun hmacSha256_known_vector() {
        // key="key", data="hello" → 9307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b
        val h = Hashing.hmacSha256("key".encodeToByteArray())
            .hashBytes("hello".encodeToByteArray()).asBytes()
        assertEquals(
            "9307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b",
            hex(h),
        )
    }
}
