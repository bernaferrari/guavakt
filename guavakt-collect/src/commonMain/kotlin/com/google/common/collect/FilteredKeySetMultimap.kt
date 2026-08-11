package dev.guavakt.collect

/** Guava FilteredKeySetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredKeySetMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredKeySetMultimap<K, V> = FilteredKeySetMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredKeySetMultimap<K, V> {
            val m = FilteredKeySetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
