package com.bernaferrari.guavakt.collect

/** Guava AbstractMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class AbstractMultimap<K, V> private constructor(
    private val backing: com.bernaferrari.guavakt.collect.ArrayListMultimap<K, V> = com.bernaferrari.guavakt.collect.ArrayListMultimap.create(),
) : com.bernaferrari.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): AbstractMultimap<K, V> = AbstractMultimap()
        fun <K, V> create(multimap: com.bernaferrari.guavakt.collect.Multimap<out K, out V>): AbstractMultimap<K, V> {
            val m = AbstractMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
