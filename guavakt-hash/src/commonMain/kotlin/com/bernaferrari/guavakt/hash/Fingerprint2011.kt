package com.bernaferrari.guavakt.hash

/**
 * Geoff Pike's 64-bit Fingerprint2011 algorithm, ported from Guava's public
 * hash-function implementation. It is intentionally a non-streaming function:
 * the algorithm needs both ends of its complete input.
 */
internal object Fingerprint2011 : AbstractNonStreamingHashFunction() {
    private val k0 = 0xa5b85c5e198ed849UL.toLong()
    private val k1 = 0x8d58ac26afe12e47UL.toLong()
    private val k2 = 0xc47b6e9e3a970ed3UL.toLong()
    private val k3 = 0xc6a4a7935bd1e995UL.toLong()

    override fun bits(): Int = 64

    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        require(off >= 0 && len >= 0 && off <= input.size - len) { "Invalid byte-array range." }
        return HashCode.fromLong(fingerprint(input, off, len))
    }

    private fun fingerprint(bytes: ByteArray, offset: Int, length: Int): Long {
        var result = when {
            length <= 32 -> murmurHash64WithSeed(bytes, offset, length, k0 xor k1 xor k2)
            length <= 64 -> hashLength33To64(bytes, offset, length)
            else -> fullFingerprint(bytes, offset, length)
        }
        val first = if (length >= Long.SIZE_BYTES) LittleEndianByteArray.load64(bytes, offset) else k0
        val last = if (length >= Long.SIZE_BYTES + 1) {
            LittleEndianByteArray.load64(bytes, offset + length - Long.SIZE_BYTES)
        } else {
            k0
        }
        result = hash128To64(result + last, first)
        return if (result == 0L || result == 1L) result - 2L else result
    }

    private fun fullFingerprint(bytes: ByteArray, initialOffset: Int, initialLength: Int): Long {
        var offset = initialOffset
        var x = LittleEndianByteArray.load64(bytes, offset)
        var y = LittleEndianByteArray.load64(bytes, offset + initialLength - 16) xor k1
        var z = LittleEndianByteArray.load64(bytes, offset + initialLength - 56) xor k0
        val v = LongArray(2)
        val w = LongArray(2)
        weakHashLength32WithSeeds(bytes, offset + initialLength - 64, initialLength.toLong(), y, v)
        weakHashLength32WithSeeds(bytes, offset + initialLength - 32, initialLength.toLong() * k1, k0, w)
        z += shiftMix(v[1]) * k1
        x = rotateRight(z + x, 39) * k1
        y = rotateRight(y, 33) * k1
        var remaining = (initialLength - 1) and -64
        do {
            x = rotateRight(x + y + v[0] + LittleEndianByteArray.load64(bytes, offset + 16), 37) * k1
            y = rotateRight(y + v[1] + LittleEndianByteArray.load64(bytes, offset + 48), 42) * k1
            x = x xor w[1]
            y = y xor v[0]
            z = rotateRight(z xor w[0], 33)
            weakHashLength32WithSeeds(bytes, offset, v[1] * k1, x + w[0], v)
            weakHashLength32WithSeeds(bytes, offset + 32, z + w[1], y, w)
            val previousZ = z
            z = x
            x = previousZ
            offset += 64
            remaining -= 64
        } while (remaining != 0)
        return hash128To64(
            hash128To64(v[0], w[0]) + shiftMix(y) * k1 + z,
            hash128To64(v[1], w[1]) + x,
        )
    }

    private fun hashLength33To64(bytes: ByteArray, offset: Int, length: Int): Long {
        var z = LittleEndianByteArray.load64(bytes, offset + 24)
        var a = LittleEndianByteArray.load64(bytes, offset) +
            (length + LittleEndianByteArray.load64(bytes, offset + length - 16)) * k0
        var b = rotateRight(a + z, 52)
        var c = rotateRight(a, 37)
        a += LittleEndianByteArray.load64(bytes, offset + 8)
        c += rotateRight(a, 7)
        a += LittleEndianByteArray.load64(bytes, offset + 16)
        val vf = a + z
        val vs = b + rotateRight(a, 31) + c
        a = LittleEndianByteArray.load64(bytes, offset + 16) +
            LittleEndianByteArray.load64(bytes, offset + length - 32)
        z = LittleEndianByteArray.load64(bytes, offset + length - 8)
        b = rotateRight(a + z, 52)
        c = rotateRight(a, 37)
        a += LittleEndianByteArray.load64(bytes, offset + length - 24)
        c += rotateRight(a, 7)
        a += LittleEndianByteArray.load64(bytes, offset + length - 16)
        val wf = a + z
        val ws = b + rotateRight(a, 31) + c
        val result = shiftMix((vf + ws) * k2 + (wf + vs) * k0)
        return shiftMix(result * k0 + vs) * k2
    }

    private fun murmurHash64WithSeed(bytes: ByteArray, offset: Int, length: Int, seed: Long): Long {
        val lengthAligned = length and -Long.SIZE_BYTES
        val lengthRemainder = length and (Long.SIZE_BYTES - 1)
        var hash = seed xor (length.toLong() * k3)
        for (index in 0 until lengthAligned step Long.SIZE_BYTES) {
            val loaded = LittleEndianByteArray.load64(bytes, offset + index)
            val data = shiftMix(loaded * k3) * k3
            hash = (hash xor data) * k3
        }
        if (lengthRemainder != 0) {
            val data = LittleEndianByteArray.load64Safely(bytes, offset + lengthAligned, lengthRemainder)
            hash = (hash xor data) * k3
        }
        hash = shiftMix(hash) * k3
        return shiftMix(hash)
    }

    private fun weakHashLength32WithSeeds(
        bytes: ByteArray,
        offset: Int,
        initialSeedA: Long,
        initialSeedB: Long,
        output: LongArray,
    ) {
        val part1 = LittleEndianByteArray.load64(bytes, offset)
        val part2 = LittleEndianByteArray.load64(bytes, offset + 8)
        val part3 = LittleEndianByteArray.load64(bytes, offset + 16)
        val part4 = LittleEndianByteArray.load64(bytes, offset + 24)
        var seedA = initialSeedA + part1
        var seedB = rotateRight(initialSeedB + seedA + part4, 51)
        val c = seedA
        seedA += part2
        seedA += part3
        seedB += rotateRight(seedA, 23)
        output[0] = seedA + part4
        output[1] = seedB + c
    }

    private fun hash128To64(high: Long, low: Long): Long {
        var first = (low xor high) * k3
        first = first xor (first ushr 47)
        var second = (high xor first) * k3
        second = second xor (second ushr 47)
        return second * k3
    }

    private fun shiftMix(value: Long): Long = value xor (value ushr 47)
    private fun rotateRight(value: Long, distance: Int): Long =
        (value ushr distance) or (value shl (Long.SIZE_BITS - distance))
}
