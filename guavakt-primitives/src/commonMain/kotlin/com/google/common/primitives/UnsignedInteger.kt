package dev.guavakt.primitives

class UnsignedInteger private constructor(private val value: Int) : Comparable<UnsignedInteger> {
    fun toInt(): Int = value
    fun toLong(): Long = value.toLong() and 0xffffffffL
    fun toFloat(): Float = toLong().toFloat()
    fun toDouble(): Double = toLong().toDouble()

    fun plus(other: UnsignedInteger): UnsignedInteger = fromIntBits(value + other.value)
    fun minus(other: UnsignedInteger): UnsignedInteger = fromIntBits(value - other.value)
    fun times(other: UnsignedInteger): UnsignedInteger = fromIntBits(value * other.value)
    fun dividedBy(other: UnsignedInteger): UnsignedInteger {
        require(other.value != 0)
        return fromIntBits((toLong() / other.toLong()).toInt())
    }
    fun mod(other: UnsignedInteger): UnsignedInteger {
        require(other.value != 0)
        return fromIntBits((toLong() % other.toLong()).toInt())
    }

    override fun compareTo(other: UnsignedInteger): Int = toLong().compareTo(other.toLong())
    override fun equals(other: Any?): Boolean = other is UnsignedInteger && value == other.value
    override fun hashCode(): Int = value
    override fun toString(): String = toLong().toString()

    companion object {
        val ZERO = fromIntBits(0)
        val ONE = fromIntBits(1)
        val MAX_VALUE = fromIntBits(-1)
        fun fromIntBits(bits: Int): UnsignedInteger = UnsignedInteger(bits)
        fun valueOf(value: Long): UnsignedInteger {
            require(value in 0..0xffffffffL)
            return fromIntBits(value.toInt())
        }
    }
}
