package dev.guavakt.primitives

class ImmutableDoubleArray private constructor(private val array: DoubleArray) {
    fun length(): Int = array.size
    fun isEmpty(): Boolean = array.isEmpty()
    fun get(index: Int): Double = array[index]
    fun toArray(): DoubleArray = array.copyOf()
    fun contains(target: Double): Boolean = array.any { it == target }
    fun indexOf(target: Double): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }
    fun lastIndexOf(target: Double): Int {
        for (i in array.indices.reversed()) if (array[i] == target) return i
        return -1
    }
    fun asList(): List<Double> = array.toList()
    override fun equals(other: Any?): Boolean =
        other is ImmutableDoubleArray && array.contentEquals(other.array)
    override fun hashCode(): Int = array.contentHashCode()

    companion object {
        private val EMPTY = ImmutableDoubleArray(DoubleArray(0))
        fun of(): ImmutableDoubleArray = EMPTY
        fun of(e0: Double): ImmutableDoubleArray = ImmutableDoubleArray(DoubleArray(1) { e0 })
        fun of(e0: Double, e1: Double): ImmutableDoubleArray = ImmutableDoubleArray(DoubleArray(2) { if (it == 0) e0 else e1 })
        fun copyOf(values: DoubleArray): ImmutableDoubleArray =
            if (values.isEmpty()) of() else ImmutableDoubleArray(values.copyOf())
        fun copyOf(values: Collection<Double>): ImmutableDoubleArray {
            if (values.isEmpty()) return of()
            val a = DoubleArray(values.size)
            var i = 0
            for (v in values) a[i++] = v
            return ImmutableDoubleArray(a)
        }
        fun builder(): Builder = Builder()
        fun builder(initialCapacity: Int): Builder = Builder(initialCapacity)
    }

    class Builder(initialCapacity: Int = 10) {
        private var array = DoubleArray(initialCapacity.coerceAtLeast(1))
        private var count = 0
        fun add(value: Double): Builder = apply {
            ensure(count + 1)
            array[count++] = value
        }
        fun addAll(values: DoubleArray): Builder = apply { for (v in values) add(v) }
        fun addAll(values: Iterable<Double>): Builder = apply { for (v in values) add(v) }
        fun build(): ImmutableDoubleArray =
            if (count == 0) of() else ImmutableDoubleArray(array.copyOf(count))
        private fun ensure(min: Int) {
            if (min > array.size) {
                val n = maxOf(array.size * 2, min)
                array = array.copyOf(n)
            }
        }
    }
}
