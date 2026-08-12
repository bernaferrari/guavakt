package com.bernaferrari.guavakt.collect

/** Guava ImmutableMapKeySet — read-only key set view of a map. */
class ImmutableMapKeySet<K> private constructor(
    private val delegate: Set<K>,
) : Set<K> by delegate {
    companion object {
        private val EMPTY = ImmutableMapKeySet<Nothing>(emptySet())

        @Suppress("UNCHECKED_CAST")
        fun <K> of(): ImmutableMapKeySet<K> = EMPTY as ImmutableMapKeySet<K>

        fun <K> copyOf(keys: Collection<K>): ImmutableMapKeySet<K> {
            if (keys.isEmpty()) return of()
            return ImmutableMapKeySet(LinkedHashSet(keys))
        }

        fun <K, V> fromMap(map: Map<out K, V>): ImmutableMapKeySet<K> = copyOf(map.keys)

        fun <K> create(): ImmutableMapKeySet<K> = of()
        fun <K> create(keys: Collection<out K>): ImmutableMapKeySet<K> = copyOf(keys.toList())
    }
}
