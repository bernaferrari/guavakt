package com.bernaferrari.guavakt.math

import com.bernaferrari.guavakt.base.Preconditions

/**
 * Guava Quantiles — fluent quantile computation (Type 7 / Excel / R definition).
 */
object Quantiles {
    fun median(): ScaleAndIndex = scale(2).index(1)
    fun quartiles(): Scale = scale(4)
    fun percentiles(): Scale = scale(100)

    fun scale(scale: Int): Scale {
        Preconditions.checkArgument(scale > 0, "Quantile scale must be positive")
        return Scale(scale)
    }

    class Scale internal constructor(private val scale: Int) {
        fun index(index: Int): ScaleAndIndex {
            Preconditions.checkArgument(index in 0..scale)
            return ScaleAndIndex(scale, index)
        }

        fun indexes(vararg indexes: Int): ScaleAndIndexes {
            Preconditions.checkArgument(indexes.isNotEmpty())
            for (i in indexes) Preconditions.checkArgument(i in 0..scale)
            return ScaleAndIndexes(scale, indexes.copyOf())
        }

        fun indexes(indexes: Collection<Int>): ScaleAndIndexes =
            indexes(*indexes.toIntArray())
    }

    class ScaleAndIndex internal constructor(
        private val scale: Int,
        private val index: Int,
    ) {
        fun compute(dataset: DoubleArray): Double = computeInPlace(dataset.copyOf())
        fun compute(dataset: Collection<Double>): Double =
            compute(dataset.toDoubleArray())
        fun compute(dataset: IntArray): Double =
            compute(DoubleArray(dataset.size) { dataset[it].toDouble() })
        fun compute(dataset: LongArray): Double =
            compute(DoubleArray(dataset.size) { dataset[it].toDouble() })

        fun computeInPlace(dataset: DoubleArray): Double {
            Preconditions.checkArgument(dataset.isNotEmpty())
            if (dataset.any { it.isNaN() }) return Double.NaN
            return interpolate(dataset, scale, index)
        }
    }

    class ScaleAndIndexes internal constructor(
        private val scale: Int,
        private val indexes: IntArray,
    ) {
        fun compute(dataset: DoubleArray): Map<Int, Double> = computeInPlace(dataset.copyOf())
        fun compute(dataset: Collection<Double>): Map<Int, Double> =
            compute(dataset.toDoubleArray())

        fun computeInPlace(dataset: DoubleArray): Map<Int, Double> {
            Preconditions.checkArgument(dataset.isNotEmpty())
            if (dataset.any { it.isNaN() }) return indexes.associateWith { Double.NaN }
            val result = LinkedHashMap<Int, Double>()
            for (idx in indexes) {
                result[idx] = interpolate(dataset.copyOf(), scale, idx)
            }
            return result
        }
    }

    private fun interpolate(dataset: DoubleArray, scale: Int, index: Int): Double {
        dataset.sort()
        val lastIndex = dataset.lastIndex
        if (index == 0) return dataset[0]
        if (index == scale) return dataset[lastIndex]

        val numerator = index.toLong() * lastIndex
        val lowerIndex = (numerator / scale).toInt()
        val remainder = (numerator - lowerIndex.toLong() * scale).toInt()
        if (remainder == 0) return dataset[lowerIndex]
        return interpolate(dataset[lowerIndex], dataset[lowerIndex + 1], remainder, scale)
    }

    private fun interpolate(lower: Double, upper: Double, remainder: Int, scale: Int): Double = when {
        lower == Double.NEGATIVE_INFINITY ->
            if (upper == Double.POSITIVE_INFINITY) Double.NaN else Double.NEGATIVE_INFINITY
        upper == Double.POSITIVE_INFINITY -> Double.POSITIVE_INFINITY
        else -> lower + (upper - lower) * remainder / scale
    }
}
