package com.bernaferrari.guavakt.parity

import com.google.common.io.FileBackedOutputStream as GuavaFileBackedOutputStream
import com.bernaferrari.guavakt.io.FileBackedOutputStream as GuavaKtFileBackedOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FileBackedOutputStreamDifferentialTest {
    @Test
    fun byteSourceViewAndResetMatchGuava() {
        val guava = GuavaFileBackedOutputStream(3)
        val kotlin = GuavaKtFileBackedOutputStream(3)
        val guavaView = guava.asByteSource()
        val kotlinView = kotlin.asByteSource()

        guava.write(byteArrayOf(1, 2, 3))
        kotlin.write(byteArrayOf(1, 2, 3))
        assertContentEquals(guavaView.read(), kotlinView.read())

        guava.write(4)
        kotlin.write(4)
        assertContentEquals(guavaView.read(), kotlinView.read())

        guava.write(5)
        kotlin.write(5)
        assertContentEquals(guavaView.read(), kotlinView.read())

        guava.reset()
        kotlin.reset()
        assertContentEquals(guavaView.read(), kotlinView.read())
        assertEquals(0, kotlin.getCount())
    }
}
