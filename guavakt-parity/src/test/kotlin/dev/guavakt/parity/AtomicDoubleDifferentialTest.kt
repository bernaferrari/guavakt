package dev.guavakt.parity

import com.google.common.util.concurrent.AtomicDouble as GuavaAtomicDouble
import dev.guavakt.util.concurrent.AtomicDouble as GuavaKtAtomicDouble
import kotlin.test.Test
import kotlin.test.assertEquals

class AtomicDoubleDifferentialTest {
    @Test
    fun mutationAccumulationAndNumericViewsMatchGuava() {
        val guava = GuavaAtomicDouble(1.5)
        val kotlin = GuavaKtAtomicDouble(1.5)

        val guavaTrace = mutableListOf<Any?>()
        guavaTrace += guava.getAndSet(2.5)
        guavaTrace += guava.weakCompareAndSet(2.5, 3.5)
        guavaTrace += guava.getAndAdd(2.0)
        guavaTrace += guava.addAndGet(1.0)
        guavaTrace += guava.getAndUpdate { it * 2.0 }
        guavaTrace += guava.updateAndGet { it - 3.0 }
        guavaTrace += guava.getAndAccumulate(4.0) { left, right -> left / right }
        guavaTrace += guava.accumulateAndGet(5.0) { left, right -> left + right * 2.0 }
        guavaTrace.addAll(listOf(guava.toByte(), guava.toShort(), guava.toInt(), guava.toLong(), guava.toFloat(), guava.toDouble(), guava.toString()))

        val kotlinTrace = mutableListOf<Any?>()
        kotlinTrace += kotlin.getAndSet(2.5)
        kotlinTrace += kotlin.weakCompareAndSet(2.5, 3.5)
        kotlinTrace += kotlin.getAndAdd(2.0)
        kotlinTrace += kotlin.addAndGet(1.0)
        kotlinTrace += kotlin.getAndUpdate { it * 2.0 }
        kotlinTrace += kotlin.updateAndGet { it - 3.0 }
        kotlinTrace += kotlin.getAndAccumulate(4.0) { left, right -> left / right }
        kotlinTrace += kotlin.accumulateAndGet(5.0) { left, right -> left + right * 2.0 }
        kotlinTrace.addAll(listOf(kotlin.toByte(), kotlin.toShort(), kotlin.toInt(), kotlin.toLong(), kotlin.toFloat(), kotlin.toDouble(), kotlin.toString()))

        assertEquals(guavaTrace, kotlinTrace)
    }

    @Test
    fun compareAndSetUsesRawBitsForSignedZeroAndNanPayloads() {
        val positiveZero = 0.0
        val negativeZero = Double.fromBits(Long.MIN_VALUE)
        val firstNan = Double.fromBits(0x7ff8000000000001L)
        val secondNan = Double.fromBits(0x7ff8000000000002L)

        listOf(
            positiveZero to negativeZero,
            negativeZero to positiveZero,
            firstNan to secondNan,
            secondNan to firstNan,
        ).forEach { (initial, expected) ->
            val guava = GuavaAtomicDouble(initial)
            val kotlin = GuavaKtAtomicDouble(initial)
            assertEquals(
                guava.compareAndSet(expected, 1.0),
                kotlin.compareAndSet(expected, 1.0),
                "initial=${initial.toRawBits()} expected=${expected.toRawBits()}",
            )
            assertEquals(
                guava.compareAndSet(initial, 2.0),
                kotlin.compareAndSet(initial, 2.0),
                "initial=${initial.toRawBits()} expected=${initial.toRawBits()}",
            )
            assertEquals(guava.get().toRawBits(), kotlin.get().toRawBits())
        }
    }
}
