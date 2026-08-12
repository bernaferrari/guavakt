package com.bernaferrari.guavakt.parity

import com.google.common.hash.HashCode as GuavaHashCode
import com.google.common.hash.Funnel as GuavaFunnel
import com.google.common.hash.Hashing as GuavaHashing
import com.google.common.hash.PrimitiveSink as GuavaPrimitiveSink
import com.bernaferrari.guavakt.hash.Funnel as GuavaKtFunnel
import com.bernaferrari.guavakt.hash.HashCode
import com.bernaferrari.guavakt.hash.Hashing
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HashDifferentialTest {
    @Test fun murmur3StringHonorsCharset() {
        val value = "GuavaKt ☃"
        for (charset in listOf("UTF-8", "UTF-16LE", "UTF-16BE", "ISO-8859-1")) {
            val encodable = if (charset == "ISO-8859-1") "GuavaKt é" else value
            assertEquals(
                GuavaHashing.murmur3_32_fixed().hashString(encodable, java.nio.charset.Charset.forName(charset)).toString(),
                Hashing.murmur3_32_fixed().hashString(encodable, charset).toString(),
            )
        }
    }

    @Test fun farmHashMatchesAcrossEveryLengthBoundary() {
        val guava = GuavaHashing.farmHashFingerprint64()
        val ours = Hashing.farmHashFingerprint64()
        for (length in listOf(0, 1, 3, 4, 7, 8, 16, 17, 32, 33, 64, 65, 80, 127, 128, 129, 1024)) {
            val bytes = ByteArray(length) { (it * 37 + 11).toByte() }
            assertEquals(guava.hashBytes(bytes).toString(), ours.hashBytes(bytes).toString(), "length=$length")
        }
    }

    @Test fun shortHashCodeHasStableJavaHashCode() {
        for (bytes in listOf(byteArrayOf(0x7f), byteArrayOf(1, 2), byteArrayOf(1, 2, 3))) {
            assertEquals(GuavaHashCode.fromBytes(bytes).hashCode(), HashCode.fromBytes(bytes).hashCode())
        }
    }

    @Test fun emptyHashCodeIsRejected() {
        assertFailsWith<IllegalArgumentException> { HashCode.fromBytes(byteArrayOf()) }
    }

    @Test fun consistentHashAccepts32BitCodes() {
        val guavaCode = GuavaHashCode.fromInt(0x12345678)
        val ourCode = HashCode.fromInt(0x12345678)
        assertEquals(GuavaHashing.consistentHash(guavaCode, 17), Hashing.consistentHash(ourCode, 17))
    }

    @Test fun goodFastHashScalesBeyond128Bits() {
        for (requested in listOf(1, 32, 33, 128, 129, 256, 1000)) {
            assertEquals(GuavaHashing.goodFastHash(requested).bits(), Hashing.goodFastHash(requested).bits())
        }
    }

    @Test fun primitiveSinkAndHashObjectLayoutsMatchGuava() {
        val bytes = byteArrayOf(9, 8, 7, 6)
        val float = Float.fromBits(0x7fc00001)
        val double = Double.fromBits(0x7ff8000000000001UL.toLong())
        fun guavaHash() = GuavaHashing.murmur3_128().newHasher()
            .putBytes(bytes, 1, 2)
            .putShort((-1234).toShort())
            .putInt(Int.MIN_VALUE)
            .putLong(Long.MAX_VALUE)
            .putFloat(float)
            .putDouble(double)
            .putBoolean(true)
            .putBoolean(false)
            .putChar('☃')
            .putUnencodedChars("a😀z")
            .putString("café", StandardCharsets.UTF_16LE)
            .hash()
            .toString()
        fun guavaKtHash() = Hashing.murmur3_128().newHasher()
            .putBytes(bytes, 1, 2)
            .putShort((-1234).toShort())
            .putInt(Int.MIN_VALUE)
            .putLong(Long.MAX_VALUE)
            .putFloat(float)
            .putDouble(double)
            .putBoolean(true)
            .putBoolean(false)
            .putChar('☃')
            .putUnencodedChars("a😀z")
            .putString("café", "UTF-16LE")
            .hash()
            .toString()
        assertEquals(guavaHash(), guavaKtHash())

        val record = PrimitiveRecord((-7).toShort(), 'Ω', -0.0f, 42.5, true, "😀")
        assertEquals(
            GuavaHashing.murmur3_128().hashObject(record, guavaRecordFunnel).toString(),
            Hashing.murmur3_128().hashObject(record, guavaKtRecordFunnel).toString(),
        )
    }

    private data class PrimitiveRecord(
        val short: Short,
        val char: Char,
        val float: Float,
        val double: Double,
        val enabled: Boolean,
        val text: String,
    )

    private val guavaRecordFunnel = object : GuavaFunnel<PrimitiveRecord> {
        override fun funnel(from: PrimitiveRecord, into: GuavaPrimitiveSink) {
            into.putShort(from.short)
                .putChar(from.char)
                .putFloat(from.float)
                .putDouble(from.double)
                .putBoolean(from.enabled)
                .putString(from.text, StandardCharsets.UTF_8)
        }
    }

    private val guavaKtRecordFunnel = GuavaKtFunnel<PrimitiveRecord> { from, into ->
        into.putShort(from.short)
            .putChar(from.char)
            .putFloat(from.float)
            .putDouble(from.double)
            .putBoolean(from.enabled)
            .putString(from.text)
    }
}
