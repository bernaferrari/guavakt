package dev.guavakt.collect

/** Guava EmptyImmutableListMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class EmptyImmutableListMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): EmptyImmutableListMultimap<K, V> = EmptyImmutableListMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): EmptyImmutableListMultimap<K, V> {
            val m = EmptyImmutableListMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
