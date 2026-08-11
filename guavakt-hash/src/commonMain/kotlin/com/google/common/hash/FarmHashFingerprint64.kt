package dev.guavakt.hash

/** Guava FarmHashFingerprint64 — 64-bit fingerprint port. */
class FarmHashFingerprint64 : HashFunction {
    override fun bits(): Int = 64
    override fun hashInt(input: Int): HashCode = hashBytes(intToBytes(input))
    override fun hashLong(input: Long): HashCode = hashBytes(longToBytes(input))
    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode =
        HashCode.fromLong(fingerprint(input, off, len))
    override fun hashUnencodedChars(input: CharSequence): HashCode {
        val bytes = ByteArray(input.length * 2)
        for (i in input.indices) {
            val c = input[i].code
            bytes[i * 2] = c.toByte()
            bytes[i * 2 + 1] = (c ushr 8).toByte()
        }
        return hashBytes(bytes)
    }
    override fun hashString(input: CharSequence, charsetName: String): HashCode =
        hashBytes(encodeString(input, charsetName))
    override fun newHasher(): Hasher = object : Hasher {
        private val data = ArrayList<Byte>()
        override fun putByte(b: Byte): Hasher = apply { data.add(b) }
        override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher = apply {
            for (i in off until off + len) data.add(bytes[i])
        }
        override fun putInt(i: Int): Hasher = putBytes(intToBytes(i), 0, 4)
        override fun putLong(l: Long): Hasher = putBytes(longToBytes(l), 0, 8)
        override fun putUnencodedChars(chars: CharSequence): Hasher {
            for (c in chars) {
                putByte(c.code.toByte())
                putByte((c.code ushr 8).toByte())
            }
            return this
        }
        override fun hash(): HashCode = hashBytes(data.toByteArray())
    }
}

private const val K0 = -0x3c5a37a36834ced9L
private const val K1 = -0x4b6d499041670d8dL
private const val K2 = -0x651e95c4d06fbfb1L

private fun fingerprint(bytes: ByteArray, offset: Int, length: Int): Long = when {
    length <= 16 -> hash0to16(bytes, offset, length)
    length <= 32 -> hash17to32(bytes, offset, length)
    length <= 64 -> hash33to64(bytes, offset, length)
    else -> hash65Plus(bytes, offset, length)
}

private fun hash65Plus(bytes: ByteArray, initialOffset: Int, length: Int): Long {
    var offset = initialOffset
    var x = 81L
    var y = 81L * K1 + 113L
    var z = shiftMix(y * K2 + 113L) * K2
    val v = LongArray(2)
    val w = LongArray(2)
    x = x * K2 + fetch64(bytes, offset)
    val end = offset + ((length - 1) / 64) * 64
    val last64Offset = end + ((length - 1) and 63) - 63
    do {
        x = rotate64(x + y + v[0] + fetch64(bytes, offset + 8), 37) * K1
        y = rotate64(y + v[1] + fetch64(bytes, offset + 48), 42) * K1
        x = x xor w[1]
        y += v[0] + fetch64(bytes, offset + 40)
        z = rotate64(z + w[0], 33) * K1
        weakHash32(bytes, offset, v[1] * K1, x + w[0], v)
        weakHash32(bytes, offset + 32, z + w[1], y + fetch64(bytes, offset + 16), w)
        val swap = x
        x = z
        z = swap
        offset += 64
    } while (offset != end)
    val mul = K1 + ((z and 0xffL) shl 1)
    offset = last64Offset
    w[0] += ((length - 1) and 63).toLong()
    v[0] += w[0]
    w[0] += v[0]
    x = rotate64(x + y + v[0] + fetch64(bytes, offset + 8), 37) * mul
    y = rotate64(y + v[1] + fetch64(bytes, offset + 48), 42) * mul
    x = x xor (w[1] * 9)
    y += v[0] * 9 + fetch64(bytes, offset + 40)
    z = rotate64(z + w[0], 33) * mul
    weakHash32(bytes, offset, v[1] * mul, x + w[0], v)
    weakHash32(bytes, offset + 32, z + w[1], y + fetch64(bytes, offset + 16), w)
    return hashLen16(
        hashLen16(v[0], w[0], mul) + shiftMix(y) * K0 + x,
        hashLen16(v[1], w[1], mul) + z,
        mul,
    )
}

