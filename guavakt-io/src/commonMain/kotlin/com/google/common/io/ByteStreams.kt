package dev.guavakt.io

/**
 * Guava ByteStreams — byte utilities.
 *
 * Common: [ByteArray] APIs plus small data in/out helpers.
 *
 * Streaming I/O uses Okio [ByteSource] and [ByteSink], rather than JVM stream overloads.
 */
object ByteStreams {
    fun toByteArray(bytes: ByteArray): ByteArray = bytes.copyOf()

    fun copy(from: ByteArray, to: MutableList<Byte>): Long {
        for (b in from) to.add(b)
        return from.size.toLong()
    }

    fun copy(from: ByteArray, to: ByteArray, off: Int = 0): Int {
        require(off >= 0 && off <= to.size)
        val n = minOf(from.size, to.size - off)
        from.copyInto(to, off, 0, n)
        return n
    }

    fun limit(bytes: ByteArray, limit: Long): ByteArray {
        require(limit >= 0)
        if (limit >= bytes.size) return bytes.copyOf()
        return bytes.copyOf(limit.toInt())
    }

    fun nullOutputStream(): (Byte) -> Unit = { }

    fun read(bytes: ByteArray, off: Int, len: Int): ByteArray {
        require(off >= 0 && len >= 0 && off + len <= bytes.size)
        return bytes.copyOfRange(off, off + len)
    }

    fun exhaust(bytes: ByteArray): Long = bytes.size.toLong()

    /**
     * Returns a Java-`DataInput` shaped view starting at the beginning of [bytes].
     *
     * The array remains the backing storage, just as Guava's `ByteArrayInputStream` overload does.
     */
    fun newDataInput(bytes: ByteArray): dev.guavakt.io.ByteArrayDataInput = ByteArrayDataInput(bytes)

    /**
     * Returns a Java-`DataInput` shaped view starting at [start].
     *
     * @throws IndexOutOfBoundsException when [start] is outside `0..bytes.size`.
     */
    fun newDataInput(bytes: ByteArray, start: Int): dev.guavakt.io.ByteArrayDataInput {
        if (start < 0 || start > bytes.size) {
            throw IndexOutOfBoundsException("start=$start, size=${bytes.size}")
        }
        return ByteArrayDataInput(bytes, start)
    }
    fun newDataOutput(): dev.guavakt.io.ByteArrayDataOutput = ByteArrayDataOutput()
    fun newDataOutput(size: Int): dev.guavakt.io.ByteArrayDataOutput = ByteArrayDataOutput(size)

    /** Processes [bytes] once, honoring [ByteProcessor.processBytes]'s early-stop signal. */
    fun <T> readBytes(bytes: ByteArray, processor: ByteProcessor<T>): T {
        processor.processBytes(bytes, 0, bytes.size)
        return processor.getResult()
    }

    class ByteArrayDataInput(private val data: ByteArray, start: Int = 0) : dev.guavakt.io.ByteArrayDataInput {
        private var pos = start

        override fun readFully(bytes: ByteArray) = readFully(bytes, 0, bytes.size)

        override fun readFully(bytes: ByteArray, offset: Int, length: Int) {
            if (offset < 0 || length < 0 || offset > bytes.size - length) {
                throw IndexOutOfBoundsException("offset=$offset, length=$length, size=${bytes.size}")
            }
            requireRemaining(length)
            data.copyInto(bytes, offset, pos, pos + length)
            pos += length
        }

        override fun skipBytes(count: Int): Int {
            val skipped = minOf(count.coerceAtLeast(0), data.size - pos)
            pos += skipped
            return skipped
        }

        override fun readBoolean(): Boolean = readUnsignedByte() != 0

        override fun readByte(): Byte {
            requireRemaining(1)
            return data[pos++]
        }

        override fun readUnsignedByte(): Int = readByte().toInt() and 0xff

        override fun readShort(): Short = readUnsignedShort().toShort()

        override fun readUnsignedShort(): Int =
            (readUnsignedByte() shl 8) or readUnsignedByte()

        override fun readChar(): Char = readUnsignedShort().toChar()

        override fun readInt(): Int =
            (readUnsignedByte() shl 24) or
                (readUnsignedByte() shl 16) or
                (readUnsignedByte() shl 8) or
                readUnsignedByte()

        override fun readLong(): Long =
            (readUnsignedByte().toLong() shl 56) or
                (readUnsignedByte().toLong() shl 48) or
                (readUnsignedByte().toLong() shl 40) or
                (readUnsignedByte().toLong() shl 32) or
                (readUnsignedByte().toLong() shl 24) or
                (readUnsignedByte().toLong() shl 16) or
                (readUnsignedByte().toLong() shl 8) or
                readUnsignedByte().toLong()

        override fun readFloat(): Float = Float.fromBits(readInt())

        override fun readDouble(): Double = Double.fromBits(readLong())

