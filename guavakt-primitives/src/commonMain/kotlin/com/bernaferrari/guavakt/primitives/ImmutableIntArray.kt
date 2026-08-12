package com.bernaferrari.guavakt.primitives

class ImmutableIntArray private constructor(private val array: IntArray) {
    fun length(): Int = array.size
    fun isEmpty(): Boolean = array.isEmpty()
    fun get(index: Int): Int = array[index]
    fun toArray(): IntArray = array.copyOf()
    fun contains(target: Int): Boolean = array.any { it == target }
    fun indexOf(target: Int): Int = array.indexOf(target)
    fun lastIndexOf(target: Int): Int = array.lastIndexOf(target)
    fun asList(): List<Int> = array.toList()
    override fun equals(other: Any?): Boolean =
        other is ImmutableIntArray && array.contentEquals(other.array)
    override fun hashCode(): Int = array.contentHashCode()

    companion object {
        private val EMPTY = ImmutableIntArray(IntArray(0))
        fun of(): ImmutableIntArray = EMPTY
        fun of(e0: Int): ImmutableIntArray = ImmutableIntArray(IntArray(1) { e0 })
        fun of(e0: Int, e1: Int): ImmutableIntArray = ImmutableIntArray(IntArray(2) { if (it == 0) e0 else e1 })
        fun copyOf(values: IntArray): ImmutableIntArray =
            if (values.isEmpty()) of() else ImmutableIntArray(values.copyOf())
        fun copyOf(values: Collection<Int>): ImmutableIntArray {
            if (values.isEmpty()) return of()
            val a = IntArray(values.size)
            var i = 0
            for (v in values) a[i++] = v
            return ImmutableIntArray(a)
        }
        fun builder(): Builder = Builder()
        fun builder(initialCapacity: Int): Builder = Builder(initialCapacity)
    }

    class Builder(initialCapacity: Int = 10) {
        private var array = IntArray(initialCapacity.coerceAtLeast(1))
        private var count = 0
        fun add(value: Int): Builder = apply {
            ensure(count + 1)
            array[count++] = value
        }
        fun addAll(values: IntArray): Builder = apply { for (v in values) add(v) }
        fun addAll(values: Iterable<Int>): Builder = apply { for (v in values) add(v) }
        fun build(): ImmutableIntArray =
            if (count == 0) of() else ImmutableIntArray(array.copyOf(count))
        private fun ensure(min: Int) {
            if (min > array.size) {
                val n = maxOf(array.size * 2, min)
                array = array.copyOf(n)
            }
        }
    }
}
