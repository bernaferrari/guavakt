package dev.guavakt.parity

import com.google.common.math.Stats as GuavaStats
import com.google.common.math.StatsAccumulator as GuavaStatsAccumulator
import dev.guavakt.math.Stats
import dev.guavakt.math.StatsAccumulator
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsDifferentialTest {
    @Test
    fun statisticsAndNonFinitePropagationMatchGuava() {
        val datasets = listOf(
            doubleArrayOf(1.0),
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(Double.NaN),
            doubleArrayOf(1.0, Double.NaN, 2.0),
            doubleArrayOf(Double.POSITIVE_INFINITY),
            doubleArrayOf(1.0, Double.POSITIVE_INFINITY),
            doubleArrayOf(Double.NEGATIVE_INFINITY, -1.0),
            doubleArrayOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
            doubleArrayOf(Double.MAX_VALUE, Double.MAX_VALUE),
            doubleArrayOf(1e308, -1e308, 3.0),
        )
        for (dataset in datasets) {
            val guava = GuavaStats.of(*dataset)
            val ours = Stats.of(*dataset)
            assertEquals(statsSnapshot(guava), statsSnapshot(ours), dataset.contentToString())
        }
    }

    @Test
    fun mergedStatisticsMatchGuavaForFiniteAndNonFinitePartitions() {
        val partitions = listOf(
            doubleArrayOf(1.0, 2.0) to doubleArrayOf(3.0, 4.0),
            doubleArrayOf(1.0, Double.POSITIVE_INFINITY) to doubleArrayOf(2.0),
            doubleArrayOf(Double.NEGATIVE_INFINITY) to doubleArrayOf(Double.POSITIVE_INFINITY),
            doubleArrayOf(Double.NaN) to doubleArrayOf(4.0, 5.0),
        )
        for ((first, second) in partitions) {
            val guavaFirst = GuavaStatsAccumulator().also { accumulator -> first.forEach(accumulator::add) }
            val guavaSecond = GuavaStatsAccumulator().also { accumulator -> second.forEach(accumulator::add) }
            val oursFirst = StatsAccumulator().also { accumulator -> first.forEach(accumulator::add) }
            val oursSecond = StatsAccumulator().also { accumulator -> second.forEach(accumulator::add) }
            guavaFirst.addAll(guavaSecond.snapshot())
            oursFirst.addAll(oursSecond.snapshot())
            assertEquals(statsSnapshot(guavaFirst.snapshot()), statsSnapshot(oursFirst.snapshot()))
        }
    }

    private fun statsSnapshot(stats: GuavaStats): List<Any?> = listOf(
        stats.count(),
        stats.mean().toRawBits(),
        stats.sum().toRawBits(),
        stats.min().toRawBits(),
        stats.max().toRawBits(),
        stats.populationVariance().toRawBits(),
        if (stats.count() > 1) stats.sampleVariance().toRawBits() else "single",
    )

    private fun statsSnapshot(stats: Stats): List<Any?> = listOf(
        stats.count(),
        stats.mean().toRawBits(),
        stats.sum().toRawBits(),
        stats.min().toRawBits(),
        stats.max().toRawBits(),
        stats.populationVariance().toRawBits(),
        if (stats.count() > 1) stats.sampleVariance().toRawBits() else "single",
    )
}
