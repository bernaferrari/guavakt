package dev.guavakt.hash

class HashCode private constructor(private val bytes: ByteArray) {
    fun bits(): Int = bytes.size * 8
    fun asInt(): Int {
        check(bytes.size >= 4)
        return (bytes[0].toInt() and 0xff) or
            ((bytes[1].toInt() and 0xff) shl 8) or
            ((bytes[2].toInt() and 0xff) shl 16) or
            ((bytes[3].toInt() and 0xff) shl 24)
    }
    fun asLong(): Long {
        check(bytes.size >= 8)
        var result = 0L
        for (i in 0 until 8) result = result or ((bytes[i].toLong() and 0xff) shl (i * 8))
        return result
    }
    /** Returns this code padded with zero high bytes to 64 bits. */
    fun padToLong(): Long {
        var result = 0L
        for (i in 0 until minOf(bytes.size, 8)) {
            result = result or ((bytes[i].toLong() and 0xff) shl (i * 8))
        }
        return result
    }
    fun asBytes(): ByteArray = bytes.copyOf()
    override fun toString(): String = bytes.joinToString("") { b ->
        val v = b.toInt() and 0xff
        ((v ushr 4).toString(16)) + (v and 0xf).toString(16)
    }
    override fun equals(other: Any?): Boolean = other is HashCode && bytes.contentEquals(other.bytes)
    override fun hashCode(): Int {
        if (bytes.size >= 4) return asInt()
        var result = 0
        for (i in bytes.indices) result = result or ((bytes[i].toInt() and 0xff) shl (i * 8))
        return result
    }
    companion object {
        fun fromInt(hash: Int): HashCode = HashCode(byteArrayOf(
            hash.toByte(), (hash ushr 8).toByte(), (hash ushr 16).toByte(), (hash ushr 24).toByte()
        ))
        fun fromLong(hash: Long): HashCode {
            val b = ByteArray(8)
            for (i in 0 until 8) b[i] = (hash ushr (i * 8)).toByte()
            return HashCode(b)
        }
        fun fromBytes(bytes: ByteArray): HashCode {
            require(bytes.isNotEmpty()) { "A HashCode must contain at least 1 byte." }
            return HashCode(bytes.copyOf())
        }
    }
}
