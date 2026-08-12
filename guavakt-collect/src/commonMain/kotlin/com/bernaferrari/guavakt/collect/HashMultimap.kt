package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

@GwtCompatible(serializable = true, emulated = true)
class HashMultimap<K, V> private constructor(
    map: MutableMap<K, MutableCollection<V>> = LinkedHashMap(),
    private val expectedValuesPerKey: Int = 2,
) : AbstractSetMultimap<K, V>(map) {

    override fun createCollection(): MutableSet<V> =
        HashSet(MapsCapacity.capacity(expectedValuesPerKey))

    companion object {
        fun <K, V> create(): HashMultimap<K, V> = HashMultimap()
        fun <K, V> create(expectedKeys: Int, expectedValuesPerKey: Int): HashMultimap<K, V> {
            Preconditions.checkArgument(expectedKeys >= 0)
            Preconditions.checkArgument(expectedValuesPerKey >= 0)
            return HashMultimap(
                LinkedHashMap(MapsCapacity.capacity(expectedKeys)),
                expectedValuesPerKey,
            )
        }
        fun <K, V> create(multimap: Multimap<out K, out V>): HashMultimap<K, V> =
            create<K, V>().also { it.putAll(multimap) }
    }
}
