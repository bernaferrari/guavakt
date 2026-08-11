package dev.guavakt.hash

/**
 * Guava Murmur3_128HashFunction — 128-bit MurmurHash3 x64.
 */
class Murmur3_128HashFunction(private val seed: Int = 0) : HashFunction {
    override fun bits(): Int = 128

    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        // Guava widens the signed Int seed directly to Long for the x64 variant.
        // Keeping that sign extension matters for every negative seed.
        var h1 = seed.toLong()
        var h2 = h1
        val nblocks = len / 16
        for (i in 0 until nblocks) {
            val index = off + i * 16
            var k1 = load64(input, index)
            var k2 = load64(input, index + 8)
            k1 *= C1; k1 = rotl(k1, 31); k1 *= C2; h1 = h1 xor k1
            h1 = rotl(h1, 27); h1 += h2; h1 = h1 * 5 + 0x52dce729L
            k2 *= C2; k2 = rotl(k2, 33); k2 *= C1; h2 = h2 xor k2
            h2 = rotl(h2, 31); h2 += h1; h2 = h2 * 5 + 0x38495ab5L
        }
        val tailStart = off + nblocks * 16
        val tailLen = len and 15
        var k1 = 0L
        var k2 = 0L
        if (tailLen > 8) {
            for (i in 0 until tailLen - 8) {
                k2 = k2 or ((input[tailStart + 8 + i].toLong() and 0xffL) shl (8 * i))
            }
            k2 *= C2; k2 = rotl(k2, 33); k2 *= C1; h2 = h2 xor k2
            for (i in 0 until 8) {
                k1 = k1 or ((input[tailStart + i].toLong() and 0xffL) shl (8 * i))
            }
            k1 *= C1; k1 = rotl(k1, 31); k1 *= C2; h1 = h1 xor k1
        } else if (tailLen > 0) {
            for (i in 0 until tailLen) {
                k1 = k1 or ((input[tailStart + i].toLong() and 0xffL) shl (8 * i))
            }
            k1 *= C1; k1 = rotl(k1, 31); k1 *= C2; h1 = h1 xor k1
        }
        h1 = h1 xor len.toLong()
        h2 = h2 xor len.toLong()
        h1 += h2
        h2 += h1
        h1 = fmix64(h1)
        h2 = fmix64(h2)
        h1 += h2
        h2 += h1
        return toHashCode128(h1, h2)
    }

    override fun hashInt(input: Int): HashCode = hashBytes(
        byteArrayOf(input.toByte(), (input ushr 8).toByte(), (input ushr 16).toByte(), (input ushr 24).toByte())
    )
    override fun hashLong(input: Long): HashCode {
        val b = ByteArray(8) { (input ushr (it * 8)).toByte() }
        return hashBytes(b)
    }
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
    /** Constant-memory incremental Murmur3 x64 hasher; only one 16-byte block is buffered. */
    override fun newHasher(): Hasher = StreamingHasher()

    private inner class StreamingHasher : AbstractStreamingHasher(bufferSize = 16) {
        private var h1 = seed.toLong()
        private var h2 = h1
        // Guava tracks this as an Int, so retain its intentional two's-complement
        // wraparound semantics for exceptionally large incremental inputs.
        private var length = 0
        private var finalHash: HashCode? = null

        override fun process(bb: ByteArray, off: Int) {
            if (finalHash != null) return
            mixBlock(load64(bb, off), load64(bb, off + Long.SIZE_BYTES))
            length += 16
        }

        override fun processRemaining(bb: ByteArray, off: Int, len: Int) {
            if (finalHash != null) return
            length += len
            var k1 = 0L
            var k2 = 0L
            if (len > Long.SIZE_BYTES) {
                for (index in 0 until len - Long.SIZE_BYTES) {
                    k2 = k2 or ((bb[off + Long.SIZE_BYTES + index].toLong() and 0xffL) shl (index * Byte.SIZE_BITS))
                }
                k2 *= C2
                k2 = rotl(k2, 33)
                k2 *= C1
                h2 = h2 xor k2
            }
            if (len > 0) {
                for (index in 0 until minOf(len, Long.SIZE_BYTES)) {
                    k1 = k1 or ((bb[off + index].toLong() and 0xffL) shl (index * Byte.SIZE_BITS))
                }
                k1 *= C1
                k1 = rotl(k1, 31)
                k1 *= C2
                h1 = h1 xor k1
            }
        }

        override fun makeHash(): HashCode = finalHash ?: run {
            var first = h1 xor length.toLong()
            var second = h2 xor length.toLong()
            first += second
            second += first
            first = fmix64(first)
            second = fmix64(second)
            first += second
            second += first
            toHashCode128(first, second).also { finalHash = it }
        }

        private fun mixBlock(first: Long, second: Long) {
            var k1 = first
            var k2 = second
            k1 *= C1
            k1 = rotl(k1, 31)
            k1 *= C2
            h1 = h1 xor k1
            h1 = rotl(h1, 27)
            h1 += h2
            h1 = h1 * 5 + 0x52dce729L

            k2 *= C2
            k2 = rotl(k2, 33)
            k2 *= C1
            h2 = h2 xor k2
            h2 = rotl(h2, 31)
            h2 += h1
            h2 = h2 * 5 + 0x38495ab5L
        }
    }

    companion object {
        // 0x87c37b91114253d5 as a signed Long.
        private const val C1 = -0x783c846eeebdac2bL
        private const val C2 = 0x4cf5ad432745937fL
        private fun load64(input: ByteArray, offset: Int): Long {
            var r = 0L
            for (i in 0 until 8) r = r or ((input[offset + i].toLong() and 0xffL) shl (i * 8))
            return r
        }
        private fun rotl(x: Long, k: Int): Long = (x shl k) or (x ushr (64 - k))
        private fun fmix64(kIn: Long): Long {
            var k = kIn xor (kIn ushr 33)
            k *= -0xae502812aa7333L
            k = k xor (k ushr 33)
            k *= -0x3b314601e57a13adL
            return k xor (k ushr 33)
        }
        private fun toHashCode128(h1: Long, h2: Long): HashCode {
            val bytes = ByteArray(16)
            for (i in 0 until 8) bytes[i] = (h1 ushr (i * 8)).toByte()
            for (i in 0 until 8) bytes[8 + i] = (h2 ushr (i * 8)).toByte()
            return HashCode.fromBytes(bytes)
        }
    }
}

internal class AccumulatingHasher(private val function: HashFunction) : Hasher {
    private val data = ArrayList<Byte>()
    override fun putByte(b: Byte): Hasher = apply { data.add(b) }
    override fun putBytes(bytes: ByteArray, off: Int, len: Int): Hasher = apply {
        for (i in off until off + len) data.add(bytes[i])
    }
    override fun putInt(i: Int): Hasher =
        putBytes(byteArrayOf(i.toByte(), (i ushr 8).toByte(), (i ushr 16).toByte(), (i ushr 24).toByte()))
    override fun putLong(l: Long): Hasher {
        for (s in 0 until 8) putByte((l ushr (s * 8)).toByte())
        return this
    }
    override fun putUnencodedChars(chars: CharSequence): Hasher {
        for (c in chars) { putByte(c.code.toByte()); putByte((c.code ushr 8).toByte()) }
        return this
    }
    override fun hash(): HashCode = function.hashBytes(data.toByteArray())
}
