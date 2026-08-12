package com.bernaferrari.guavakt.hash

/**
 * Guava SipHashFunction — SipHash-2-4 producing 64-bit codes.
 */
class SipHashFunction(
    private val k0: Long = 0x0706050403020100L,
    private val k1: Long = 0x0f0e0d0c0b0a0908L,
) : HashFunction {
    override fun bits(): Int = 64

    override fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode {
        val s = longArrayOf(
            0x736f6d6570736575L xor k0,
            0x646f72616e646f6dL xor k1,
            0x6c7967656e657261L xor k0,
            0x7465646279746573L xor k1,
        )
        val nblocks = len / 8
        for (i in 0 until nblocks) {
            val m = load64(input, off + i * 8)
            s[3] = s[3] xor m
            repeat(2) { sipRound(s) }
            s[0] = s[0] xor m
        }
        var b = (len.toLong() and 0xffL) shl 56
        val tail = off + nblocks * 8
        for (i in 0 until (len and 7)) {
            b = b or ((input[tail + i].toLong() and 0xffL) shl (8 * i))
        }
        s[3] = s[3] xor b
        repeat(2) { sipRound(s) }
        s[0] = s[0] xor b
        s[2] = s[2] xor 0xffL
        repeat(4) { sipRound(s) }
        return HashCode.fromLong(s[0] xor s[1] xor s[2] xor s[3])
    }

    override fun hashInt(input: Int): HashCode = hashBytes(
        byteArrayOf(input.toByte(), (input ushr 8).toByte(), (input ushr 16).toByte(), (input ushr 24).toByte())
    )
    override fun hashLong(input: Long): HashCode =
        hashBytes(ByteArray(8) { (input ushr (it * 8)).toByte() })
    override fun hashUnencodedChars(input: CharSequence): HashCode {
        val bytes = ByteArray(input.length * 2)
        for (i in input.indices) {
            val c = input[i].code
            bytes[i * 2] = c.toByte(); bytes[i * 2 + 1] = (c ushr 8).toByte()
        }
        return hashBytes(bytes)
    }
    override fun hashString(input: CharSequence, charsetName: String): HashCode =
        hashBytes(encodeString(input, charsetName))
    /** Incremental SipHash-2-4 state; only the current 8-byte block is buffered. */
    override fun newHasher(): Hasher = StreamingHasher()

    private inner class StreamingHasher : AbstractStreamingHasher(bufferSize = Long.SIZE_BYTES) {
        private val state = longArrayOf(
            0x736f6d6570736575L xor k0,
            0x646f72616e646f6dL xor k1,
            0x6c7967656e657261L xor k0,
            0x7465646279746573L xor k1,
        )
        private var length = 0L
        private var finalHash: HashCode? = null

        override fun process(bb: ByteArray, off: Int) {
            if (finalHash != null) return
            length += Long.SIZE_BYTES
            processMessage(load64(bb, off))
        }

        override fun processRemaining(bb: ByteArray, off: Int, len: Int) {
            if (finalHash != null) return
            length += len.toLong()
            var tail = 0L
            for (index in 0 until len) {
                tail = tail or ((bb[off + index].toLong() and 0xffL) shl (index * Byte.SIZE_BITS))
            }
            processMessage(tail xor (length shl 56))
        }

        override fun makeHash(): HashCode = finalHash ?: run {
            state[2] = state[2] xor 0xffL
            repeat(4) { sipRound(state) }
            HashCode.fromLong(state[0] xor state[1] xor state[2] xor state[3]).also { finalHash = it }
        }

        private fun processMessage(message: Long) {
            state[3] = state[3] xor message
            repeat(2) { sipRound(state) }
            state[0] = state[0] xor message
        }
    }

    companion object {
        private fun load64(input: ByteArray, offset: Int): Long {
            var r = 0L
            for (i in 0 until 8) r = r or ((input[offset + i].toLong() and 0xffL) shl (i * 8))
            return r
        }
        private fun rotl(x: Long, b: Int): Long = (x shl b) or (x ushr (64 - b))
        private fun sipRound(s: LongArray) {
            s[0] += s[1]; s[1] = rotl(s[1], 13); s[1] = s[1] xor s[0]; s[0] = rotl(s[0], 32)
            s[2] += s[3]; s[3] = rotl(s[3], 16); s[3] = s[3] xor s[2]
            s[0] += s[3]; s[3] = rotl(s[3], 21); s[3] = s[3] xor s[0]
            s[2] += s[1]; s[1] = rotl(s[1], 17); s[1] = s[1] xor s[2]; s[2] = rotl(s[2], 32)
        }
    }
}
