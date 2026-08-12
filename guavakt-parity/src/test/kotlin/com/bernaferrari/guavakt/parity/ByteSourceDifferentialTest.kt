package com.bernaferrari.guavakt.parity

import com.google.common.io.ByteSource as GuavaByteSource
import com.google.common.io.ByteProcessor as GuavaByteProcessor
import com.bernaferrari.guavakt.io.ByteSource as GuavaKtByteSource
import com.bernaferrari.guavakt.io.ByteProcessor as GuavaKtByteProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteSourceDifferentialTest {
    @Test
    fun sliceBoundariesAndNestedSlicesMatch() {
        val bytes = ByteArray(12) { (it * 19).toByte() }
        val guava = GuavaByteSource.wrap(bytes)
        val guavaKt = GuavaKtByteSource.wrap(bytes)
        val shapes = listOf(
            0L to 0L,
            0L to 1L,
            1L to 4L,
            11L to 10L,
            12L to 1L,
            50L to 5L,
            3L to Long.MAX_VALUE,
        )

        assertEquals(
            shapes.map { (offset, length) -> guava.slice(offset, length).read().toList() },
            shapes.map { (offset, length) -> guavaKt.slice(offset, length).read().toList() },
        )
        assertEquals(
            guava.slice(2, 7).slice(3, 20).read().toList(),
            guavaKt.slice(2, 7).slice(3, 20).read().toList(),
        )
    }

    @Test
    fun concatenationReadSizeAndEmptyBehaviorMatch() {
        val guava = GuavaByteSource.concat(
            GuavaByteSource.empty(),
            GuavaByteSource.wrap(byteArrayOf(1, 2)),
            GuavaByteSource.empty(),
            GuavaByteSource.wrap(byteArrayOf(3)),
        )
        val guavaKt = GuavaKtByteSource.concat(
            GuavaKtByteSource.empty(),
            GuavaKtByteSource.wrap(byteArrayOf(1, 2)),
            GuavaKtByteSource.empty(),
            GuavaKtByteSource.wrap(byteArrayOf(3)),
        )

        assertEquals(guava.read().toList(), guavaKt.read().toList())
        assertEquals(guava.size(), guavaKt.size())
        assertEquals(guava.isEmpty, guavaKt.isEmpty())
        assertEquals(guava.sizeIfKnown().orNull(), guavaKt.sizeIfKnown())
        assertEquals(GuavaByteSource.concat().isEmpty, GuavaKtByteSource.concat().isEmpty())
    }

    @Test
    fun iterableConcat_matchesGuava() {
        val guava = GuavaByteSource.concat(
            listOf(GuavaByteSource.wrap(byteArrayOf(1, 2)), GuavaByteSource.wrap(byteArrayOf(3))),
        )
        val guavaKt = GuavaKtByteSource.concat(
            listOf(GuavaKtByteSource.wrap(byteArrayOf(1, 2)), GuavaKtByteSource.wrap(byteArrayOf(3))),
        )

        assertEquals(guava.read().toList(), guavaKt.read().toList())
        assertEquals(guava.sizeIfKnown().orNull(), guavaKt.sizeIfKnown())
    }

    @Test
    fun wrappingRetainsTheSameObservableArrayAliasing() {
        val guavaBytes = byteArrayOf(1, 2, 3)
        val guavaKtBytes = guavaBytes.copyOf()
        val guava = GuavaByteSource.wrap(guavaBytes)
        val guavaKt = GuavaKtByteSource.wrap(guavaKtBytes)
        guavaBytes[1] = 9
        guavaKtBytes[1] = 9

        assertEquals(guava.read().toList(), guavaKt.read().toList())
    }

    @Test
    fun invalidSliceFailuresMatch() {
        val guava = GuavaByteSource.wrap(byteArrayOf(1))
        val guavaKt = GuavaKtByteSource.wrap(byteArrayOf(1))

        assertEquals(
            listOf(
                failureName { guava.slice(-1, 1) },
                failureName { guava.slice(0, -1) },
            ),
            listOf(
                failureName { guavaKt.slice(-1, 1) },
                failureName { guavaKt.slice(0, -1) },
            ),
        )
    }

    @Test
    fun contentEqualsAndByteProcessorEarlyStopMatchGuava() {
        val bytes = ByteArray(20_000) { (it * 31).toByte() }
        val guava = GuavaByteSource.wrap(bytes)
        val guavaKt = GuavaKtByteSource.wrap(bytes)

        assertEquals(
            listOf(
                guava.contentEquals(GuavaByteSource.wrap(bytes.copyOf())),
                guava.contentEquals(GuavaByteSource.wrap(bytes.copyOf().also { it[it.lastIndex] = 0 })),
            ),
            listOf(
                guavaKt.contentEquals(GuavaKtByteSource.wrap(bytes.copyOf())),
                guavaKt.contentEquals(GuavaKtByteSource.wrap(bytes.copyOf().also { it[it.lastIndex] = 0 })),
            ),
        )
        assertEquals(guavaProcessedBytes(guava), guavaKtProcessedBytes(guavaKt))
    }

    private fun guavaProcessedBytes(source: GuavaByteSource): Int = source.read(
        object : GuavaByteProcessor<Int> {
            private var processed = 0
            override fun processBytes(buffer: ByteArray, offset: Int, length: Int): Boolean {
                processed += length
                return processed < 9_000
            }
            override fun getResult(): Int = processed
        },
    )

    private fun guavaKtProcessedBytes(source: GuavaKtByteSource): Int = source.read(
        object : GuavaKtByteProcessor<Int> {
            private var processed = 0
            override fun processBytes(buffer: ByteArray, offset: Int, length: Int): Boolean {
                processed += length
                return processed < 9_000
            }
            override fun getResult(): Int = processed
        },
    )

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
