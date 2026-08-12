package com.bernaferrari.guavakt.io

/** Common equivalent of Guava's `ByteArrayDataOutput` / Java `DataOutput`. */
interface ByteArrayDataOutput {
    fun write(value: Int)
    fun write(bytes: ByteArray)
    fun write(bytes: ByteArray, offset: Int, length: Int)
    fun writeBoolean(value: Boolean)
    fun writeByte(value: Int)
    fun writeShort(value: Int)
    fun writeChar(value: Int)
    fun writeInt(value: Int)
    fun writeLong(value: Long)
    fun writeFloat(value: Float)
    fun writeDouble(value: Double)
    fun writeBytes(value: String)
    fun writeChars(value: String)
    fun writeUTF(value: String)
    fun toByteArray(): ByteArray

    /** Kotlin-shaped convenience retained from GuavaKt's initial byte-array implementation. */
    fun size(): Int
}
