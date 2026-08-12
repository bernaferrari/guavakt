package com.bernaferrari.guavakt.io

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkioFileSystemTest {
    @Test
    fun injectedFileSystemReadsWritesAndProvidesSourcesAndSinks() {
        val fileSystem = FakeFileSystem()
        val directory = "/workspace".toPath()
        val binary = "/workspace/data.bin".toPath()
        val text = "/workspace/message.txt".toPath()
        fileSystem.createDirectories(directory)

        Files.write(fileSystem, binary, byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(fileSystem, binary))
        assertEquals(3L, Files.asByteSource(fileSystem, binary).sizeIfKnown())

        Files.asByteSink(fileSystem, binary).write(byteArrayOf(4, 5))
        assertContentEquals(byteArrayOf(4, 5), Files.asByteSource(fileSystem, binary).read())

        Files.asCharSink(fileSystem, text).write("olá KMP")
        assertEquals("olá KMP", Files.asCharSource(fileSystem, text).read())
    }

    @Test
    fun injectedFileSystemCreatesParentsAndTraversesDeterministically() {
        val fileSystem = FakeFileSystem()
        val root = "/root".toPath()
        val nested = "/root/a/b.txt".toPath()

        MoreFiles.createParentDirectories(fileSystem, nested)
        Files.write(fileSystem, nested, byteArrayOf(7))

        assertTrue(MoreFiles.isDirectory(fileSystem, "/root/a".toPath()))
        assertEquals(
            listOf("/root", "/root/a", "/root/a/b.txt"),
            MoreFiles.fileTraverser().breadthFirst(fileSystem, root).map { it.toString() },
        )
        assertEquals(
            listOf("/root", "/root/a", "/root/a/b.txt"),
            MoreFiles.fileTraverser().depthFirstPreOrder(fileSystem, root).map { it.toString() },
        )
    }
}
