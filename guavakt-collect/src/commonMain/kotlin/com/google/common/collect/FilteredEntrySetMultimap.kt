package dev.guavakt.collect

/** Guava FilteredEntrySetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredEntrySetMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredEntrySetMultimap<K, V> = FilteredEntrySetMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredEntrySetMultimap<K, V> {
            val m = FilteredEntrySetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
