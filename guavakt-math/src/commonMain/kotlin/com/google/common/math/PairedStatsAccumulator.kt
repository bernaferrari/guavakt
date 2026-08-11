package dev.guavakt.math

/** Guava PairedStatsAccumulator — accumulates bivariate stats online. */
class PairedStatsAccumulator {
    private val xStats = StatsAccumulator()
    private val yStats = StatsAccumulator()
    private var sumOfProductsOfDeltas = 0.0

    fun add(x: Double, y: Double) {
        xStats.add(x)
        if (x.isFinite() && y.isFinite() && xStats.count() > 1L) {
            sumOfProductsOfDeltas += (x - xStats.mean()) * (y - yStats.mean())
        } else if (!x.isFinite() || !y.isFinite()) {
            sumOfProductsOfDeltas = Double.NaN
        }
        yStats.add(y)
    }

    fun addAll(values: PairedStats) {
        if (values.count() == 0L) return
        xStats.addAll(values.xStats())
        if (yStats.count() == 0L) {
            sumOfProductsOfDeltas = values.sumOfProductsOfDeltas()
        } else {
            sumOfProductsOfDeltas += values.sumOfProductsOfDeltas() +
                (values.xStats().mean() - xStats.mean()) *
                (values.yStats().mean() - yStats.mean()) *
                values.count()
        }
        yStats.addAll(values.yStats())
    }

    fun snapshot(): PairedStats =
        PairedStats.from(xStats.snapshot(), yStats.snapshot(), sumOfProductsOfDeltas)

    fun count(): Long = xStats.count()
    fun populationCovariance(): Double = snapshot().populationCovariance()
}
