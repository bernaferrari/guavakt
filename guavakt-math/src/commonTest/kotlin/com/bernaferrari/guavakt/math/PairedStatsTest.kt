package com.bernaferrari.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PairedStatsTest {
    @Test
    fun mergedSnapshotsPreserveCovarianceAndRegression() {
        val first = PairedStatsAccumulator().apply {
            add(1.0, 1.0)
            add(2.0, 4.0)
        }
        val second = PairedStatsAccumulator().apply {
            add(3.0, 9.0)
            add(4.0, 16.0)
        }

        first.addAll(second.snapshot())
        val stats = first.snapshot()

        assertEquals(4L, stats.count())
        assertEquals(6.25, stats.populationCovariance())
        assertEquals(25.0 / kotlin.math.sqrt(645.0), stats.pearsonsCorrelationCoefficient())
        assertEquals(5.0, stats.leastSquaresFit().slope())
    }

    @Test
    fun constantAxesAndNonFiniteValuesFollowTheDocumentedStates() {
        val constantX = PairedStatsAccumulator().apply {
            add(1.0, 2.0)
            add(1.0, 3.0)
        }.snapshot()
        assertFailsWith<IllegalStateException> { constantX.pearsonsCorrelationCoefficient() }
        assertTrue(constantX.leastSquaresFit().isVertical())

        val nonFinite = PairedStatsAccumulator().apply {
            add(1.0, Double.NaN)
            add(2.0, 3.0)
        }.snapshot()
        assertTrue(nonFinite.populationCovariance().isNaN())
        assertTrue(nonFinite.pearsonsCorrelationCoefficient().isNaN())
        assertTrue(nonFinite.leastSquaresFit().slope().isNaN())
    }
}
