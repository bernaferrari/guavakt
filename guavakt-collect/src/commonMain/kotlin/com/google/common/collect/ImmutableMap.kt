package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions


/**
 * Guava ImmutableMap — **thin interop shim** over Kotlin read-only collections.
 * Prefer `mapOf(...) / emptyMap() / Map` in new Kotlin code; factories kept for Guava-shaped call sites only.
 */
@GwtCompatible(serializable = true, emulated = true)
class ImmutableMap<K, V> private constructor(
    private val delegate: Map<K, V>,
) : AbstractMutableMap<K, V>() {
    override val size: Int get() = delegate.size
    override fun get(key: K): V? = delegate[key]
    override fun containsKey(key: K): Boolean = delegate.containsKey(key)
    override fun containsValue(value: V): Boolean = delegate.containsValue(value)
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> =
        object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            private val snapshot = delegate.map { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                (Maps.immutableEntry(key, value) as MutableMap.MutableEntry<K, V>)
            }
            override val size: Int get() = snapshot.size
            override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean = snapshot.contains(element)
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val iterator = snapshot.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> = iterator.next()
                    override fun remove(): Unit = throw UnsupportedOperationException("ImmutableMap")
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
                throw UnsupportedOperationException("ImmutableMap")
            override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean =
                throw UnsupportedOperationException("ImmutableMap")
            override fun clear(): Unit = throw UnsupportedOperationException("ImmutableMap")
        }

    override fun put(key: K, value: V): V? = throw UnsupportedOperationException("ImmutableMap")
    override fun remove(key: K): V? = throw UnsupportedOperationException("ImmutableMap")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableMap")

    companion object {
        private val EMPTY = ImmutableMap<Any, Any>(emptyMap())

        @Suppress("UNCHECKED_CAST")
        fun <K, V> of(): ImmutableMap<K, V> = EMPTY as ImmutableMap<K, V>

        fun <K, V> of(k1: K, v1: V): ImmutableMap<K, V> = builder<K, V>().put(k1, v1).build()

        fun <K, V> of(k1: K, v1: V, k2: K, v2: V): ImmutableMap<K, V> =
            builder<K, V>().put(k1, v1).put(k2, v2).build()

        fun <K, V> of(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V): ImmutableMap<K, V> =
            builder<K, V>().put(k1, v1).put(k2, v2).put(k3, v3).build()

        fun <K, V> of(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V): ImmutableMap<K, V> =
            builder<K, V>().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build()

        fun <K, V> of(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V): ImmutableMap<K, V> =
            builder<K, V>().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5).build()

        fun <K, V> copyOf(map: Map<out K, V>): ImmutableMap<K, V> {
            if (map is ImmutableMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return map as ImmutableMap<K, V>
            }
            if (map.isEmpty()) return of()
            val snapshot = LinkedHashMap<K, V>()
            for ((key, value) in map) snapshot[nonNull(key, "key")] = nonNull(value, "value")
            return ImmutableMap(snapshot)
        }

        fun <K, V> copyOf(entries: Iterable<Map.Entry<out K, V>>): ImmutableMap<K, V> =
            builder<K, V>().putAll(entries).build()

        fun <K, V> builder(): Builder<K, V> = Builder()

        fun <K, V> builderWithExpectedSize(expectedSize: Int): Builder<K, V> {
            Preconditions.checkArgument(expectedSize >= 0)
            return Builder(expectedSize)
        }

        private fun <T> nonNull(value: T, role: String): T =
            value ?: throw NullPointerException("null $role")
    }

    class Builder<K, V>(expectedSize: Int = 4) {
        private val pending = ArrayList<Pair<K, V>>(expectedSize.coerceAtLeast(4))
        fun put(key: K, value: V): Builder<K, V> = apply {
            pending.add(nonNull(key, "key") to nonNull(value, "value"))
        }
        fun put(entry: Map.Entry<out K, V>): Builder<K, V> = put(entry.key, entry.value)
        fun putAll(map: Map<out K, V>): Builder<K, V> = apply {
            for ((key, value) in map) put(key, value)
        }
        fun putAll(entries: Iterable<Map.Entry<out K, V>>): Builder<K, V> = apply {
            for (entry in entries) put(entry)
        }
        fun build(): ImmutableMap<K, V> {
            if (pending.isEmpty()) return of()
            val result = LinkedHashMap<K, V>()
            for ((key, value) in pending) {
                if (result.containsKey(key)) throw IllegalArgumentException("duplicate key: $key")
                result[key] = value
            }
            return ImmutableMap(result)
        }
        fun buildOrThrow(): ImmutableMap<K, V> = build()
        fun buildKeepingLast(): ImmutableMap<K, V> {
            if (pending.isEmpty()) return of()
            val result = LinkedHashMap<K, V>()
            for ((key, value) in pending) result[key] = value
            return ImmutableMap(result)
        }
    }
}
