package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HashAlgorithmTest {
    @Test
    fun murmur3_128_is_128_bits_and_differs_from_32() {
        val h128 = Hashing.murmur3_128()
        val h32 = Hashing.murmur3_32()
        assertEquals(128, h128.bits())
        assertEquals(32, h32.bits())
        val a = h128.hashBytes(byteArrayOf(1, 2, 3, 4))
        val b = h128.hashBytes(byteArrayOf(1, 2, 3, 5))
        assertNotEquals(a, b)
        assertEquals(16, a.asBytes().size)
        // not the same as 32-bit murmur on same input width
        assertNotEquals(h32.hashBytes(byteArrayOf(1, 2, 3, 4)).asInt(), a.asInt())
    }

    @Test
    fun sipHash_stable_64() {
        val h = Hashing.sipHash24()
        assertEquals(64, h.bits())
        val x = h.hashBytes("hello".encodeToByteArray())
        val y = h.hashBytes("hello".encodeToByteArray())
        assertEquals(x, y)
        assertNotEquals(x, h.hashBytes("world".encodeToByteArray()))
    }

    @Test
    fun mac_and_checksum_not_murmur_delegate() {
        val mac = Hashing.hmacSha256(byteArrayOf(1, 2, 3))
        assertEquals(256, mac.bits())
        val c1 = mac.hashBytes(byteArrayOf(9))
        assertEquals(32, c1.asBytes().size) // SHA-256 HMAC output
        val crc = Hashing.crc32().hashBytes(byteArrayOf(1, 2, 3))
        assertEquals(4, crc.asBytes().size)
        assertNotEquals(
            Hashing.murmur3_32().hashBytes(byteArrayOf(9)).asBytes().size,
            c1.asBytes().size,
        )
    }


    @Test
    fun md5_known_vector_empty() {
        // MD5("") = d41d8cd98f00b204e9800998ecf8427e
        val h = Hashing.md5().hashBytes(ByteArray(0)).asBytes()
        val hex = h.joinToString("") { b ->
            val v = b.toInt() and 0xff
            ((v ushr 4).toString(16)) + (v and 0xf).toString(16)
        }
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex)
    }

    @Test
    fun sha256_known_vector_empty() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val h = Hashing.sha256().hashBytes(ByteArray(0)).asBytes()
        assertEquals(32, h.size)
        val hex = h.joinToString("") { b ->
            val v = b.toInt() and 0xff
            ((v ushr 4).toString(16)) + (v and 0xf).toString(16)
        }
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex)
    }
}
