package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible(serializable = true, emulated = true)
class ArrayListMultimap<K, V> private constructor(
    map: MutableMap<K, MutableCollection<V>> = LinkedHashMap(),
    private val expectedValuesPerKey: Int = 3,
) : AbstractListMultimap<K, V>(map) {

    override fun createCollection(): MutableList<V> =
        ArrayList(expectedValuesPerKey.coerceAtLeast(1))

    fun trimToSize() {
        // Trim each value list capacity when possible
        for (key in keySet().toList()) {
            val col = get(key)
            if (col is ArrayList<*>) {
                @Suppress("UNCHECKED_CAST")
                (col as ArrayList<V>).trimToSize()
            }
        }
    }

    companion object {
        fun <K, V> create(): ArrayListMultimap<K, V> = ArrayListMultimap()

        fun <K, V> create(expectedKeys: Int, expectedValuesPerKey: Int): ArrayListMultimap<K, V> {
            Preconditions.checkArgument(expectedKeys >= 0)
            Preconditions.checkArgument(expectedValuesPerKey >= 0)
            val map = LinkedHashMap<K, MutableCollection<V>>(
                MapsCapacity.capacity(expectedKeys).coerceAtLeast(16),
            )
            return ArrayListMultimap(map, expectedValuesPerKey)
        }

        fun <K, V> create(multimap: Multimap<out K, out V>): ArrayListMultimap<K, V> {
            val result = create<K, V>()
            result.putAll(multimap)
            return result
        }
    }
}
