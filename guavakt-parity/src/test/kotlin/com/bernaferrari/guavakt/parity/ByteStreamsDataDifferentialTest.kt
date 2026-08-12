package com.bernaferrari.guavakt.parity

import com.google.common.io.ByteStreams as GuavaByteStreams
import com.bernaferrari.guavakt.io.ByteStreams
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteStreamsDataDifferentialTest {
    @Test
    fun primitiveAndModifiedUtfWireBytesMatchGuava() {
        val guava = GuavaByteStreams.newDataOutput()
        val kotlin = ByteStreams.newDataOutput()
        fun writeBoth(action: (com.google.common.io.ByteArrayDataOutput) -> Unit, ours: (com.bernaferrari.guavakt.io.ByteArrayDataOutput) -> Unit) {
            action(guava)
            ours(kotlin)
        }

        writeBoth({ it.writeBoolean(true) }, { it.writeBoolean(true) })
        writeBoth({ it.writeShort(0x8123) }, { it.writeShort(0x8123) })
        writeBoth({ it.writeInt(0x89abcdef.toInt()) }, { it.writeInt(0x89abcdef.toInt()) })
        writeBoth({ it.writeLong(0x0123456789abcdefL) }, { it.writeLong(0x0123456789abcdefL) })
        writeBoth({ it.writeUTF("zero=\u0000; accent=é; emoji=😀") }, { it.writeUTF("zero=\u0000; accent=é; emoji=😀") })

        assertContentEquals(guava.toByteArray(), kotlin.toByteArray())

        val guavaInput = GuavaByteStreams.newDataInput(guava.toByteArray())
        val kotlinInput = ByteStreams.newDataInput(kotlin.toByteArray())
        assertEquals(guavaInput.readBoolean(), kotlinInput.readBoolean())
        assertEquals(guavaInput.readShort(), kotlinInput.readShort())
        assertEquals(guavaInput.readInt(), kotlinInput.readInt())
        assertEquals(guavaInput.readLong(), kotlinInput.readLong())
        assertEquals(guavaInput.readUTF(), kotlinInput.readUTF())
    }

    @Test
    fun indexedInputBufferWindowsAndAllDataOutputWritesMatchGuava() {
        val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10)
        val guavaInput = GuavaByteStreams.newDataInput(bytes, 2)
        val kotlinInput = ByteStreams.newDataInput(bytes, 2)
        assertEquals(guavaInput.readInt(), kotlinInput.readInt())
        assertEquals(2, kotlinInput.available())

        val guava = GuavaByteStreams.newDataOutput()
        val kotlin = ByteStreams.newDataOutput()
        fun writeBoth(
            guavaAction: (com.google.common.io.ByteArrayDataOutput) -> Unit,
            kotlinAction: (com.bernaferrari.guavakt.io.ByteArrayDataOutput) -> Unit,
        ) {
            guavaAction(guava)
            kotlinAction(kotlin)
        }

        writeBoth({ it.write(0x12) }, { it.write(0x12) })
        writeBoth({ it.write(bytes, 1, 3) }, { it.write(bytes, 1, 3) })
        writeBoth({ it.writeBoolean(true) }, { it.writeBoolean(true) })
        writeBoth({ it.writeByte(0x123) }, { it.writeByte(0x123) })
        writeBoth({ it.writeShort(0x8123) }, { it.writeShort(0x8123) })
        writeBoth({ it.writeChar('λ'.code) }, { it.writeChar('λ'.code) })
        writeBoth({ it.writeInt(0x89abcdef.toInt()) }, { it.writeInt(0x89abcdef.toInt()) })
        writeBoth({ it.writeLong(0x0123456789abcdefL) }, { it.writeLong(0x0123456789abcdefL) })
        writeBoth({ it.writeFloat(Float.fromBits(0x7fc01234)) }, { it.writeFloat(Float.fromBits(0x7fc01234)) })
        writeBoth(
            { it.writeDouble(Double.fromBits(0x7ff8000000001234L)) },
            { it.writeDouble(Double.fromBits(0x7ff8000000001234L)) },
        )
        writeBoth({ it.writeBytes("Aé") }, { it.writeBytes("Aé") })
        writeBoth({ it.writeChars("λ😀") }, { it.writeChars("λ😀") })
        writeBoth({ it.writeUTF("zero=\u0000; lone=\ud800") }, { it.writeUTF("zero=\u0000; lone=\ud800") })
        assertContentEquals(guava.toByteArray(), kotlin.toByteArray())

        val guavaSnapshot = guava.toByteArray()
        val kotlinSnapshot = kotlin.toByteArray()
        guava.writeByte(0)
        kotlin.writeByte(0)
        assertContentEquals(guavaSnapshot, kotlinSnapshot)

        assertFailsWith<IndexOutOfBoundsException> { GuavaByteStreams.newDataInput(bytes, -1) }
        assertFailsWith<IndexOutOfBoundsException> { ByteStreams.newDataInput(bytes, -1) }
        assertFailsWith<IndexOutOfBoundsException> { guava.write(bytes, 7, 2) }
        assertFailsWith<IndexOutOfBoundsException> { kotlin.write(bytes, 7, 2) }
        assertFailsWith<IndexOutOfBoundsException> { GuavaByteStreams.newDataInput(bytes).readFully(ByteArray(2), 1, 2) }
        assertFailsWith<IndexOutOfBoundsException> { ByteStreams.newDataInput(bytes).readFully(ByteArray(2), 1, 2) }
    }

    @Test
    fun modifiedUtfMalformedAndOversizedInputsMatchGuavaCategories() {
        fun readGuava(bytes: ByteArray) = GuavaByteStreams.newDataInput(bytes).readUTF()
        fun readKotlin(bytes: ByteArray) = ByteStreams.newDataInput(bytes).readUTF()

        assertEquals(readGuava(byteArrayOf(0, 1, 0)), readKotlin(byteArrayOf(0, 1, 0)))
        assertEquals(
            readGuava(byteArrayOf(0, 2, 0xc0.toByte(), 0x81.toByte())),
            readKotlin(byteArrayOf(0, 2, 0xc0.toByte(), 0x81.toByte())),
        )
        assertFailsWith<IllegalStateException> { readGuava(byteArrayOf(0, 1, 0x80.toByte())) }
        assertFailsWith<IllegalStateException> { readKotlin(byteArrayOf(0, 1, 0x80.toByte())) }

        val tooLong = "\u0000".repeat(32_768)
        assertFailsWith<AssertionError> { GuavaByteStreams.newDataOutput().writeUTF(tooLong) }
        assertFailsWith<AssertionError> { ByteStreams.newDataOutput().writeUTF(tooLong) }
    }
}
