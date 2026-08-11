package dev.guavakt.collect

/**
 * Guava ImmutableEnumMap — read-only map (enum keys preferred; storage is ordered snapshot).
 */
class ImmutableEnumMap<K, V> private constructor(
    private val delegate: Map<K, V>,
) : Map<K, V> by delegate {

    companion object {
        private val EMPTY = ImmutableEnumMap<Nothing, Nothing>(emptyMap())

        @Suppress("UNCHECKED_CAST")
        fun <K, V> of(): ImmutableEnumMap<K, V> = EMPTY as ImmutableEnumMap<K, V>

        fun <K, V> copyOf(map: Map<out K, V>): ImmutableEnumMap<K, V> {
            if (map is ImmutableEnumMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return map as ImmutableEnumMap<K, V>
            }
            if (map.isEmpty()) return of()
            return ImmutableEnumMap(LinkedHashMap(map))
        }

        /** @deprecated Prefer [copyOf]; empty map is immutable. */
        fun <K, V> create(): ImmutableEnumMap<K, V> = of()

        fun <K, V> create(map: Map<out K, V>): ImmutableEnumMap<K, V> = copyOf(map)
    }
}
