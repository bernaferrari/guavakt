package com.bernaferrari.guavakt.collect

/** Guava AbstractSortedKeySortedSetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class AbstractSortedKeySortedSetMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): AbstractSortedKeySortedSetMultimap<K, V> = AbstractSortedKeySortedSetMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): AbstractSortedKeySortedSetMultimap<K, V> {
            val m = AbstractSortedKeySortedSetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
