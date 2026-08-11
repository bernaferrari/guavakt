package dev.guavakt.parity

import com.google.common.hash.Hashing as GuavaHashing
import com.google.common.hash.HashingInputStream as GuavaHashingInputStream
import com.google.common.hash.HashingOutputStream as GuavaHashingOutputStream
import dev.guavakt.hash.Hashing
import dev.guavakt.hash.HashingSink
import dev.guavakt.hash.HashingSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import okio.Buffer
import okio.Sink
import okio.Timeout
import kotlin.test.Test
import kotlin.test.assertEquals

class HashingOkioDifferentialTest {
    @Test
    fun okioHashingSourceAndSinkMatchGuavaHashingStreams() {
        val bytes = ByteArray(20_003) { (it * 73).toByte() }

        assertEquals(
            guavaInputTrace(bytes),
            guavaKtInputTrace(bytes),
        )
        assertEquals(
            guavaOutputTrace(bytes),
            guavaKtOutputTrace(bytes),
        )
    }

    @Test
    fun failedWriteStillContributesTheSameHashPrefixAsGuava() {
        val bytes = byteArrayOf(5, 4, 3, 2, 1)
        val guava = GuavaHashingOutputStream(GuavaHashing.sha256(), GuavaFailingOutputStream())
        val guavaTrace = failureName { guava.write(bytes) } to guava.hash().toString()

        val guavaKt = HashingSink(FailingSink(), Hashing.sha256())
        val guavaKtTrace = failureName {
            guavaKt.write(Buffer().write(bytes), bytes.size.toLong())
        } to guavaKt.hash().toString()

        assertEquals(guavaTrace, guavaKtTrace)
    }

    private fun guavaInputTrace(bytes: ByteArray): Pair<List<Byte>, String> {
        val stream = GuavaHashingInputStream(GuavaHashing.sha512(), ByteArrayInputStream(bytes))
        return try {
            readGuava(stream) to stream.hash().toString()
        } finally {
            stream.close()
        }
    }

    private fun guavaKtInputTrace(bytes: ByteArray): Pair<List<Byte>, String> {
        val stream = HashingSource(Buffer().write(bytes), Hashing.sha512())
        return try {
            readGuavaKt(stream) to stream.hash().toString()
        } finally {
            stream.close()
        }
    }

    private fun guavaOutputTrace(bytes: ByteArray): Pair<List<Byte>, String> {
        val output = ByteArrayOutputStream()
        val stream = GuavaHashingOutputStream(GuavaHashing.sha256(), output)
        return try {
            writeGuava(stream, bytes)
            output.toByteArray().toList() to stream.hash().toString()
        } finally {
            stream.close()
        }
    }

    private fun guavaKtOutputTrace(bytes: ByteArray): Pair<List<Byte>, String> {
        val output = Buffer()
        val stream = HashingSink(output, Hashing.sha256())
        return try {
            writeGuavaKt(stream, bytes)
            output.readByteArray().toList() to stream.hash().toString()
        } finally {
            stream.close()
        }
    }

    private fun readGuava(stream: GuavaHashingInputStream): List<Byte> {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(2_048)
        for (requested in CHUNKS) {
            val count = stream.read(buffer, 0, minOf(requested, buffer.size))
            if (count < 0) return output.toByteArray().toList()
            output.write(buffer, 0, count)
        }
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) return output.toByteArray().toList()
            output.write(buffer, 0, count)
        }
    }

    private fun readGuavaKt(stream: HashingSource): List<Byte> {
        val output = Buffer()
        for (requested in CHUNKS) {
            if (stream.read(output, requested.toLong()) < 0L) return output.readByteArray().toList()
        }
        while (stream.read(output, 2_048L) >= 0L) Unit
        return output.readByteArray().toList()
    }

    private fun writeGuava(stream: GuavaHashingOutputStream, bytes: ByteArray) {
        var offset = 0
        for (requested in CHUNKS) {
            if (offset == bytes.size) return
            val count = minOf(requested, bytes.size - offset)
            stream.write(bytes, offset, count)
            offset += count
        }
        if (offset < bytes.size) stream.write(bytes, offset, bytes.size - offset)
    }

    private fun writeGuavaKt(stream: HashingSink, bytes: ByteArray) {
        var offset = 0
        for (requested in CHUNKS) {
            if (offset == bytes.size) return
            val count = minOf(requested, bytes.size - offset)
            stream.write(Buffer().write(bytes, offset, count), count.toLong())
            offset += count
        }
        if (offset < bytes.size) {
            val count = bytes.size - offset
            stream.write(Buffer().write(bytes, offset, count), count.toLong())
        }
    }

    private class GuavaFailingOutputStream : OutputStream() {
        override fun write(b: Int): Nothing = throw IllegalStateException("downstream failed")
    }

    private class FailingSink : Sink {
        override fun write(source: Buffer, byteCount: Long): Nothing =
            throw IllegalStateException("downstream failed")
        override fun flush() = Unit
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() = Unit
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }

    private companion object {
        val CHUNKS = (1..17).toList()
    }
}
