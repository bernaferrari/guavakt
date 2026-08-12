package com.bernaferrari.guavakt.hash

class Murmur3_32HashFunction(private val seed: Int = 0) : HashFunction {
    override fun bits(): Int = 32
    override fun hashInt(input: Int): HashCode = HashCode.fromInt(fmix(mixH1(seed, mixK1(input)), 4))
    override fun hashLong(input: Long): HashCode {
        var h1 = mixH1(seed, mixK1(input.toInt()))
        h1 = mixH1(h1, mixK1((input ushr 32).toInt()))
        return HashCode.fromInt(fmix(h1, 8))
    }
    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        var h1 = seed
        val nblocks = len / 4
        for (i in 0 until nblocks) {
            val index = off + i * 4
            val k1 = (input[index].toInt() and 0xff) or ((input[index + 1].toInt() and 0xff) shl 8) or
                ((input[index + 2].toInt() and 0xff) shl 16) or ((input[index + 3].toInt() and 0xff) shl 24)
            h1 = mixH1(h1, mixK1(k1))
        }
        val tail = off + nblocks * 4
        var k1 = 0
        when (len and 3) {
            3 -> { k1 = k1 xor ((input[tail + 2].toInt() and 0xff) shl 16); k1 = k1 xor ((input[tail + 1].toInt() and 0xff) shl 8); k1 = k1 xor (input[tail].toInt() and 0xff); h1 = h1 xor mixK1(k1) }
            2 -> { k1 = k1 xor ((input[tail + 1].toInt() and 0xff) shl 8); k1 = k1 xor (input[tail].toInt() and 0xff); h1 = h1 xor mixK1(k1) }
            1 -> { k1 = k1 xor (input[tail].toInt() and 0xff); h1 = h1 xor mixK1(k1) }
        }
        return HashCode.fromInt(fmix(h1, len))
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
    /** Constant-memory incremental Murmur3 hasher; no input bytes are retained after a block. */
    override fun newHasher(): Hasher = StreamingHasher()

    private inner class StreamingHasher : AbstractStreamingHasher(bufferSize = 4) {
        private var h1 = seed
        private var length = 0
        private var finalHash: HashCode? = null

        override fun process(bb: ByteArray, off: Int) {
            if (finalHash != null) return
            val block = (bb[off].toInt() and 0xff) or
                ((bb[off + 1].toInt() and 0xff) shl 8) or
                ((bb[off + 2].toInt() and 0xff) shl 16) or
                ((bb[off + 3].toInt() and 0xff) shl 24)
            h1 = mixH1(h1, mixK1(block))
            length += Int.SIZE_BYTES
        }

        override fun processRemaining(bb: ByteArray, off: Int, len: Int) {
            if (finalHash != null) return
            length += len
            var tail = 0
            for (index in 0 until len) {
                tail = tail or ((bb[off + index].toInt() and 0xff) shl (index * Byte.SIZE_BITS))
            }
            if (len != 0) h1 = h1 xor mixK1(tail)
        }

        override fun makeHash(): HashCode = finalHash ?: HashCode.fromInt(fmix(h1, length)).also {
            finalHash = it
        }
    }
    private fun mixK1(k1In: Int): Int {
        var k1 = k1In * C1
        k1 = (k1 shl 15) or (k1 ushr 17)
        k1 *= C2
        return k1
    }
    private fun mixH1(h1In: Int, k1: Int): Int {
        var h1 = h1In xor k1
        h1 = (h1 shl 13) or (h1 ushr 19)
        h1 = h1 * 5 + 0xe6546b64.toInt()
        return h1
    }
    private fun fmix(h1In: Int, length: Int): Int {
        var h1 = h1In xor length
        h1 = h1 xor (h1 ushr 16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor (h1 ushr 16)
        return h1
    }
    companion object {
        private const val C1 = -0x3361d2af
        private const val C2 = 0x1b873593
    }
}
