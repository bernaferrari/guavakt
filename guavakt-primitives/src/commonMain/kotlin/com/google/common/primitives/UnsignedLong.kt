package dev.guavakt.primitives

/**
 * Guava UnsignedLong — 64-bit unsigned value in a signed Long bit pattern.
 */
class UnsignedLong private constructor(private val value: Long) : Comparable<UnsignedLong> {
    fun toLong(): Long = value
    fun toDouble(): Double =
        if (value >= 0) value.toDouble()
        else (value ushr 1).toDouble() * 2 + (value and 1)

    fun toBigEndianBytes(): ByteArray {
        val b = ByteArray(8)
        for (i in 0 until 8) b[7 - i] = (value ushr (i * 8)).toByte()
        return b
    }

    fun plus(other: UnsignedLong): UnsignedLong = fromLongBits(value + other.value)
    fun minus(other: UnsignedLong): UnsignedLong = fromLongBits(value - other.value)
    fun times(other: UnsignedLong): UnsignedLong = fromLongBits(value * other.value)

    fun dividedBy(other: UnsignedLong): UnsignedLong {
        require(other.value != 0L)
        // unsigned division via toULong
        return fromLongBits((value.toULong() / other.value.toULong()).toLong())
    }

    fun mod(other: UnsignedLong): UnsignedLong {
        require(other.value != 0L)
        return fromLongBits((value.toULong() % other.value.toULong()).toLong())
    }

    override fun compareTo(other: UnsignedLong): Int =
        value.toULong().compareTo(other.value.toULong())

    override fun equals(other: Any?): Boolean = other is UnsignedLong && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value.toULong().toString()
    fun toString(radix: Int): String = value.toULong().toString(radix)

    companion object {
        val ZERO = fromLongBits(0L)
        val ONE = fromLongBits(1L)
        val MAX_VALUE = fromLongBits(-1L)

        fun fromLongBits(bits: Long): UnsignedLong = UnsignedLong(bits)
        fun valueOf(value: Long): UnsignedLong {
            require(value >= 0) { "value is negative" }
            return fromLongBits(value)
        }
        fun valueOf(string: String): UnsignedLong = valueOf(string, 10)
        fun valueOf(string: String, radix: Int): UnsignedLong =
            fromLongBits(string.toULong(radix).toLong())
    }
}
