package com.bernaferrari.guavakt.collect

/** Guava EmptyImmutableSetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class EmptyImmutableSetMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): EmptyImmutableSetMultimap<K, V> = EmptyImmutableSetMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): EmptyImmutableSetMultimap<K, V> {
            val m = EmptyImmutableSetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
