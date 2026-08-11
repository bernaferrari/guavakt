package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/** Immutable base for Guava-shaped multimaps. New Kotlin code should prefer the concrete subtype. */
abstract class ImmutableMultimap<K, V> : Multimap<K, V> {
    abstract override fun get(key: K): MutableCollection<V>
    abstract fun inverse(): ImmutableMultimap<V, K>

    final override fun put(key: K, value: V): Boolean = throw UnsupportedOperationException("ImmutableMultimap")
    final override fun putAll(key: K, values: Iterable<V>): Boolean = throw UnsupportedOperationException("ImmutableMultimap")
    final override fun putAll(multimap: Multimap<out K, out V>): Boolean = throw UnsupportedOperationException("ImmutableMultimap")
    final override fun remove(key: Any?, value: Any?): Boolean = throw UnsupportedOperationException("ImmutableMultimap")
    open override fun removeAll(key: Any?): Collection<V> = throw UnsupportedOperationException("ImmutableMultimap")
    open override fun replaceValues(key: K, values: Iterable<V>): Collection<V> =
        throw UnsupportedOperationException("ImmutableMultimap")
    final override fun clear(): Unit = throw UnsupportedOperationException("ImmutableMultimap")

    class Builder<K, V> {
        private val delegate = ImmutableListMultimap.Builder<K, V>()

        fun put(key: K, value: V): Builder<K, V> = apply { delegate.put(key, value) }
        fun put(entry: Map.Entry<out K, V>): Builder<K, V> = apply { delegate.put(entry) }
        fun putAll(key: K, values: Iterable<V>): Builder<K, V> = apply { delegate.putAll(key, values) }
        fun putAll(key: K, vararg values: V): Builder<K, V> = apply { delegate.putAll(key, *values) }
        fun putAll(multimap: Multimap<out K, out V>): Builder<K, V> = apply { delegate.putAll(multimap) }
        fun putAll(entries: Iterable<Map.Entry<out K, V>>): Builder<K, V> = apply { delegate.putAll(entries) }
        fun orderKeysBy(comparator: Comparator<in K>): Builder<K, V> = apply {
            delegate.orderKeysBy(Preconditions.checkNotNull(comparator))
        }
        fun orderValuesBy(comparator: Comparator<in V>): Builder<K, V> = apply {
            delegate.orderValuesBy(Preconditions.checkNotNull(comparator))
        }
        fun build(): ImmutableMultimap<K, V> = delegate.build()
    }

    companion object {
        fun <K, V> of(): ImmutableMultimap<K, V> = ImmutableListMultimap.of()
        fun <K, V> of(key: K, value: V): ImmutableMultimap<K, V> = ImmutableListMultimap.of(key, value)
        fun <K, V> builder(): Builder<K, V> = Builder()
        fun <K, V> copyOf(multimap: Multimap<out K, out V>): ImmutableMultimap<K, V> =
            if (multimap is ImmutableMultimap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                multimap as ImmutableMultimap<K, V>
            } else {
                ImmutableListMultimap.copyOf(multimap)
            }
    }
}
