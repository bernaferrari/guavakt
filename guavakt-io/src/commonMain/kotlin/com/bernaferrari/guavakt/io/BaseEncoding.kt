package com.bernaferrari.guavakt.io

import com.bernaferrari.guavakt.base.Preconditions

abstract class BaseEncoding {
    abstract fun encode(bytes: ByteArray): String
    abstract fun encode(bytes: ByteArray, off: Int, len: Int): String
    abstract fun decode(chars: CharSequence): ByteArray
    fun omitPadding(): BaseEncoding = this
    fun withPadChar(padChar: Char): BaseEncoding = this
    companion object {
        private val BASE64 = Base64Encoding("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", '=')
        private val BASE64_URL = Base64Encoding("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", '=')
        private val BASE16 = Base16Encoding()
        fun base64(): BaseEncoding = BASE64
        fun base64Url(): BaseEncoding = BASE64_URL
        fun base16(): BaseEncoding = BASE16
    }
}

private class Base64Encoding(private val alphabet: String, private val pad: Char) : BaseEncoding() {
    private val dec = IntArray(128) { -1 }.also { for (i in alphabet.indices) it[alphabet[i].code] = i }
    override fun encode(bytes: ByteArray): String = encode(bytes, 0, bytes.size)
    override fun encode(bytes: ByteArray, off: Int, len: Int): String {
        Preconditions.checkPositionIndexes(off, off + len, bytes.size)
        val out = StringBuilder((len + 2) / 3 * 4)
        var i = off
        val end = off + len
        while (i + 3 <= end) {
            val b0 = bytes[i].toInt() and 0xff
            val b1 = bytes[i + 1].toInt() and 0xff
            val b2 = bytes[i + 2].toInt() and 0xff
            out.append(alphabet[b0 shr 2])
            out.append(alphabet[((b0 and 3) shl 4) or (b1 shr 4)])
            out.append(alphabet[((b1 and 0xf) shl 2) or (b2 shr 6)])
            out.append(alphabet[b2 and 0x3f])
            i += 3
        }
        val rem = end - i
        if (rem == 1) {
            val b0 = bytes[i].toInt() and 0xff
            out.append(alphabet[b0 shr 2])
            out.append(alphabet[(b0 and 3) shl 4])
            out.append(pad).append(pad)
        } else if (rem == 2) {
            val b0 = bytes[i].toInt() and 0xff
            val b1 = bytes[i + 1].toInt() and 0xff
            out.append(alphabet[b0 shr 2])
            out.append(alphabet[((b0 and 3) shl 4) or (b1 shr 4)])
            out.append(alphabet[(b1 and 0xf) shl 2])
            out.append(pad)
        }
        return out.toString()
    }
    override fun decode(chars: CharSequence): ByteArray {
        val s = chars.toString().trim().replace("=", "")
        val out = ArrayList<Byte>()
        var i = 0
        while (i < s.length) {
            val c0 = dec[s[i++].code]
            val c1 = if (i < s.length) dec[s[i++].code] else 0
            val c2 = if (i < s.length) dec[s[i++].code] else -1
            val c3 = if (i < s.length) dec[s[i++].code] else -1
            out.add(((c0 shl 2) or (c1 shr 4)).toByte())
            if (c2 >= 0) out.add((((c1 and 0xf) shl 4) or (c2 shr 2)).toByte())
            if (c3 >= 0) out.add((((c2 and 3) shl 6) or c3).toByte())
        }
        return out.toByteArray()
    }
}

private class Base16Encoding : BaseEncoding() {
    private val hex = "0123456789ABCDEF"
    override fun encode(bytes: ByteArray): String = encode(bytes, 0, bytes.size)
    override fun encode(bytes: ByteArray, off: Int, len: Int): String {
        val out = StringBuilder(len * 2)
        for (i in off until off + len) {
            val v = bytes[i].toInt() and 0xff
            out.append(hex[v ushr 4]); out.append(hex[v and 0xf])
        }
        return out.toString()
    }
    override fun decode(chars: CharSequence): ByteArray {
        require(chars.length % 2 == 0)
        val out = ByteArray(chars.length / 2)
        for (i in out.indices) {
            fun hv(c: Char): Int = when (c) {
                in '0'..'9' -> c - '0'
                in 'A'..'F' -> c - 'A' + 10
                in 'a'..'f' -> c - 'a' + 10
                else -> throw IllegalArgumentException("Invalid hex: $c")
            }
            out[i] = ((hv(chars[i * 2]) shl 4) + hv(chars[i * 2 + 1])).toByte()
        }
        return out
    }
}
