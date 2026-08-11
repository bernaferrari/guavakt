package dev.guavakt.collect

/** Guava FilteredEntryMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredEntryMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredEntryMultimap<K, V> = FilteredEntryMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredEntryMultimap<K, V> {
            val m = FilteredEntryMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
