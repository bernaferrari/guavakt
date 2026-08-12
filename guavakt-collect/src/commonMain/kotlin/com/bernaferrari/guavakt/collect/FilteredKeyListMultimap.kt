package com.bernaferrari.guavakt.collect

/** Guava FilteredKeyListMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredKeyListMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredKeyListMultimap<K, V> = FilteredKeyListMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): FilteredKeyListMultimap<K, V> {
            val m = FilteredKeyListMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
