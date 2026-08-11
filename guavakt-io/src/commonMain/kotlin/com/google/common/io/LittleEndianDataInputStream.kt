package dev.guavakt.io

/** Guava LittleEndianDataInputStream — reads little-endian primitives. */
class LittleEndianDataInputStream(private val input: ByteArrayInputLike) : AutoCloseable {
    fun readUnsignedByte(): Int {
        val b = input.read()
        if (b < 0) throw IllegalStateException("EOF")
        return b
    }
    fun readByte(): Byte = readUnsignedByte().toByte()
    fun readShort(): Short {
        val b1 = readUnsignedByte(); val b2 = readUnsignedByte()
        return ((b2 shl 8) or b1).toShort()
    }
    fun readInt(): Int {
        val b1 = readUnsignedByte(); val b2 = readUnsignedByte()
        val b3 = readUnsignedByte(); val b4 = readUnsignedByte()
        return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
    }
    fun readLong(): Long {
        val lo = readInt().toLong() and 0xffffffffL
        val hi = readInt().toLong()
        return (hi shl 32) or lo
    }
    fun readFloat(): Float = Float.fromBits(readInt())
    fun readDouble(): Double = Double.fromBits(readLong())
    override fun close() = input.close()
}
