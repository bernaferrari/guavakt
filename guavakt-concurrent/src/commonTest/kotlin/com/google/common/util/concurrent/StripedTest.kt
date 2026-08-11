package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class StripedTest {
    @Test
    fun customRoundsUpEagerlyAndUsesStableHashSmearing() {
        var supplied = 0
        val striped = Striped.custom(5) { supplied++ }

        assertEquals(8, striped.size())
        assertEquals(8, supplied)
        assertEquals((0 until 8).toList(), (0 until 8).map(striped::getAt))
        assertEquals(0, striped.get(FixedHash(0)))
        assertEquals(1, striped.get(FixedHash(1)))
        assertEquals(7, striped.get(FixedHash(-1)))
        assertEquals(striped.get(FixedHash(1)), striped.get(FixedHash(1)))
    }

    @Test
    fun bulkGetSortsByStripeAndPreservesRepeatedStripes() {
        val striped = Striped.custom(5) { Any() }
        val result = striped.bulkGet(
            listOf(FixedHash(-1), FixedHash(1), FixedHash(0), FixedHash(1)),
        )

        assertEquals(4, result.size)
        assertSame(striped.getAt(0), result[0])
        assertSame(striped.getAt(1), result[1])
        assertSame(striped.getAt(1), result[2])
        assertSame(striped.getAt(7), result[3])
    }

    @Test
    fun eagerFactoriesRejectImpossibleStripeCounts() {
        assertFailsWith<IllegalArgumentException> { Striped.custom(0) { Any() } }
        assertFailsWith<IllegalArgumentException> { Striped.custom((1 shl 30) + 1) { Any() } }
        assertFailsWith<IllegalArgumentException> { Striped.semaphore((1 shl 30) + 1, 1) }
    }

    private class FixedHash(private val hash: Int) {
        override fun hashCode(): Int = hash
    }
}
