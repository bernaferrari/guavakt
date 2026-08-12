package com.bernaferrari.guavakt.collect

/** Guava BaseImmutableMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class BaseImmutableMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): BaseImmutableMultimap<K, V> = BaseImmutableMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): BaseImmutableMultimap<K, V> {
            val m = BaseImmutableMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
