package dev.guavakt.math

import kotlin.math.sqrt

data class Stats(
    private val count: Long,
    private val mean: Double,
    private val sumOfSquaresOfDeltas: Double,
    private val min: Double,
    private val max: Double,
) {
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
    fun sumOfSquaresOfDeltas(): Double = sumOfSquaresOfDeltas

    companion object {
        fun of(values: Iterable<Number>): Stats {
            val acc = StatsAccumulator()
            acc.addAll(values)
            return acc.snapshot()
        }
        fun of(vararg values: Double): Stats = of(values.toList())
    }

    private fun ensureNonNegative(value: Double): Double = if (value >= 0.0) value else 0.0
}
