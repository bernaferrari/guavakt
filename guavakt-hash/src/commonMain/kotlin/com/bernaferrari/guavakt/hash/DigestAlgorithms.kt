package com.bernaferrari.guavakt.hash

/**
 * Pure-Kotlin cryptographic digests (MD5, SHA-1, SHA-256, SHA-512) for Guava MessageDigestHashFunction on KMP.
 * Algorithms follow FIPS / RFC specs (not FNV stand-ins).
 */
internal object DigestAlgorithms {
    fun md5(input: ByteArray, off: Int, len: Int): ByteArray = Md5.hash(input, off, len)
    fun sha1(input: ByteArray, off: Int, len: Int): ByteArray = Sha1.hash(input, off, len)
    fun sha256(input: ByteArray, off: Int, len: Int): ByteArray = Sha256.hash(input, off, len)
    fun sha256Hasher(): Hasher = Sha256.newHasher()
    fun sha384(input: ByteArray, off: Int, len: Int): ByteArray = Sha512.hash384(input, off, len)
    fun sha384Hasher(): Hasher = Sha512.newHasher384()
    fun sha512(input: ByteArray, off: Int, len: Int): ByteArray = Sha512.hash(input, off, len)
    fun sha512Hasher(): Hasher = Sha512.newHasher()

    /** Returns a fixed-memory HMAC hasher for the portable SHA-2 variants. */
    fun hmacHasher(key: ByteArray, algorithm: String): Hasher? {
        val (newDigest, blockSize) = when {
            algorithm.contains("256") -> (::sha256Hasher) to 64
            algorithm.contains("384") -> (::sha384Hasher) to 128
            algorithm.contains("512") -> (::sha512Hasher) to 128
            else -> return null
        }
        return HmacStreamingHasher(key, newDigest, blockSize)
    }

    fun hmac(key: ByteArray, data: ByteArray, off: Int, len: Int, digest: (ByteArray, Int, Int) -> ByteArray, blockSize: Int): ByteArray {
        var keyUse = key
        if (keyUse.size > blockSize) keyUse = digest(keyUse, 0, keyUse.size)
        val k = ByteArray(blockSize)
        keyUse.copyInto(k, 0, 0, minOf(keyUse.size, blockSize))
        val ipad = ByteArray(blockSize) { (k[it].toInt() xor 0x36).toByte() }
        val opad = ByteArray(blockSize) { (k[it].toInt() xor 0x5c).toByte() }
        val inner = ByteArray(blockSize + len)
        ipad.copyInto(inner, 0)
        data.copyInto(inner, blockSize, off, off + len)
        val innerHash = digest(inner, 0, inner.size)
        val outer = ByteArray(blockSize + innerHash.size)
        opad.copyInto(outer, 0)
        innerHash.copyInto(outer, blockSize)
        return digest(outer, 0, outer.size)
    }

    private class HmacStreamingHasher(
        key: ByteArray,
        private val newDigest: () -> Hasher,
        blockSize: Int,
    ) : AbstractByteHasher() {
        private val inner: Hasher
        private val outerPad: ByteArray
        private var finalHash: HashCode? = null

        init {
            var keyUse = key.copyOf()
            if (keyUse.size > blockSize) keyUse = newDigest().putBytes(keyUse).hash().asBytes()
            val normalizedKey = ByteArray(blockSize)
            keyUse.copyInto(normalizedKey, endIndex = minOf(keyUse.size, normalizedKey.size))
            inner = newDigest()
            inner.putBytes(ByteArray(blockSize) { (normalizedKey[it].toInt() xor 0x36).toByte() })
            outerPad = ByteArray(blockSize) { (normalizedKey[it].toInt() xor 0x5c).toByte() }
        }

        override fun update(b: Byte) {
            check(finalHash == null) { "Cannot add bytes after hash() has been called." }
            inner.putByte(b)
        }

        override fun hash(): HashCode = finalHash ?: run {
            val outer = newDigest()
            outer.putBytes(outerPad)
            outer.putBytes(inner.hash().asBytes())
            outer.hash().also { finalHash = it }
        }
    }
}

// File-private digest engines (not nested — keeps DigestAlgorithms entrypoints small)