        override fun readLine(): String? {
            if (pos == data.size) return null
            val line = StringBuilder()
            while (pos < data.size) {
                val value = readUnsignedByte()
                when (value) {
                    '\n'.code -> return line.toString()
                    '\r'.code -> {
                        if (pos < data.size && (data[pos].toInt() and 0xff) == '\n'.code) pos++
                        return line.toString()
                    }
                    else -> line.append(value.toChar())
                }
            }
            return line.toString()
        }

        override fun readUTF(): String {
            val length = readUnsignedShort()
            val encoded = ByteArray(length)
            readFully(encoded)
            return decodeModifiedUtf8(encoded)
        }

        override fun available(): Int = data.size - pos

        private fun requireRemaining(length: Int) {
            if (length < 0 || data.size - pos < length) {
                throw IllegalStateException("Unexpected end of byte-array input")
            }
        }
    }

    class ByteArrayDataOutput(size: Int = 32) : dev.guavakt.io.ByteArrayDataOutput {
        private val buf = ArrayList<Byte>(size.also { require(it >= 0) })

        override fun write(value: Int) {
            buf.add(value.toByte())
        }

        override fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            if (offset < 0 || length < 0 || offset > bytes.size - length) {
                throw IndexOutOfBoundsException("offset=$offset, length=$length, size=${bytes.size}")
            }
            for (index in offset until offset + length) buf.add(bytes[index])
        }

        override fun writeBoolean(value: Boolean) = write(if (value) 1 else 0)
        override fun writeByte(value: Int) = write(value)
        override fun writeShort(value: Int) {
            write(value ushr 8)
            write(value)
        }
        override fun writeChar(value: Int) = writeShort(value)
        override fun writeInt(value: Int) {
            write(value ushr 24)
            write(value ushr 16)
            write(value ushr 8)
            write(value)
        }
        override fun writeLong(value: Long) {
            write((value ushr 56).toInt())
            write((value ushr 48).toInt())
            write((value ushr 40).toInt())
            write((value ushr 32).toInt())
            write((value ushr 24).toInt())
            write((value ushr 16).toInt())
            write((value ushr 8).toInt())
            write(value.toInt())
        }
        override fun writeFloat(value: Float) = writeInt(value.toBits())
        override fun writeDouble(value: Double) = writeLong(value.toBits())
        override fun writeBytes(value: String) {
            for (character in value) write(character.code)
        }
        override fun writeChars(value: String) {
            for (character in value) writeChar(character.code)
        }
        override fun writeUTF(value: String) {
            val encoded = encodeModifiedUtf8(value)
            if (encoded.size > 0xffff) {
                // Guava's DataOutputStream adapter treats its impossible checked exception as an
                // assertion failure. Keep that observable unchecked category on every target.
                throw AssertionError("encoded string is too long: ${encoded.size} bytes")
            }
            writeShort(encoded.size)
            write(encoded)
        }

        override fun toByteArray(): ByteArray = ByteArray(buf.size) { buf[it] }
        override fun size(): Int = buf.size
    }

    private fun encodeModifiedUtf8(value: String): ByteArray {
        val bytes = ArrayList<Byte>(value.length)
        for (character in value) {
            when (val code = character.code) {
                in 0x0001..0x007f -> bytes += code.toByte()
                in 0x0000..0x07ff -> {
                    bytes += (0xc0 or (code ushr 6)).toByte()
                    bytes += (0x80 or (code and 0x3f)).toByte()
                }
                else -> {
                    bytes += (0xe0 or (code ushr 12)).toByte()
                    bytes += (0x80 or ((code ushr 6) and 0x3f)).toByte()
                    bytes += (0x80 or (code and 0x3f)).toByte()
                }
            }
        }
        return ByteArray(bytes.size) { bytes[it] }
    }

    private fun decodeModifiedUtf8(bytes: ByteArray): String {
        val characters = StringBuilder(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xff
            when {
                first and 0x80 == 0 -> {
                    characters.append(first.toChar())
                    index++
                }
                first and 0xe0 == 0xc0 -> {
                    if (index + 1 >= bytes.size) malformedModifiedUtf8()
                    val second = bytes[index + 1].toInt() and 0xff
                    if (second and 0xc0 != 0x80) malformedModifiedUtf8()
                    characters.append((((first and 0x1f) shl 6) or (second and 0x3f)).toChar())
                    index += 2
                }
                first and 0xf0 == 0xe0 -> {
                    if (index + 2 >= bytes.size) malformedModifiedUtf8()
                    val second = bytes[index + 1].toInt() and 0xff
                    val third = bytes[index + 2].toInt() and 0xff
                    if (second and 0xc0 != 0x80 || third and 0xc0 != 0x80) malformedModifiedUtf8()
                    characters.append(
                        (((first and 0x0f) shl 12) or ((second and 0x3f) shl 6) or (third and 0x3f)).toChar(),
                    )
                    index += 3
                }
                else -> malformedModifiedUtf8()
            }
        }
        return characters.toString()
    }

    private fun malformedModifiedUtf8(): Nothing =
        throw IllegalStateException("Malformed modified UTF-8 byte sequence")
}
