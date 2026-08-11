package dev.guavakt.collect

/** Guava FilteredSetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredSetMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredSetMultimap<K, V> = FilteredSetMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredSetMultimap<K, V> {
            val m = FilteredSetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
