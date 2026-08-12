package com.bernaferrari.guavakt.collect

/** Guava FilteredKeySetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredKeySetMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredKeySetMultimap<K, V> = FilteredKeySetMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): FilteredKeySetMultimap<K, V> {
            val m = FilteredKeySetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
