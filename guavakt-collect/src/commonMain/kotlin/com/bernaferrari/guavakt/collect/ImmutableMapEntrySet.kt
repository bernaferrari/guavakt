package com.bernaferrari.guavakt.collect

/**
 * Guava ImmutableMapEntrySet — read-only set of map entries (not a Map).
 */
class ImmutableMapEntrySet<K, V> private constructor(
    private val delegate: Set<Map.Entry<K, V>>,
) : Set<Map.Entry<K, V>> by delegate {

    companion object {
        private val EMPTY = ImmutableMapEntrySet<Nothing, Nothing>(emptySet())

        @Suppress("UNCHECKED_CAST")
        fun <K, V> of(): ImmutableMapEntrySet<K, V> = EMPTY as ImmutableMapEntrySet<K, V>

        fun <K, V> copyOf(map: Map<out K, V>): ImmutableMapEntrySet<K, V> {
            if (map.isEmpty()) return of()
            val entries = LinkedHashSet<Map.Entry<K, V>>()
            for ((k, v) in map) {
                entries.add(Maps.immutableEntry(k, v))
            }
            return ImmutableMapEntrySet(entries)
        }

        fun <K, V> create(): ImmutableMapEntrySet<K, V> = of()
        fun <K, V> create(map: Map<out K, V>): ImmutableMapEntrySet<K, V> = copyOf(map)
    }
}
