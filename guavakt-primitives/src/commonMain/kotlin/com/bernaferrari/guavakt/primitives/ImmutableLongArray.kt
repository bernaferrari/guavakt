package com.bernaferrari.guavakt.primitives

class ImmutableLongArray private constructor(private val array: LongArray) {
    fun length(): Int = array.size
    fun isEmpty(): Boolean = array.isEmpty()
    fun get(index: Int): Long = array[index]
    fun toArray(): LongArray = array.copyOf()
    fun contains(target: Long): Boolean = array.any { it == target }
    fun indexOf(target: Long): Int = array.indexOf(target)
    fun lastIndexOf(target: Long): Int = array.lastIndexOf(target)
    fun asList(): List<Long> = array.toList()
    override fun equals(other: Any?): Boolean =
        other is ImmutableLongArray && array.contentEquals(other.array)
    override fun hashCode(): Int = array.contentHashCode()

    companion object {
        private val EMPTY = ImmutableLongArray(LongArray(0))
        fun of(): ImmutableLongArray = EMPTY
        fun of(e0: Long): ImmutableLongArray = ImmutableLongArray(LongArray(1) { e0 })
        fun of(e0: Long, e1: Long): ImmutableLongArray = ImmutableLongArray(LongArray(2) { if (it == 0) e0 else e1 })
        fun copyOf(values: LongArray): ImmutableLongArray =
            if (values.isEmpty()) of() else ImmutableLongArray(values.copyOf())
        fun copyOf(values: Collection<Long>): ImmutableLongArray {
            if (values.isEmpty()) return of()
            val a = LongArray(values.size)
            var i = 0
            for (v in values) a[i++] = v
            return ImmutableLongArray(a)
        }
        fun builder(): Builder = Builder()
        fun builder(initialCapacity: Int): Builder = Builder(initialCapacity)
    }

    class Builder(initialCapacity: Int = 10) {
        private var array = LongArray(initialCapacity.coerceAtLeast(1))
        private var count = 0
        fun add(value: Long): Builder = apply {
            ensure(count + 1)
            array[count++] = value
        }
        fun addAll(values: LongArray): Builder = apply { for (v in values) add(v) }
        fun addAll(values: Iterable<Long>): Builder = apply { for (v in values) add(v) }
        fun build(): ImmutableLongArray =
            if (count == 0) of() else ImmutableLongArray(array.copyOf(count))
        private fun ensure(min: Int) {
            if (min > array.size) {
                val n = maxOf(array.size * 2, min)
                array = array.copyOf(n)
            }
        }
    }
}
