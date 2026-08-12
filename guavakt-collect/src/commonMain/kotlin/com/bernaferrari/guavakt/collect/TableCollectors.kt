package com.bernaferrari.guavakt.collect

/** Guava TableCollectors — collect cells into ImmutableTable. */
object TableCollectors {
    fun <T, R, C, V> toImmutableTable(
        rowFn: (T) -> R,
        columnFn: (T) -> C,
        valueFn: (T) -> V,
    ): (Iterable<T>) -> ImmutableTable<R, C, V> = { items ->
        val b = ImmutableTable.builder<R, C, V>()
        for (item in items) b.put(rowFn(item), columnFn(item), valueFn(item))
        b.build()
    }

    fun <T, R, C, V> toImmutableTable(
        rowFn: (T) -> R,
        columnFn: (T) -> C,
        valueFn: (T) -> V,
        mergeFn: (V, V) -> V,
    ): (Iterable<T>) -> ImmutableTable<R, C, V> = { items ->
        val acc = HashBasedTable.create<R, C, V>()
        for (item in items) {
            val r = rowFn(item); val c = columnFn(item); val v = valueFn(item)
            val old = acc.get(r, c)
            acc.put(r, c, if (old == null) v else mergeFn(old, v))
        }
        ImmutableTable.copyOf(acc)
    }
}
