package com.bernaferrari.guavakt.collect

/** Guava ImmutableMapValues — read-only values collection of a map. */
class ImmutableMapValues<V> private constructor(
    private val delegate: Collection<V>,
) : Collection<V> by delegate {
    companion object {
        private val EMPTY = ImmutableMapValues<Nothing>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <V> of(): ImmutableMapValues<V> = EMPTY as ImmutableMapValues<V>

        fun <V> copyOf(values: Collection<V>): ImmutableMapValues<V> {
            if (values.isEmpty()) return of()
            return ImmutableMapValues(values.toList())
        }

        fun <K, V> fromMap(map: Map<out K, V>): ImmutableMapValues<V> = copyOf(map.values)

        fun <V> create(): ImmutableMapValues<V> = of()
        fun <V> create(values: Collection<out V>): ImmutableMapValues<V> = copyOf(values.toList())
    }
}
