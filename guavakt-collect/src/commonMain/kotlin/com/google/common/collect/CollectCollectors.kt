package dev.guavakt.collect

/**
 * Guava CollectCollectors — factory helpers analogous to Java Collectors for Guava types.
 * On KMP we expose builder functions rather than java.util.stream.Collector.
 */
object CollectCollectors {
    fun <E> toImmutableList(): (Iterable<E>) -> List<E> = { it.toList() }
    fun <E> toImmutableSet(): (Iterable<E>) -> Set<E> = { it.toSet() }
    fun <T, K, V> toImmutableMap(keyFn: (T) -> K, valueFn: (T) -> V): (Iterable<T>) -> Map<K, V> =
        { items -> items.associate { keyFn(it) to valueFn(it) } }
    fun <T, K> toImmutableMultiset(elementFn: (T) -> K): (Iterable<T>) -> Multiset<K> = { items ->
        val ms = HashMultiset.create<K>()
        for (item in items) ms.add(elementFn(item))
        ms
    }
}
