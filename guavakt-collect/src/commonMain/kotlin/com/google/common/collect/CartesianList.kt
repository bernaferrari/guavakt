package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Guava CartesianList — immutable list of all tuples from axis lists (cartesian product).
 */
class CartesianList<E> private constructor(
    private val axes: List<List<E>>,
    private val axesSizeProduct: IntArray,
) : AbstractList<List<E>>() {
    override val size: Int get() = axesSizeProduct[0]

    override fun get(index: Int): List<E> {
        Preconditions.checkElementIndex(index, size)
        val result = ArrayList<E>(axes.size)
        var idx = index
        for (i in axes.indices) {
            val axis = axes[i]
            val axisSize = axis.size
            // product of sizes of axes after i is axesSizeProduct[i+1]
            val divisor = axesSizeProduct[i + 1]
            result.add(axis[idx / divisor])
            idx %= divisor
        }
        return result
    }

    companion object {
        fun <E> create(lists: List<List<E>>): List<List<E>> {
            val axes = ArrayList<List<E>>(lists.size)
            for (list in lists) {
                val copy = list.toList()
                if (copy.isEmpty()) return emptyList()
                axes.add(copy)
            }
            val product = IntArray(axes.size + 1)
            product[axes.size] = 1
            for (i in axes.indices.reversed()) {
                val next = product[i + 1].toLong() * axes[i].size
                if (next > Int.MAX_VALUE) throw IllegalArgumentException("cartesian product too large")
                product[i] = next.toInt()
            }
            return CartesianList(axes, product)
        }
    }
}