private object Md5 {
    private val S = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    )
    private val K = intArrayOf(
        -680876936, -389564586, 606105819, -1044525330, -176418897, 1200080426, -1473231341, -45705983, 1770035416, -1958414417, -42063, -1990404162, 1804603682, -40341101, -1502002290, 1236535329, -165796510, -1069501632, 643717713, -373897302, -701558691, 38016083, -660478335, -405537848, 568446438, -1019803690, -187363961, 1163531501, -1444681467, -51403784, 1735328473, -1926607734, -378558, -2022574463, 1839030562, -35309556, -1530992060, 1272893353, -155497632, -1094730640, 681279174, -358537222, -722521979, 76029189, -640364487, -421815835, 530742520, -995338651, -198630844, 1126891415, -1416354905, -57434055, 1700485571, -1894986606, -1051523, -2054922799, 1873313359, -30611744, -1560198380, 1309151649, -145523070, -1120210379, 718787259, -343485551
    )

    fun hash(msg: ByteArray, off: Int, len: Int): ByteArray {
        val bitLen = len.toLong() * 8
        val withOne = len + 1
        val padLen = (56 - (withOne % 64) + 64) % 64
        val total = withOne + padLen + 8
        val data = ByteArray(total)
        msg.copyInto(data, 0, off, off + len)
        data[len] = 0x80.toByte()
        for (i in 0 until 8) data[total - 8 + i] = (bitLen ushr (8 * i)).toByte()
        var a0 = 0x67452301
        var b0 = -0x10325477 // 0xefcdab89
        var c0 = -0x67452302 // 0x98badcfe
        var d0 = 0x10325476
        val M = IntArray(16)
        var offset = 0
        while (offset < total) {
            for (i in 0 until 16) {
                val j = offset + i * 4
                M[i] = (data[j].toInt() and 0xff) or ((data[j + 1].toInt() and 0xff) shl 8) or
                    ((data[j + 2].toInt() and 0xff) shl 16) or ((data[j + 3].toInt() and 0xff) shl 24)
            }
            var A = a0; var B = b0; var C = c0; var D = d0
            for (i in 0 until 64) {
                val (F, g) = when {
                    i < 16 -> (B and C) or (B.inv() and D) to i
                    i < 32 -> (D and B) or (D.inv() and C) to (5 * i + 1) % 16
                    i < 48 -> (B xor C xor D) to (3 * i + 5) % 16
                    else -> (C xor (B or D.inv())) to (7 * i) % 16
                }
                val tmp = D
                D = C
                C = B
                B = B + Integer.rotateLeft(A + F + K[i] + M[g], S[i])
                A = tmp
            }
            a0 += A; b0 += B; c0 += C; d0 += D
            offset += 64
        }
        return leInts(a0, b0, c0, d0)
    }

    private fun leInts(vararg xs: Int): ByteArray {
        val out = ByteArray(xs.size * 4)
        var p = 0
        for (x in xs) {
            out[p++] = x.toByte(); out[p++] = (x ushr 8).toByte()
            out[p++] = (x ushr 16).toByte(); out[p++] = (x ushr 24).toByte()
        }
        return out
    }
}

// Integer.rotateLeft not on KMP — local
private object Integer {
    fun rotateLeft(x: Int, n: Int): Int = (x shl n) or (x ushr (32 - n))
    fun rotateRight(x: Int, n: Int): Int = (x ushr n) or (x shl (32 - n))
}

