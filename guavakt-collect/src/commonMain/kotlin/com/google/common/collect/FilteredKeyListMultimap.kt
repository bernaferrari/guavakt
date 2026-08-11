package dev.guavakt.collect

/** Guava FilteredKeyListMultimap — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredKeyListMultimap<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredKeyListMultimap<K, V> = FilteredKeyListMultimap()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredKeyListMultimap<K, V> {
            val m = FilteredKeyListMultimap<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
