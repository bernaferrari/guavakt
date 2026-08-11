package dev.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantilesTest {
    @Test
    fun interpolationUsesTypeSevenAndHandlesInfiniteBounds() {
        assertEquals(5.0, Quantiles.median().compute(doubleArrayOf(9.0, 1.0)))
        assertEquals(2.0, Quantiles.quartiles().index(1).compute(doubleArrayOf(1.0, 3.0, 7.0)))
        assertEquals(
            Double.NEGATIVE_INFINITY,
            Quantiles.median().compute(doubleArrayOf(Double.NEGATIVE_INFINITY, 4.0)),
        )
        assertEquals(
            Double.POSITIVE_INFINITY,
            Quantiles.median().compute(doubleArrayOf(4.0, Double.POSITIVE_INFINITY)),
        )
        assertTrue(
            Quantiles.median()
                .compute(doubleArrayOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
                .isNaN(),
        )
    }

    @Test
    fun multiIndexComputationPreservesRequestedIndexOrder() {
        val input = doubleArrayOf(9.0, 1.0, 7.0, 3.0)
        val result = Quantiles.scale(4).indexes(4, 0, 2).computeInPlace(input)

        assertEquals(listOf(4, 0, 2), result.keys.toList())
        assertEquals(mapOf(4 to 9.0, 0 to 1.0, 2 to 5.0), result)
        assertEquals(listOf(9.0, 1.0, 7.0, 3.0), input.toList())
    }
}