private object Sha1 {
    fun hash(msg: ByteArray, off: Int, len: Int): ByteArray {
        val bitLen = len.toLong() * 8
        val withOne = len + 1
        val padLen = (56 - (withOne % 64) + 64) % 64
        val total = withOne + padLen + 8
        val data = ByteArray(total)
        msg.copyInto(data, 0, off, off + len)
        data[len] = 0x80.toByte()
        // big-endian length
        for (i in 0 until 8) data[total - 1 - i] = (bitLen ushr (8 * i)).toByte()
        var h0 = 0x67452301
        var h1 = -0x10325477
        var h2 = -0x67452302
        var h3 = 0x10325476
        var h4 = -0x3c2d1e10 // 0xc3d2e1f0
        val w = IntArray(80)
        var offset = 0
        while (offset < total) {
            for (i in 0 until 16) {
                val j = offset + i * 4
                w[i] = ((data[j].toInt() and 0xff) shl 24) or ((data[j + 1].toInt() and 0xff) shl 16) or
                    ((data[j + 2].toInt() and 0xff) shl 8) or (data[j + 3].toInt() and 0xff)
            }
            for (i in 16 until 80) {
                w[i] = Integer.rotateLeft(w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16], 1)
            }
            var a = h0; var b = h1; var c = h2; var d = h3; var e = h4
            for (i in 0 until 80) {
                val (f, k) = when {
                    i < 20 -> ((b and c) or (b.inv() and d)) to 0x5a827999
                    i < 40 -> (b xor c xor d) to 0x6ed9eba1
                    i < 60 -> ((b and c) or (b and d) or (c and d)) to -0x70e44324 // 0x8f1bbcdc
                    else -> (b xor c xor d) to -0x359d3e2a // 0xca62c1d6
                }
                val temp = Integer.rotateLeft(a, 5) + f + e + k + w[i]
                e = d; d = c; c = Integer.rotateLeft(b, 30); b = a; a = temp
            }
            h0 += a; h1 += b; h2 += c; h3 += d; h4 += e
            offset += 64
        }
        return beInts(h0, h1, h2, h3, h4)
    }

    private fun beInts(vararg xs: Int): ByteArray {
        val out = ByteArray(xs.size * 4)
        var p = 0
        for (x in xs) {
            out[p++] = (x ushr 24).toByte(); out[p++] = (x ushr 16).toByte()
            out[p++] = (x ushr 8).toByte(); out[p++] = x.toByte()
        }
        return out
    }
}

