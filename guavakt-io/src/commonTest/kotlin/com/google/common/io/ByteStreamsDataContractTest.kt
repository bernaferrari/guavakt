package dev.guavakt.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteStreamsDataContractTest {
    @Test
    fun dataOutputAndInputRoundTripBigEndianPrimitivesAndModifiedUtf() {
        val output = ByteStreams.newDataOutput()
        output.writeBoolean(true)
        output.writeShort(0x8123)
        output.writeChar('λ'.code)
        output.writeInt(0x89abcdef.toInt())
        output.writeLong(0x0123456789abcdefL)
        output.writeFloat(1.25f)
        output.writeDouble(-2.5)
        output.writeUTF("zero=\u0000; accent=é; emoji=😀")

        val input = ByteStreams.newDataInput(output.toByteArray())
        assertEquals(true, input.readBoolean())
        assertEquals(0x8123.toShort(), input.readShort())
        assertEquals('λ', input.readChar())
        assertEquals(0x89abcdef.toInt(), input.readInt())
        assertEquals(0x0123456789abcdefL, input.readLong())
        assertEquals(1.25f, input.readFloat())
        assertEquals(-2.5, input.readDouble())
        assertEquals("zero=\u0000; accent=é; emoji=😀", input.readUTF())
        assertEquals(0, input.available())
        assertFailsWith<IllegalStateException> { input.readByte() }
    }

    @Test
    fun dataInputSupportsRangesAndLegacyLineTerminators() {
        val input = ByteStreams.newDataInput(byteArrayOf(1, 2, 3, 'a'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte(), 'b'.code.toByte(), '\r'.code.toByte(), 'c'.code.toByte()))
        val destination = byteArrayOf(9, 9, 9, 9, 9)

        input.readFully(destination, 1, 2)
        assertContentEquals(byteArrayOf(9, 1, 2, 9, 9), destination)
        assertEquals(1, input.skipBytes(1))
        assertEquals("a", input.readLine())
        assertEquals("b", input.readLine())
        assertEquals("c", input.readLine())
        assertEquals(null, input.readLine())
    }

    @Test
    fun indexedInputAndInvalidWindowsFollowDataInputContracts() {
        val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val input = ByteStreams.newDataInput(bytes, 2)
        assertEquals(0x5678, input.readUnsignedShort())
        assertEquals(0, input.available())
        assertFailsWith<IndexOutOfBoundsException> { ByteStreams.newDataInput(bytes, -1) }
        assertFailsWith<IndexOutOfBoundsException> { ByteStreams.newDataInput(bytes, bytes.size + 1) }
        assertFailsWith<IndexOutOfBoundsException> { ByteStreams.newDataInput(bytes).readFully(ByteArray(2), 1, 2) }
        assertFailsWith<IndexOutOfBoundsException> { ByteStreams.newDataOutput().write(bytes, 3, 2) }
    }

    @Test
    fun modifiedUtfKeepsJavaCompatibleWireRules() {
        assertEquals("\u0000", ByteStreams.newDataInput(byteArrayOf(0, 1, 0)).readUTF())
        assertEquals("\u0001", ByteStreams.newDataInput(byteArrayOf(0, 2, 0xc0.toByte(), 0x81.toByte())).readUTF())
        assertFailsWith<IllegalStateException> {
            ByteStreams.newDataInput(byteArrayOf(0, 1, 0x80.toByte())).readUTF()
        }
        assertFailsWith<AssertionError> { ByteStreams.newDataOutput().writeUTF("\u0000".repeat(32_768)) }
    }

    @Test
    fun byteAndLineProcessorsHonorTheirStopSignals() {
        val processed = mutableListOf<Byte>()
        val byteResult = ByteStreams.readBytes(
            byteArrayOf(1, 2, 3),
            object : ByteProcessor<List<Byte>> {
                override fun processBytes(buffer: ByteArray, offset: Int, length: Int): Boolean {
                    processed += buffer.copyOfRange(offset, offset + length).toList()
                    return false
                }
                override fun getResult(): List<Byte> = processed
            },
        )
        assertEquals(listOf<Byte>(1, 2, 3), byteResult)

        val lines = CharStreams.readLines(
            "first\r\nsecond\rthird\nignored",
            object : LineProcessor<List<String>> {
                private val result = mutableListOf<String>()
                override fun processLine(line: String): Boolean {
                    result += line
                    return line != "second"
                }
                override fun getResult(): List<String> = result
            },
        )
        assertEquals(listOf("first", "second"), lines)
    }
}
