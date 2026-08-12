package com.bernaferrari.guavakt.io

/** Guava LittleEndianDataOutputStream — writes little-endian primitives. */
class LittleEndianDataOutputStream(private val output: ByteArrayOutputLike) : AutoCloseable {
    fun write(b: Int) = output.write(b)
    fun writeByte(v: Int) = write(v)
    fun writeShort(v: Int) { write(v); write(v ushr 8) }
    fun writeInt(v: Int) {
        write(v); write(v ushr 8); write(v ushr 16); write(v ushr 24)
    }
    fun writeLong(v: Long) {
        writeInt(v.toInt()); writeInt((v ushr 32).toInt())
    }
    fun writeFloat(v: Float) = writeInt(v.toRawBits())
    fun writeDouble(v: Double) = writeLong(v.toRawBits())
    override fun close() = output.close()
}
