package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible

/**
 * Guava TreeMultimap — sorted keys and sorted value sets (portable trees).
 */
@GwtCompatible(serializable = true, emulated = true)
class TreeMultimap<K, V> private constructor(
    private val keyComparator: Comparator<in K>?,
    private val valueComparator: Comparator<in V>?,
    map: MutableMap<K, MutableCollection<V>> = ComparatorTreeMap(keyComparator),
) : AbstractSetMultimap<K, V>(map), SortedSetMultimap<K, V> {

    override fun createCollection(): MutableSet<V> = ComparatorTreeSet(valueComparator)

    override fun get(key: K): MutableSet<V> = super.get(key)

    override fun valueComparator(): Comparator<in V>? = valueComparator

    fun keyComparator(): Comparator<in K>? = keyComparator

    companion object {
        fun <K : Comparable<K>, V : Comparable<V>> create(): TreeMultimap<K, V> =
            TreeMultimap(null, null)

        fun <K, V> create(
            keyComparator: Comparator<in K>?,
            valueComparator: Comparator<in V>?,
        ): TreeMultimap<K, V> = TreeMultimap(keyComparator, valueComparator)

        fun <K : Comparable<K>, V : Comparable<V>> create(
            multimap: Multimap<out K, out V>,
        ): TreeMultimap<K, V> = create<K, V>().also { it.putAll(multimap) }
    }
}
