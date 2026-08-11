package dev.guavakt.collect

/** Guava FilteredKeyMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredKeyMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredKeyMultimap<K, V> = FilteredKeyMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredKeyMultimap<K, V> {
            val m = FilteredKeyMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
