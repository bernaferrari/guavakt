package com.bernaferrari.guavakt.collect

/** Guava FilteredEntryMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredEntryMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredEntryMultimap<K, V> = FilteredEntryMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): FilteredEntryMultimap<K, V> {
            val m = FilteredEntryMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