private fun weakHash32(bytes: ByteArray, offset: Int, seedAValue: Long, seedBValue: Long, out: LongArray) {
    var seedA = seedAValue + fetch64(bytes, offset)
    var seedB = rotate64(seedBValue + seedA + fetch64(bytes, offset + 24), 21)
    val c = seedA
    seedA += fetch64(bytes, offset + 8) + fetch64(bytes, offset + 16)
    seedB += rotate64(seedA, 44)
    out[0] = seedA + fetch64(bytes, offset + 24)
    out[1] = seedB + c
}

private fun hash0to16(s: ByteArray, off: Int, len: Int): Long {
    if (len >= 8) {
        val mul = K2 + len * 2L
        val a = fetch64(s, off) + K2
        val b = fetch64(s, off + len - 8)
        val c = rotate64(b, 37) * mul + a
        val d = (rotate64(a, 25) + b) * mul
        return hashLen16(c, d, mul)
    }
    if (len >= 4) {
        val mul = K2 + len * 2L
        val a = fetch32(s, off).toLong() and 0xffffffffL
        return hashLen16(len + (a shl 3), fetch32(s, off + len - 4).toLong() and 0xffffffffL, mul)
    }
    if (len > 0) {
        val a = s[off].toInt() and 0xff
        val b = s[off + (len shr 1)].toInt() and 0xff
        val c = s[off + len - 1].toInt() and 0xff
        val y = a + (b shl 8)
        val z = len + (c shl 2)
        return shiftMix(y * K2 xor z * K0) * K2
    }
    return K2
}

private fun hash17to32(s: ByteArray, off: Int, len: Int): Long {
    val mul = K2 + len * 2L
    val a = fetch64(s, off) * K1
    val b = fetch64(s, off + 8)
    val c = fetch64(s, off + len - 8) * mul
    val d = fetch64(s, off + len - 16) * K2
    return hashLen16(rotate64(a + b, 43) + rotate64(c, 30) + d, a + rotate64(b + K2, 18) + c, mul)
}

private fun hash33to64(s: ByteArray, off: Int, len: Int): Long {
    val mul = K2 + len * 2L
    val a = fetch64(s, off) * K2
    val b = fetch64(s, off + 8)
    val c = fetch64(s, off + len - 8) * mul
    val d = fetch64(s, off + len - 16) * K2
    val y = rotate64(a + b, 43) + rotate64(c, 30) + d
    val z = hashLen16(y, a + rotate64(b + K2, 18) + c, mul)
    val e = fetch64(s, off + 16) * mul
    val f = fetch64(s, off + 24)
    val g = (y + fetch64(s, off + len - 32)) * mul
    val h = (z + fetch64(s, off + len - 24)) * mul
    return hashLen16(rotate64(e + f, 43) + rotate64(g, 30) + h, e + rotate64(f + a, 18) + g, mul)
}

private fun hashLen16(u: Long, v: Long, mul: Long): Long {
    var a = (u xor v) * mul
    a = a xor (a ushr 47)
    var b = (v xor a) * mul
    b = b xor (b ushr 47)
    return b * mul
}
private fun shiftMix(v: Long): Long = v xor (v ushr 47)
private fun rotate64(v: Long, shift: Int): Long = (v ushr shift) or (v shl (64 - shift))
private fun fetch64(s: ByteArray, off: Int): Long {
    var r = 0L
    for (i in 0 until 8) r = r or ((s[off + i].toLong() and 0xff) shl (i * 8))
    return r
}
private fun fetch32(s: ByteArray, off: Int): Int =
    (s[off].toInt() and 0xff) or ((s[off + 1].toInt() and 0xff) shl 8) or
        ((s[off + 2].toInt() and 0xff) shl 16) or ((s[off + 3].toInt() and 0xff) shl 24)
private fun intToBytes(i: Int) = byteArrayOf(i.toByte(), (i ushr 8).toByte(), (i ushr 16).toByte(), (i ushr 24).toByte())
private fun longToBytes(l: Long): ByteArray {
    val b = ByteArray(8)
    for (i in 0 until 8) b[i] = (l ushr (i * 8)).toByte()
    return b
}
