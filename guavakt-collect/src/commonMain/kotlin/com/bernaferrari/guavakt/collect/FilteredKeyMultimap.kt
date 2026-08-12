package com.bernaferrari.guavakt.collect

/** Guava FilteredKeyMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredKeyMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredKeyMultimap<K, V> = FilteredKeyMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): FilteredKeyMultimap<K, V> {
            val m = FilteredKeyMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
