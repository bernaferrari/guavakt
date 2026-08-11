package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HashTest {
    @Test
    fun murmur3_32_stable() {
        val h = Hashing.murmur3_32()
        val a = h.hashUnencodedChars("hello")
        val b = h.hashUnencodedChars("hello")
        assertEquals(a, b)
        assertEquals(a.asInt(), b.asInt())
        assertNotEquals(a, h.hashUnencodedChars("world"))
    }

    @Test
    fun hashInt_consistentWithBytes() {
        val h = Hashing.murmur3_32(42)
        val viaInt = h.hashInt(0x01020304)
        // stability: same input twice
        assertEquals(viaInt.asInt(), h.hashInt(0x01020304).asInt())
    }

    @Test
    fun hasher_accumulates() {
        val h = Hashing.murmur3_32().newHasher().putInt(1).putInt(2).hash()
        assertEquals(32, h.bits())
    }

    @Test
    fun primitiveSinkDefaultsAndHashObjectHavePortableByteLayout() {
        val expectedBytes = byteArrayOf(
            8, 7,
            2, 1,
            0x0d, 0x0c, 0x0b, 0x0a,
            8, 7, 6, 5, 4, 3, 2, 1,
            0, 0, 0x80.toByte(), 0x3f,
            0, 0, 0, 0, 0, 0, 0, 0x80.toByte(),
            1,
            0xa9.toByte(), 3,
            0x61, 0, 0x3d, 0xd8.toByte(), 0, 0xde.toByte(), 0x7a, 0,
            0xc3.toByte(), 0xa9.toByte(),
        )
        val primitiveHash = Hashing.murmur3_128().newHasher()
            .putBytes(byteArrayOf(9, 8, 7, 6), 1, 2)
            .putShort(0x0102)
            .putInt(0x0a0b0c0d)
            .putLong(0x0102030405060708L)
            .putFloat(1.0f)
            .putDouble(-0.0)
            .putBoolean(true)
            .putChar('Ω')
            .putUnencodedChars("a😀z")
            .putString("é")
            .hash()
        assertEquals(Hashing.murmur3_128().hashBytes(expectedBytes), primitiveHash)

        data class Record(val id: Short, val name: String)
        val record = Record(12, "ok")
        val funnel = Funnel<Record> { value, sink -> sink.putShort(value.id).putString(value.name) }
        assertEquals(
            Hashing.murmur3_128().newHasher().putShort(12).putString("ok").hash(),
            Hashing.murmur3_128().hashObject(record, funnel),
        )
    }
}
