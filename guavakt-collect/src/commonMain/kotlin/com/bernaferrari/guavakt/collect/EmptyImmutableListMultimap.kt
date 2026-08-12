package com.bernaferrari.guavakt.collect

/** Guava EmptyImmutableListMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class EmptyImmutableListMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): EmptyImmutableListMultimap<K, V> = EmptyImmutableListMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): EmptyImmutableListMultimap<K, V> {
            val m = EmptyImmutableListMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
