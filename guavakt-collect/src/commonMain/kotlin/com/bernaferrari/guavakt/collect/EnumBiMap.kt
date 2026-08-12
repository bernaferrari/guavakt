package com.bernaferrari.guavakt.collect

/**
 * Guava EnumBiMap — bidirectional map (enum keys/values in Guava; here any K/V with BiMap semantics).
 */
class EnumBiMap<K, V> private constructor(
    forward: MutableMap<K, V> = LinkedHashMap(),
    backward: MutableMap<V, K> = LinkedHashMap(),
) : AbstractBiMap<K, V>(forward, backward) {
    companion object {
        fun <K, V> create(): EnumBiMap<K, V> = EnumBiMap()
        fun <K, V> create(map: Map<out K, V>): EnumBiMap<K, V> =
            create<K, V>().also { for ((k, v) in map) it.put(k, v) }
    }
}