private object Sha256 {
    private val K = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )
    private val H0 = intArrayOf(
        0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6,
        0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19,
    )

    fun newHasher(): Hasher = StreamingHasher()

    fun hash(msg: ByteArray, off: Int, len: Int): ByteArray {
        val bitLen = len.toLong() * 8
        val withOne = len + 1
        val padLen = (56 - (withOne % 64) + 64) % 64
        val total = withOne + padLen + 8
        val data = ByteArray(total)
        msg.copyInto(data, 0, off, off + len)
        data[len] = 0x80.toByte()
        for (i in 0 until 8) data[total - 1 - i] = (bitLen ushr (8 * i)).toByte()
        val h = H0.copyOf()
        val w = IntArray(64)
        var offset = 0
        while (offset < total) {
            for (i in 0 until 16) {
                val j = offset + i * 4
                w[i] = ((data[j].toInt() and 0xff) shl 24) or ((data[j + 1].toInt() and 0xff) shl 16) or
                    ((data[j + 2].toInt() and 0xff) shl 8) or (data[j + 3].toInt() and 0xff)
            }
            for (i in 16 until 64) {
                val s0 = Integer.rotateRight(w[i - 15], 7) xor Integer.rotateRight(w[i - 15], 18) xor (w[i - 15] ushr 3)
                val s1 = Integer.rotateRight(w[i - 2], 17) xor Integer.rotateRight(w[i - 2], 19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (i in 0 until 64) {
                val S1 = Integer.rotateRight(e, 6) xor Integer.rotateRight(e, 11) xor Integer.rotateRight(e, 25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + S1 + ch + K[i] + w[i]
                val S0 = Integer.rotateRight(a, 2) xor Integer.rotateRight(a, 13) xor Integer.rotateRight(a, 22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = S0 + maj
                hh = g; g = f; f = e; e = d + temp1; d = c; c = b; b = a; a = temp1 + temp2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
            offset += 64
        }
        val out = ByteArray(32)
        var p = 0
        for (x in h) {
            out[p++] = (x ushr 24).toByte(); out[p++] = (x ushr 16).toByte()
            out[p++] = (x ushr 8).toByte(); out[p++] = x.toByte()
        }
        return out
    }

    private class StreamingHasher : AbstractByteHasher() {
        private val state = H0.copyOf()
        private val block = ByteArray(BLOCK_BYTES)
        private var blockLength = 0
        private var byteCount = 0L
        private var finalHash: HashCode? = null

        override fun update(b: Byte) {
            check(finalHash == null) { "Cannot add bytes after hash() has been called." }
            block[blockLength++] = b
            byteCount++
            if (blockLength == BLOCK_BYTES) {
                processBlock(block)
                blockLength = 0
            }
        }

        override fun hash(): HashCode = finalHash ?: run {
            val bitCount = byteCount shl 3
            block[blockLength++] = 0x80.toByte()
            if (blockLength > LENGTH_OFFSET) {
                while (blockLength < BLOCK_BYTES) block[blockLength++] = 0
                processBlock(block)
                blockLength = 0
            }
            while (blockLength < LENGTH_OFFSET) block[blockLength++] = 0
            for (shift in 56 downTo 0 step Byte.SIZE_BITS) {
                block[blockLength++] = (bitCount ushr shift).toByte()
            }
            processBlock(block)
            blockLength = 0
            val digest = ByteArray(DIGEST_BYTES)
            var position = 0
            for (word in state) {
                digest[position++] = (word ushr 24).toByte()
                digest[position++] = (word ushr 16).toByte()
                digest[position++] = (word ushr 8).toByte()
                digest[position++] = word.toByte()
            }
            HashCode.fromBytes(digest).also { finalHash = it }
        }

        private fun processBlock(bytes: ByteArray) {
            val words = IntArray(64)
            for (index in 0 until 16) {
                val offset = index * Int.SIZE_BYTES
                words[index] = ((bytes[offset].toInt() and 0xff) shl 24) or
                    ((bytes[offset + 1].toInt() and 0xff) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xff) shl 8) or
                    (bytes[offset + 3].toInt() and 0xff)
            }
            for (index in 16 until 64) {
                val s0 = Integer.rotateRight(words[index - 15], 7) xor
                    Integer.rotateRight(words[index - 15], 18) xor (words[index - 15] ushr 3)
                val s1 = Integer.rotateRight(words[index - 2], 17) xor
                    Integer.rotateRight(words[index - 2], 19) xor (words[index - 2] ushr 10)
                words[index] = words[index - 16] + s0 + words[index - 7] + s1
            }
            var a = state[0]; var b = state[1]; var c = state[2]; var d = state[3]
            var e = state[4]; var f = state[5]; var g = state[6]; var h = state[7]
            for (index in 0 until 64) {
                val sigma1 = Integer.rotateRight(e, 6) xor Integer.rotateRight(e, 11) xor Integer.rotateRight(e, 25)
                val choose = (e and f) xor (e.inv() and g)
                val temporary1 = h + sigma1 + choose + K[index] + words[index]
                val sigma0 = Integer.rotateRight(a, 2) xor Integer.rotateRight(a, 13) xor Integer.rotateRight(a, 22)
                val majority = (a and b) xor (a and c) xor (b and c)
                val temporary2 = sigma0 + majority
                h = g; g = f; f = e; e = d + temporary1
                d = c; c = b; b = a; a = temporary1 + temporary2
            }
            state[0] += a; state[1] += b; state[2] += c; state[3] += d
            state[4] += e; state[5] += f; state[6] += g; state[7] += h
        }

        private companion object {
            const val BLOCK_BYTES = 64
            const val LENGTH_OFFSET = 56
            const val DIGEST_BYTES = 32
        }
    }
}
private object Sha512 {
    private val K = longArrayOf(
        4794697086780616226L,
        8158064640168781261L,
        -5349999486874862801L,
        -1606136188198331460L,
        4131703408338449720L,
        6480981068601479193L,
        -7908458776815382629L,
        -6116909921290321640L,
        -2880145864133508542L,
        1334009975649890238L,
        2608012711638119052L,
        6128411473006802146L,
        8268148722764581231L,
        -9160688886553864527L,
        -7215885187991268811L,
        -4495734319001033068L,
        -1973867731355612462L,
        -1171420211273849373L,
        1135362057144423861L,
        2597628984639134821L,
        3308224258029322869L,
        5365058923640841347L,
        6679025012923562964L,
        8573033837759648693L,
        -7476448914759557205L,
        -6327057829258317296L,
        -5763719355590565569L,
        -4658551843659510044L,
        -4116276920077217854L,
        -3051310485924567259L,
        489312712824947311L,
        1452737877330783856L,
        2861767655752347644L,
        3322285676063803686L,
        5560940570517711597L,
        5996557281743188959L,
        7280758554555802590L,
        8532644243296465576L,
        -9096487096722542874L,
        -7894198246740708037L,
        -6719396339535248540L,
        -6333637450476146687L,
        -4446306890439682159L,
        -4076793802049405392L,
        -3345356375505022440L,
        -2983346525034927856L,
        -860691631967231958L,
        1182934255886127544L,
        1847814050463011016L,
        2177327727835720531L,
        2830643537854262169L,
        3796741975233480872L,
        4115178125766777443L,
        5681478168544905931L,
        6601373596472566643L,
        7507060721942968483L,
        8399075790359081724L,
        8693463985226723168L,
        -8878714635349349518L,
        -8302665154208450068L,
        -8016688836872298968L,
        -6606660893046293015L,
        -4685533653050689259L,
        -4147400797238176981L,
        -3880063495543823972L,
        -3348786107499101689L,
        -1523767162380948706L,
        -757361751448694408L,
        500013540394364858L,
        748580250866718886L,
        1242879168328830382L,
        1977374033974150939L,
        2944078676154940804L,
        3659926193048069267L,
        4368137639120453308L,
        4836135668995329356L,
        5532061633213252278L,
        6448918945643986474L,
        6902733635092675308L,
        7801388544844847127L
    )
    private val H0 = longArrayOf(7640891576956012808L, -4942790177534073029L, 4354685564936845355L, -6534734903238641935L, 5840696475078001361L, -7276294671716946913L, 2270897969802886507L, 6620516959819538809L)
    private val H384 = longArrayOf(
        0xcbbb9d5dc1059ed8UL.toLong(), 0x629a292a367cd507UL.toLong(),
        0x9159015a3070dd17UL.toLong(), 0x152fecd8f70e5939UL.toLong(),
        0x67332667ffc00b31UL.toLong(), 0x8eb44a8768581511UL.toLong(),
        0xdb0c2e0d64f98fa7UL.toLong(), 0x47b5481dbefa4fa4UL.toLong(),
    )

    private fun rotr(x: Long, n: Int): Long = (x ushr n) or (x shl (64 - n))

    fun hash(msg: ByteArray, off: Int, len: Int): ByteArray = hash(msg, off, len, H0, outputWords = 8)

    fun hash384(msg: ByteArray, off: Int, len: Int): ByteArray = hash(msg, off, len, H384, outputWords = 6)

    fun newHasher(): Hasher = StreamingHasher(H0, outputWords = 8)

    fun newHasher384(): Hasher = StreamingHasher(H384, outputWords = 6)

    private fun hash(
        msg: ByteArray,
        off: Int,
        len: Int,
        initialHash: LongArray,
        outputWords: Int,
    ): ByteArray {
        val bitLen = len.toLong() * 8L
        val withOne = len + 1
        val padLen = (112 - (withOne % 128) + 128) % 128
        val total = withOne + padLen + 16
        val data = ByteArray(total)
        msg.copyInto(data, 0, off, off + len)
        data[len] = 0x80.toByte()
        // length in bits as 128-bit BE; high 64 = 0 for len < 2^61 bytes
        for (i in 0 until 8) {
            data[total - 1 - i] = (bitLen ushr (8 * i)).toByte()
        }
        val h = initialHash.copyOf()
        val w = LongArray(80)
        var offset = 0
        while (offset < total) {
            for (i in 0 until 16) {
                var v = 0L
                val base = offset + i * 8
                for (b in 0 until 8) {
                    v = (v shl 8) or (data[base + b].toLong() and 0xffL)
                }
                w[i] = v
            }
            for (i in 16 until 80) {
                val s0 = rotr(w[i - 15], 1) xor rotr(w[i - 15], 8) xor (w[i - 15] ushr 7)
                val s1 = rotr(w[i - 2], 19) xor rotr(w[i - 2], 61) xor (w[i - 2] ushr 6)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (i in 0 until 80) {
                val S1 = rotr(e, 14) xor rotr(e, 18) xor rotr(e, 41)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + S1 + ch + K[i] + w[i]
                val S0 = rotr(a, 28) xor rotr(a, 34) xor rotr(a, 39)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = S0 + maj
                hh = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d
            h[4] += e; h[5] += f; h[6] += g; h[7] += hh
            offset += 128
        }
        val out = ByteArray(outputWords * Long.SIZE_BYTES)
        var p = 0
        for (wordIndex in 0 until outputWords) {
            val x = h[wordIndex]
            for (shift in 56 downTo 0 step 8) {
                out[p++] = (x ushr shift).toByte()
            }
        }
        return out
    }

    private class StreamingHasher(initialHash: LongArray, private val outputWords: Int) : AbstractByteHasher() {
        private val state = initialHash.copyOf()
        private val block = ByteArray(BLOCK_BYTES)
        private var blockLength = 0
        private var byteCount = 0L
        private var finalHash: HashCode? = null

        override fun update(b: Byte) {
            check(finalHash == null) { "Cannot add bytes after hash() has been called." }
            block[blockLength++] = b
            byteCount++
            if (blockLength == BLOCK_BYTES) {
                processBlock(block)
                blockLength = 0
            }
        }

        override fun hash(): HashCode = finalHash ?: run {
            val bitCount = byteCount shl 3
            block[blockLength++] = 0x80.toByte()
            if (blockLength > LENGTH_OFFSET) {
                while (blockLength < BLOCK_BYTES) block[blockLength++] = 0
                processBlock(block)
                blockLength = 0
            }
            while (blockLength < LENGTH_OFFSET) block[blockLength++] = 0
            // The high 64 length bits are zero until the input reaches 2^61 bytes.
            repeat(Long.SIZE_BYTES) { block[blockLength++] = 0 }
            for (shift in 56 downTo 0 step Byte.SIZE_BITS) {
                block[blockLength++] = (bitCount ushr shift).toByte()
            }
            processBlock(block)
            blockLength = 0
            val digest = ByteArray(outputWords * Long.SIZE_BYTES)
            var position = 0
            for (index in 0 until outputWords) {
                for (shift in 56 downTo 0 step Byte.SIZE_BITS) {
                    digest[position++] = (state[index] ushr shift).toByte()
                }
            }
            HashCode.fromBytes(digest).also { finalHash = it }
        }

        private fun processBlock(bytes: ByteArray) {
            val words = LongArray(80)
            for (index in 0 until 16) {
                var word = 0L
                val offset = index * Long.SIZE_BYTES
                for (byteIndex in 0 until Long.SIZE_BYTES) {
                    word = (word shl Byte.SIZE_BITS) or (bytes[offset + byteIndex].toLong() and 0xffL)
                }
                words[index] = word
            }
            for (index in 16 until 80) {
                val s0 = rotr(words[index - 15], 1) xor rotr(words[index - 15], 8) xor (words[index - 15] ushr 7)
                val s1 = rotr(words[index - 2], 19) xor rotr(words[index - 2], 61) xor (words[index - 2] ushr 6)
                words[index] = words[index - 16] + s0 + words[index - 7] + s1
            }
            var a = state[0]; var b = state[1]; var c = state[2]; var d = state[3]
            var e = state[4]; var f = state[5]; var g = state[6]; var h = state[7]
            for (index in 0 until 80) {
                val sigma1 = rotr(e, 14) xor rotr(e, 18) xor rotr(e, 41)
                val choose = (e and f) xor (e.inv() and g)
                val temporary1 = h + sigma1 + choose + K[index] + words[index]
                val sigma0 = rotr(a, 28) xor rotr(a, 34) xor rotr(a, 39)
                val majority = (a and b) xor (a and c) xor (b and c)
                val temporary2 = sigma0 + majority
                h = g; g = f; f = e; e = d + temporary1
                d = c; c = b; b = a; a = temporary1 + temporary2
            }
            state[0] += a; state[1] += b; state[2] += c; state[3] += d
            state[4] += e; state[5] += f; state[6] += g; state[7] += h
        }

        private companion object {
            const val BLOCK_BYTES = 128
            const val LENGTH_OFFSET = 112
        }
    }
}
