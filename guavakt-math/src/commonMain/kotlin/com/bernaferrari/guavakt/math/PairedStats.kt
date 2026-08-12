package com.bernaferrari.guavakt.math

/**
 * Guava PairedStats — immutable snapshot of bivariate statistics.
 */
class PairedStats internal constructor(
    private val xStats: Stats,
    private val yStats: Stats,
    private val sumOfProductsOfDeltas: Double,
) {
    fun count(): Long = xStats.count()
    fun xStats(): Stats = xStats
    fun yStats(): Stats = yStats
    fun populationCovariance(): Double {
        check(count() != 0L)
        return sumOfProductsOfDeltas / count()
    }
    fun sampleCovariance(): Double {
        check(count() > 1L)
        return sumOfProductsOfDeltas / (count() - 1)
    }
    fun pearsonsCorrelationCoefficient(): Double {
        check(count() > 1L)
        if (sumOfProductsOfDeltas.isNaN()) return Double.NaN
        val xSumOfSquares = xStats.sumOfSquaresOfDeltas()
        val ySumOfSquares = yStats.sumOfSquaresOfDeltas()
        check(xSumOfSquares > 0.0)
        check(ySumOfSquares > 0.0)
        val product = xSumOfSquares * ySumOfSquares
        return ensureInUnitRange(sumOfProductsOfDeltas / kotlin.math.sqrt(ensurePositive(product)))
    }
    fun leastSquaresFit(): LinearTransformation {
        check(count() > 1L)
        if (sumOfProductsOfDeltas.isNaN()) return LinearTransformation.forNaN()
        val xSumOfSquares = xStats.sumOfSquaresOfDeltas()
        if (xSumOfSquares > 0.0) {
            return if (yStats.sumOfSquaresOfDeltas() > 0.0) {
                LinearTransformation.mapping(xStats.mean(), yStats.mean())
                    .withSlope(sumOfProductsOfDeltas / xSumOfSquares)
            } else {
                LinearTransformation.horizontal(yStats.mean())
            }
        }
        check(yStats.sumOfSquaresOfDeltas() > 0.0)
        return LinearTransformation.vertical(xStats.mean())
    }

    private fun ensureInUnitRange(v: Double): Double = v.coerceIn(-1.0, 1.0)
    private fun ensurePositive(v: Double): Double = if (v > 0.0) v else Double.MIN_VALUE

    internal fun sumOfProductsOfDeltas(): Double = sumOfProductsOfDeltas

    companion object {
        fun from(xStats: Stats, yStats: Stats, sumOfProductsOfDeltas: Double): PairedStats =
            PairedStats(xStats, yStats, sumOfProductsOfDeltas)
    }
}
