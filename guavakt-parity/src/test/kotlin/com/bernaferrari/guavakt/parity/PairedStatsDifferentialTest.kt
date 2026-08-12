package com.bernaferrari.guavakt.parity

import com.google.common.math.PairedStats as GuavaPairedStats
import com.google.common.math.PairedStatsAccumulator as GuavaPairedStatsAccumulator
import com.bernaferrari.guavakt.math.PairedStats
import com.bernaferrari.guavakt.math.PairedStatsAccumulator
import kotlin.test.Test
import kotlin.test.assertEquals

class PairedStatsDifferentialTest {
    @Test
    fun covarianceAndCorrelationMatchGuavaForFiniteConstantAndNonFinitePairs() {
        val datasets = listOf(
            listOf(1.0 to 1.0, 2.0 to 4.0, 3.0 to 9.0),
            listOf(1.0 to 2.0, 1.0 to 3.0),
            listOf(1.0 to 2.0, 2.0 to 2.0),
            listOf(Double.POSITIVE_INFINITY to 1.0),
            listOf(1.0 to Double.NaN, 2.0 to 3.0),
        )
        for (dataset in datasets) {
            val guava = GuavaPairedStatsAccumulator().also { accumulator -> dataset.forEach { (x, y) -> accumulator.add(x, y) } }.snapshot()
            val ours = PairedStatsAccumulator().also { accumulator -> dataset.forEach { (x, y) -> accumulator.add(x, y) } }.snapshot()
            assertEquals(snapshot(guava), snapshot(ours), dataset.toString())
        }
    }

    @Test
    fun mergedPairedStatsMatchGuava() {
        val first = listOf(1.0 to 1.0, 2.0 to 4.0)
        val second = listOf(3.0 to 9.0, 4.0 to 16.0)
        val guavaFirst = GuavaPairedStatsAccumulator().also { accumulator -> first.forEach { (x, y) -> accumulator.add(x, y) } }
        val guavaSecond = GuavaPairedStatsAccumulator().also { accumulator -> second.forEach { (x, y) -> accumulator.add(x, y) } }
        val oursFirst = PairedStatsAccumulator().also { accumulator -> first.forEach { (x, y) -> accumulator.add(x, y) } }
        val oursSecond = PairedStatsAccumulator().also { accumulator -> second.forEach { (x, y) -> accumulator.add(x, y) } }
        guavaFirst.addAll(guavaSecond.snapshot())
        oursFirst.addAll(oursSecond.snapshot())
        assertEquals(snapshot(guavaFirst.snapshot()), snapshot(oursFirst.snapshot()))
    }

    private fun snapshot(stats: GuavaPairedStats): List<Any?> = listOf(
        stats.count(),
        stats.populationCovariance().toRawBits(),
        if (stats.count() > 1) stats.sampleCovariance().toRawBits() else "single",
        outcome { stats.pearsonsCorrelationCoefficient().toRawBits() },
    )

    private fun snapshot(stats: PairedStats): List<Any?> = listOf(
        stats.count(),
        stats.populationCovariance().toRawBits(),
        if (stats.count() > 1) stats.sampleCovariance().toRawBits() else "single",
        outcome { stats.pearsonsCorrelationCoefficient().toRawBits() },
    )

    private fun <T> outcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
