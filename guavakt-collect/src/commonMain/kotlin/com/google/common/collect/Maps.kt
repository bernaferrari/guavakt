package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

/** Guava Maps factories — **thin aliases** to [HashMap] / [LinkedHashMap] / [ComparatorTreeMap]. Prefer `hashMapOf` / `mutableMapOf`; use GuavaKt for Guava-only views. */
@GwtCompatible
object Maps {
    fun <K, V> newHashMap(): MutableMap<K, V> = HashMap()
    fun <K, V> newHashMap(map: Map<out K, V>): MutableMap<K, V> = HashMap(map)
    fun <K, V> newHashMapWithExpectedSize(expectedSize: Int): MutableMap<K, V> {
        Preconditions.checkArgument(expectedSize >= 0)
        return HashMap(capacity(expectedSize))
    }

    fun <K, V> newLinkedHashMap(): MutableMap<K, V> = LinkedHashMap()
    fun <K, V> newLinkedHashMap(map: Map<out K, V>): MutableMap<K, V> = LinkedHashMap(map)
    fun <K, V> newLinkedHashMapWithExpectedSize(expectedSize: Int): MutableMap<K, V> {
        Preconditions.checkArgument(expectedSize >= 0)
        return LinkedHashMap(capacity(expectedSize))
    }

    /** Sorted map with natural key order (portable [ComparatorTreeMap]). */
    fun <K : Comparable<K>, V> newTreeMap(): MutableMap<K, V> = ComparatorTreeMap(null)
    fun <C, K : C, V> newTreeMap(comparator: Comparator<C>): MutableMap<K, V> = ComparatorTreeMap(comparator)
    fun <K : Comparable<K>, V> newTreeMap(map: Map<out K, V>): MutableMap<K, V> =
        ComparatorTreeMap<K, V>(null).also { it.putAll(map) }

    /**
     * Identity-semantic map on JVM-capable runtimes; on pure KMP uses reference equality via
     * [IdentityHashMapKmp] (=== / identityHashCode), not [Any.equals].
     */
    fun <K, V> newIdentityHashMap(): MutableMap<K, V> = IdentityHashMapKmp()

    fun <K, V> immutableEntry(key: K, value: V): Map.Entry<K, V> {
        val k = key
        val v = value
        return object : MutableMap.MutableEntry<K, V> {
            override val key: K get() = k
            override val value: V get() = v
            override fun setValue(newValue: V): V = throw UnsupportedOperationException("immutable entry")
            override fun equals(other: Any?): Boolean =
                other is Map.Entry<*, *> && key == other.key && value == other.value
            override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
            override fun toString(): String = "$key=$value"
        }
    }

    fun <K, V1, V2> transformValues(fromMap: Map<K, V1>, function: (V1) -> V2): Map<K, V2> =
        fromMap.mapValues { function(it.value) }

    fun <K1, K2, V> transformEntries(
        fromMap: Map<K1, V>,
        transformer: (K1, V) -> Pair<K2, V>,
    ): Map<K2, V> {
        val out = LinkedHashMap<K2, V>(fromMap.size)
        for ((k, v) in fromMap) {
            val (nk, nv) = transformer(k, v)
            out[nk] = nv
        }
        return out
    }

    /**
     * Live filtered view when [unfiltered] is a [MutableMap]; otherwise a stdlib snapshot.
     * Puts with rejected keys throw [IllegalArgumentException].
     */
    fun <K, V> filterKeys(unfiltered: Map<K, V>, keyPredicate: (K) -> Boolean): Map<K, V> =
        if (unfiltered is MutableMap<K, V>) FilteredKeyMapView(unfiltered, keyPredicate)
        else unfiltered.filterKeys(keyPredicate)

    fun <K, V> filterValues(unfiltered: Map<K, V>, valuePredicate: (V) -> Boolean): Map<K, V> =
        if (unfiltered is MutableMap<K, V>) FilteredValueMapView(unfiltered, valuePredicate)
        else unfiltered.filterValues(valuePredicate)

    fun <K, V> filterEntries(unfiltered: Map<K, V>, entryPredicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> =
        if (unfiltered is MutableMap<K, V>) FilteredEntryMapView(unfiltered, entryPredicate)
        else unfiltered.filter { entryPredicate(it) }

    /** Prefer returning a Kotlin [Map]; historically Guava returned ImmutableMap. */
    fun <K, V> uniqueIndex(values: Iterable<V>, keyFunction: (V) -> K): Map<K, V> {
        val map = LinkedHashMap<K, V>()
        for (value in values) {
            val key = keyFunction(value)
            val prev = map.put(key, value)
            require(prev == null) { "Duplicate key: $key" }
        }
        return map
    }

    fun <K, V> asMap(set: Set<K>, function: (K) -> V): Map<K, V> {
        val map = LinkedHashMap<K, V>(set.size)
        for (k in set) map[k] = function(k)
        return map
    }

    fun <K, V> toMap(keys: Iterable<K>, valueFunction: (K) -> V): Map<K, V> {
        val map = LinkedHashMap<K, V>()
        for (k in keys) map[k] = valueFunction(k)
        return map
    }

    fun <K, V> difference(left: Map<out K, V>, right: Map<out K, *>): MapDifference<K, V> =
        MapDifferenceImpl.compute(left, right)

    /**
     * Comparator-ordered [SortedMapDifference] snapshot.
     *
     * This is Kotlin Multiplatform's equivalent of Guava's `difference(SortedMap, Map)`: callers
     * supply the ordering explicitly because common Kotlin has no `SortedMap` interface.
     */
    fun <K, V> difference(
        left: Map<out K, V>,
        right: Map<out K, *>,
        comparator: Comparator<in K>,
    ): SortedMapDifference<K, V> = SortedMapDifferenceImpl.compute(left, right, comparator)

    fun <K, V> unmodifiableMap(map: Map<out K, V>): Map<K, V> = map.toMap()

    fun fromProperties(props: Map<String, String>): Map<String, String> = props.toMap()

    /** Key set of a map as a live view when possible; otherwise a snapshot. */
    fun <K, V> keySet(map: Map<K, V>): Set<K> = map.keys

    fun <K, V> values(map: Map<K, V>): Collection<V> = map.values

    fun <K, V> entrySet(map: Map<K, V>): Set<Map.Entry<K, V>> = map.entries

    private fun capacity(expectedSize: Int): Int {
        if (expectedSize < 3) {
            CollectPreconditions.checkNonnegative(expectedSize, "expectedSize")
            return expectedSize + 1
        }
        if (expectedSize < Int.MAX_VALUE / 2) return expectedSize + expectedSize / 3
        return Int.MAX_VALUE
    }
}
