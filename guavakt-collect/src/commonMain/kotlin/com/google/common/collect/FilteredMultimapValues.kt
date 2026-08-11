package dev.guavakt.collect

/** Guava FilteredMultimapValues — list/set multimap (ArrayListMultimap / HashMultimap storage). */
open class FilteredMultimapValues<K, V> private constructor(
    private val backing: dev.guavakt.collect.ArrayListMultimap<K, V> = dev.guavakt.collect.ArrayListMultimap.create(),
) : dev.guavakt.collect.Multimap<K, V> by backing {
    companion object {
        fun <K, V> create(): FilteredMultimapValues<K, V> = FilteredMultimapValues()
        fun <K, V> create(multimap: dev.guavakt.collect.Multimap<out K, out V>): FilteredMultimapValues<K, V> {
            val m = FilteredMultimapValues<K, V>()
            m.backing.putAll(multimap)
            return m
        }
    }
}
