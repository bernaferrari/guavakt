package com.bernaferrari.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsTest {
    @Test
    fun stats_mean_and_count() {
        val s = Stats.of(1.0, 2.0, 3.0, 4.0)
        assertEquals(4, s.count())
        assertEquals(2.5, s.mean(), 1e-9)
        assertTrue(s.sampleVariance() > 0)
    }

    @Test
    fun nonFiniteValuesPropagateThroughStatisticsAndMerges() {
        val nanStats = Stats.of(1.0, Double.NaN, 2.0)
        assertTrue(nanStats.mean().isNaN())
        assertTrue(nanStats.min().isNaN())
        assertTrue(nanStats.max().isNaN())
        assertTrue(nanStats.populationVariance().isNaN())

        val first = StatsAccumulator().apply { add(Double.POSITIVE_INFINITY) }
        val second = StatsAccumulator().apply { add(Double.NEGATIVE_INFINITY) }
        first.addAll(second.snapshot())
        assertTrue(first.mean().isNaN())
        assertTrue(first.populationVariance().isNaN())
    }
}
