package dev.guavakt.collect

/**
 * Guava MultimapBuilder — fluent factory for list/set multimaps (Kotlin collections under the hood).
 *
 * ```
 * MultimapBuilder.hashKeys().arrayListValues().build<String, Int>()
 * MultimapBuilder.linkedHashKeys().hashSetValues().build()
 * MultimapBuilder.treeKeys().treeSetValues().build()
 * ```
 */
object MultimapBuilder {
    fun hashKeys(): MultimapBuilderWithKeys = MultimapBuilderWithKeys(KeyKind.HASH)
    fun linkedHashKeys(): MultimapBuilderWithKeys = MultimapBuilderWithKeys(KeyKind.LINKED)
    fun treeKeys(): MultimapBuilderWithKeys = MultimapBuilderWithKeys(KeyKind.TREE)

    enum class KeyKind { HASH, LINKED, TREE }

    class MultimapBuilderWithKeys internal constructor(private val keyKind: KeyKind) {
        fun arrayListValues(): ListMultimapBuilder = ListMultimapBuilder(keyKind)
        fun linkedListValues(): ListMultimapBuilder = ListMultimapBuilder(keyKind, linkedValues = true)
        fun hashSetValues(): SetMultimapBuilder = SetMultimapBuilder(keyKind, ValueKind.HASH)
        fun linkedHashSetValues(): SetMultimapBuilder = SetMultimapBuilder(keyKind, ValueKind.LINKED)
        fun treeSetValues(): SetMultimapBuilder = SetMultimapBuilder(keyKind, ValueKind.TREE)
    }

    enum class ValueKind { HASH, LINKED, TREE }

    class ListMultimapBuilder internal constructor(
        private val keyKind: KeyKind,
        private val linkedValues: Boolean = false,
    ) {
        fun <K : Any, V> build(): ListMultimap<K, V> {
            val map: MutableMap<K, MutableCollection<V>> = when (keyKind) {
                KeyKind.HASH -> HashMap()
                KeyKind.LINKED -> LinkedHashMap()
                KeyKind.TREE -> {
                    @Suppress("UNCHECKED_CAST")
                    ComparatorTreeMap<Comparable<Any>, MutableCollection<V>>(null) as MutableMap<K, MutableCollection<V>>
                }
            }
            return Multimaps.newListMultimap(map) {
                if (linkedValues) mutableListOf() else ArrayList()
            }
        }
    }

    class SetMultimapBuilder internal constructor(
        private val keyKind: KeyKind,
        private val valueKind: ValueKind,
    ) {
        fun <K : Any, V> build(): SetMultimap<K, V> {
            val map: MutableMap<K, MutableCollection<V>> = when (keyKind) {
                KeyKind.HASH -> HashMap()
                KeyKind.LINKED -> LinkedHashMap()
                KeyKind.TREE -> {
                    @Suppress("UNCHECKED_CAST")
                    ComparatorTreeMap<Comparable<Any>, MutableCollection<V>>(null) as MutableMap<K, MutableCollection<V>>
                }
            }
            return Multimaps.newSetMultimap(map) {
                when (valueKind) {
                    ValueKind.HASH -> HashSet()
                    ValueKind.LINKED -> LinkedHashSet()
                    ValueKind.TREE -> {
                        @Suppress("UNCHECKED_CAST")
                        ComparatorTreeSet<Comparable<Any>>(null) as MutableSet<V>
                    }
                }
            }
        }
    }
}
