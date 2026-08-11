package dev.guavakt.io

import java.nio.file.Files as JFiles
import java.nio.charset.StandardCharsets
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilesByteSourceJvmTest {
    @Test
    fun jvmOnlyPathBridgeKeepsJdkCharsetAndTempDirectoryInteropOutOfCommonCode() {
        val dir = Files.createTempDir("guavakt-jvm-bridge")
        val latin1 = dir.resolve("latin1.txt")
        JFiles.writeString(latin1, "café", StandardCharsets.ISO_8859_1)

        assertEquals("café", Files.asCharSource(latin1, StandardCharsets.ISO_8859_1).read())
        assertTrue(JFiles.isDirectory(dir))
    }

    @Test
    fun asByteSource_roundTrip() {
        val dir = JFiles.createTempDirectory("guavakt")
        val path = dir.resolve("t.txt")
        val payload = "hello-guavakt".encodeToByteArray()
        Files.write(path, payload)
        val read = Files.asByteSource(path).read()
        assertTrue(payload.contentEquals(read))
        assertEquals("hello-guavakt", Files.asCharSource(path).read())
        val sinkPath = dir.resolve("out.bin")
        ByteSource.wrap(payload).copyTo(Files.asByteSink(sinkPath))
        assertTrue(payload.contentEquals(Files.readAllBytes(sinkPath)))
    }

    @Test
    fun jvmPathSourcesAndSinksUseIncrementalOkioResources() {
        val dir = JFiles.createTempDirectory("guavakt-streaming")
        val path = dir.resolve("stream.txt")
        Files.write(path, "first\nsecond".encodeToByteArray())

        Files.asByteSource(path).openSource().use { source ->
            val buffer = Buffer()
            assertEquals(5L, source.read(buffer, 5L))
            assertEquals("first", buffer.readUtf8())
        }
        assertEquals("first", Files.asCharSource(path).readFirstLine())

        val output = dir.resolve("incremental.bin")
        Files.asByteSink(output).openSink().use { sink ->
            sink.write(Buffer().writeUtf8("before-flush"), 12L)
            sink.flush()
            assertEquals("before-flush", JFiles.readString(output))
        }
    }
}
