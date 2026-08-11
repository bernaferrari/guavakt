package dev.guavakt.collect

/** Guava EmptyImmutableSetMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class EmptyImmutableSetMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): EmptyImmutableSetMultimap<K, V> = EmptyImmutableSetMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): EmptyImmutableSetMultimap<K, V> {
            val m = EmptyImmutableSetMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
