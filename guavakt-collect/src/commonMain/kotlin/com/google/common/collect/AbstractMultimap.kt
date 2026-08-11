package dev.guavakt.collect

/** Guava AbstractMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class AbstractMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): AbstractMultimap<K, V> = AbstractMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): AbstractMultimap<K, V> {
            val m = AbstractMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
