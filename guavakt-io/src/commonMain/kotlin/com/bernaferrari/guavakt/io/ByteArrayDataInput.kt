package com.bernaferrari.guavakt.io

/**
 * Common equivalent of Guava's `ByteArrayDataInput` / Java `DataInput`.
 *
 * End-of-input and malformed modified UTF-8 report [IllegalStateException], since common Kotlin
 * has no portable checked `EOFException` or `UTFDataFormatException` hierarchy.
 */
interface ByteArrayDataInput {
    fun readFully(bytes: ByteArray)
    fun readFully(bytes: ByteArray, offset: Int, length: Int)
    fun skipBytes(count: Int): Int
    fun readBoolean(): Boolean
    fun readByte(): Byte
    fun readUnsignedByte(): Int
    fun readShort(): Short
    fun readUnsignedShort(): Int
    fun readChar(): Char
    fun readInt(): Int
    fun readLong(): Long
    fun readFloat(): Float
    fun readDouble(): Double
    fun readLine(): String?
    fun readUTF(): String

    /** Kotlin-shaped convenience retained from GuavaKt's initial byte-array implementation. */
    fun available(): Int
}
