package com.bernaferrari.guavakt.collect

/** Guava EnumHashBiMap — BiMap with enum-oriented API shape (portable HashBiMap storage). */
class EnumHashBiMap<K, V> private constructor(
    forward: MutableMap<K, V> = LinkedHashMap(),
    backward: MutableMap<V, K> = LinkedHashMap(),
) : AbstractBiMap<K, V>(forward, backward) {
    companion object {
        fun <K, V> create(): EnumHashBiMap<K, V> = EnumHashBiMap()
        fun <K, V> create(map: Map<out K, V>): EnumHashBiMap<K, V> =
            create<K, V>().also { for ((k, v) in map) it.put(k, v) }
    }
}
