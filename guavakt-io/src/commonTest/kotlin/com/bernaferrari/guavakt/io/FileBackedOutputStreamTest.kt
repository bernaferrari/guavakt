package com.bernaferrari.guavakt.io

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileBackedOutputStreamTest {
    @Test
    fun injectedFileSystemSpillsAtThresholdAndCachedViewStaysLive() {
        val fileSystem = FakeFileSystem()
        val directory = "/scratch".toPath()
        val spillFile = directory / "fbos.bin"
        fileSystem.createDirectories(directory)
        val stream = FileBackedOutputStream(3, fileSystem, spillFile)
        val view = stream.asByteSource()

        stream.write(byteArrayOf(1, 2, 3))
        assertNull(stream.spilledPathOrNull())
        assertContentEquals(byteArrayOf(1, 2, 3), view.read())

        stream.write(4)
        assertEquals(spillFile, stream.spilledPathOrNull())
        assertTrue(fileSystem.exists(spillFile))
        assertContentEquals(byteArrayOf(1, 2, 3, 4), view.read())

        stream.write(5)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), view.read())

        stream.reset()
        assertEquals(0, stream.getCount())
        assertFalse(fileSystem.exists(spillFile))
        assertContentEquals(byteArrayOf(), view.read())

        stream.write(9)
        assertContentEquals(byteArrayOf(9), view.read())
    }

    @Test
    fun memoryOnlyModeIsExplicitAndResettable() {
        val stream = FileBackedOutputStream(0)
        stream.write(byteArrayOf(7, 8))
        assertNull(stream.spilledPathOrNull())
        assertContentEquals(byteArrayOf(7, 8), stream.asByteSource().read())
        stream.reset()
        assertEquals(0, stream.getCount())
        assertContentEquals(byteArrayOf(), stream.asByteSource().read())
    }
}
