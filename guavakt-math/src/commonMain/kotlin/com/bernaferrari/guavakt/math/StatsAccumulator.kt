package com.bernaferrari.guavakt.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class StatsAccumulator {
    private var count = 0L
    private var mean = 0.0
    private var sumOfSquaresOfDeltas = 0.0
    private var min = Double.NaN
    private var max = Double.NaN

    fun add(value: Double) {
        if (count == 0L) {
            count = 1
            mean = value
            min = value
            max = value
            sumOfSquaresOfDeltas = if (value.isFinite()) 0.0 else Double.NaN
        } else {
            count++
            if (value.isFinite() && mean.isFinite()) {
                val delta = value - mean
                mean += delta / count
                sumOfSquaresOfDeltas += delta * (value - mean)
            } else {
                mean = calculateNewMeanNonFinite(mean, value)
                sumOfSquaresOfDeltas = Double.NaN
            }
            min = min(min, value)
            max = max(max, value)
        }
    }

    fun addAll(values: Iterable<Number>) {
        for (v in values) add(v.toDouble())
    }

    fun addAll(values: Stats) {
        if (values.count() == 0L) return
        if (count == 0L) {
            count = values.count()
            mean = values.mean()
            min = values.min()
            max = values.max()
            sumOfSquaresOfDeltas = values.sumOfSquaresOfDeltas()
        } else {
            val combinedCount = count + values.count()
            if (mean.isFinite() && values.mean().isFinite()) {
                val delta = values.mean() - mean
                mean += delta * values.count() / combinedCount
                sumOfSquaresOfDeltas += values.sumOfSquaresOfDeltas() +
                    delta * (values.mean() - mean) * values.count()
            } else {
                mean = calculateNewMeanNonFinite(mean, values.mean())
                sumOfSquaresOfDeltas = Double.NaN
            }
            count = combinedCount
            min = min(min, values.min())
            max = max(max, values.max())
        }
    }

    fun count(): Long = count
    fun mean(): Double { check(count > 0); return mean }
    fun sum(): Double = mean * count
    fun min(): Double { check(count > 0); return min }
    fun max(): Double { check(count > 0); return max }
    fun populationVariance(): Double {
        check(count > 0)
        if (sumOfSquaresOfDeltas.isNaN()) return Double.NaN
        return if (count == 1L) 0.0 else ensureNonNegative(sumOfSquaresOfDeltas) / count
    }
    fun populationStandardDeviation(): Double = sqrt(populationVariance())
    fun sampleVariance(): Double {
        check(count > 1)
        if (sumOfSquaresOfDeltas.isNaN()) return Double.NaN
        return ensureNonNegative(sumOfSquaresOfDeltas) / (count - 1)
    }
    fun sampleStandardDeviation(): Double = sqrt(sampleVariance())
    fun snapshot(): Stats = Stats(count, mean, sumOfSquaresOfDeltas, min, max)

    private fun calculateNewMeanNonFinite(previousMean: Double, value: Double): Double = when {
        previousMean.isFinite() -> value
        value.isFinite() || previousMean == value -> previousMean
        else -> Double.NaN
    }

    private fun ensureNonNegative(value: Double): Double = if (value >= 0.0) value else 0.0
}
