package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible(serializable = true, emulated = true)
class LinkedHashMultimap<K, V> private constructor(
    map: MutableMap<K, MutableCollection<V>> = LinkedHashMap(),
    private val expectedValuesPerKey: Int = 2,
) : AbstractSetMultimap<K, V>(map) {

    override fun createCollection(): MutableSet<V> =
        LinkedHashSet(MapsCapacity.capacity(expectedValuesPerKey))

    companion object {
        fun <K, V> create(): LinkedHashMultimap<K, V> = LinkedHashMultimap()
        fun <K, V> create(expectedKeys: Int, expectedValuesPerKey: Int): LinkedHashMultimap<K, V> {
            Preconditions.checkArgument(expectedKeys >= 0)
            Preconditions.checkArgument(expectedValuesPerKey >= 0)
            return LinkedHashMultimap(
                LinkedHashMap(MapsCapacity.capacity(expectedKeys)),
                expectedValuesPerKey,
            )
        }
        fun <K, V> create(multimap: Multimap<out K, out V>): LinkedHashMultimap<K, V> =
            create<K, V>().also { it.putAll(multimap) }
    }
}
