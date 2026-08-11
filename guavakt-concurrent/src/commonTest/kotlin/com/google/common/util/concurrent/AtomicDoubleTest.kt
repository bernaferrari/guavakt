package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtomicDoubleTest {
    @Test
    fun rawBitsUpdatesAndAccumulatorsAreAtomicAtTheApiBoundary() {
        val firstNan = Double.fromBits(0x7ff8000000000001L)
        val secondNan = Double.fromBits(0x7ff8000000000002L)
        val atomic = AtomicDouble(firstNan)
        assertFalse(atomic.compareAndSet(secondNan, 1.0))
        assertTrue(atomic.compareAndSet(firstNan, 2.0))
        assertEquals(2.0, atomic.getAndAccumulate(3.0) { left, right -> left * right })
        assertEquals(10.0, atomic.accumulateAndGet(4.0) { left, right -> left + right })
        assertEquals(11.0, atomic.updateAndGet { it + 1.0 })
        assertEquals(11, atomic.toInt())
        assertEquals(11.0, atomic.toDouble())
    }
}
