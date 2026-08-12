package com.bernaferrari.guavakt.collect

/** A lazy, removal-capable transformed list-multimap view. */
internal class TransformedListMultimapView<K, V1, V2>(
    private val fromMultimap: Multimap<K, V1>,
    private val transformer: (K, V1) -> V2,
) : ListMultimap<K, V2> {
    override fun size(): Int = fromMultimap.size()
    override fun isEmpty(): Boolean = fromMultimap.isEmpty()
    override fun containsKey(key: Any?): Boolean = fromMultimap.containsKey(key)
    override fun containsValue(value: Any?): Boolean = values().any { it == value }

    override fun containsEntry(key: Any?, value: Any?): Boolean =
        fromMultimap.entries().any {
            it.key == key && transformer(it.key, it.value) == value
        }

    override fun get(key: K): MutableList<V2> =
        TransformingMutableList(fromMultimap.get(key)) { transformer(key, it) }

    override fun keySet(): Set<K> = fromMultimap.keySet()
    override fun keys(): Multiset<K> = fromMultimap.keys()

    override fun values(): Collection<V2> = object : AbstractMutableCollection<V2>() {
        override val size: Int get() = fromMultimap.size()

        override fun iterator(): MutableIterator<V2> {
            val iterator = fromMultimap.entries().iterator()
            return object : MutableIterator<V2> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): V2 {
                    val entry = iterator.next()
                    return transformer(entry.key, entry.value)
                }
                override fun remove() {
                    @Suppress("UNCHECKED_CAST")
                    (iterator as MutableIterator<Map.Entry<K, V1>>).remove()
                }
            }
        }

        override fun add(element: V2): Nothing = throw UnsupportedOperationException()
    }

    override fun entries(): Collection<Map.Entry<K, V2>> =
        object : AbstractMutableCollection<Map.Entry<K, V2>>() {
            override val size: Int get() = fromMultimap.size()

            override fun iterator(): MutableIterator<Map.Entry<K, V2>> {
                val iterator = fromMultimap.entries().iterator()
                return object : MutableIterator<Map.Entry<K, V2>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): Map.Entry<K, V2> {
                        val entry = iterator.next()
                        return transformedEntry(entry.key, transformer(entry.key, entry.value))
                    }
                    override fun remove() {
                        @Suppress("UNCHECKED_CAST")
                        (iterator as MutableIterator<Map.Entry<K, V1>>).remove()
                    }
                }
            }

            override fun add(element: Map.Entry<K, V2>): Nothing =
                throw UnsupportedOperationException()
        }

    override fun asMap(): Map<K, List<V2>> = object : AbstractMutableMap<K, List<V2>>() {
        override val size: Int get() = fromMultimap.keySet().size
        override fun containsKey(key: K): Boolean = fromMultimap.containsKey(key)
        override fun get(key: K): List<V2>? =
            if (fromMultimap.containsKey(key)) this@TransformedListMultimapView.get(key) else null
        override fun remove(key: K): List<V2>? =
            if (fromMultimap.containsKey(key)) this@TransformedListMultimapView.removeAll(key) else null
        override fun clear() = fromMultimap.clear()
        override fun put(key: K, value: List<V2>): List<V2>? = throw UnsupportedOperationException()

        override val entries: MutableSet<MutableMap.MutableEntry<K, List<V2>>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, List<V2>>>() {
                override val size: Int get() = fromMultimap.keySet().size

                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, List<V2>>> {
                    val keys = fromMultimap.keySet().iterator()
                    return object : MutableIterator<MutableMap.MutableEntry<K, List<V2>>> {
                        private var current: K? = null
                        private var canRemove = false
                        override fun hasNext(): Boolean = keys.hasNext()
                        override fun next(): MutableMap.MutableEntry<K, List<V2>> {
                            val key = keys.next()
                            current = key
                            canRemove = true
                            return object : MutableMap.MutableEntry<K, List<V2>> {
                                override val key: K = key
                                override val value: List<V2>
                                    get() = this@TransformedListMultimapView.get(key)
                                override fun setValue(newValue: List<V2>): List<V2> =
                                    throw UnsupportedOperationException()
                                override fun equals(other: Any?): Boolean =
                                    other is Map.Entry<*, *> && key == other.key && value == other.value
                                override fun hashCode(): Int =
                                    (key?.hashCode() ?: 0) xor value.hashCode()
                                override fun toString(): String = "$key=$value"
                            }
                        }
                        override fun remove() {
                            if (!canRemove) throw IllegalStateException()
                            @Suppress("UNCHECKED_CAST")
                            (keys as MutableIterator<K>).remove()
                            canRemove = false
                        }
                    }
                }

                override fun add(element: MutableMap.MutableEntry<K, List<V2>>): Nothing =
                    throw UnsupportedOperationException()
            }
    }

    override fun put(key: K, value: V2): Nothing = throw UnsupportedOperationException()
    override fun putAll(key: K, values: Iterable<V2>): Nothing = throw UnsupportedOperationException()
    override fun putAll(multimap: Multimap<out K, out V2>): Nothing = throw UnsupportedOperationException()

    override fun remove(key: Any?, value: Any?): Boolean {
        val iterator = fromMultimap.entries().iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == key && transformer(entry.key, entry.value) == value) {
                @Suppress("UNCHECKED_CAST")
                (iterator as MutableIterator<Map.Entry<K, V1>>).remove()
                return true
            }
        }
        return false
    }

    override fun removeAll(key: Any?): List<V2> {
        var found = false
        var actualKey: Any? = null
        for (candidate in fromMultimap.keySet()) {
            if (candidate == key) {
                found = true
                actualKey = candidate
                break
            }
        }
        if (!found) return emptyList()
        @Suppress("UNCHECKED_CAST")
        val typedKey = actualKey as K
        val removed = fromMultimap.removeAll(key).toList()
        return TransformingReadOnlyList(removed) { transformer(typedKey, it) }
    }

    override fun replaceValues(key: K, values: Iterable<V2>): Nothing =
        throw UnsupportedOperationException()

    override fun clear() = fromMultimap.clear()

    override fun equals(other: Any?): Boolean =
        other === this || (other is ListMultimap<*, *> && asMap() == other.asMap())

    override fun hashCode(): Int = asMap().hashCode()
    override fun toString(): String = asMap().toString()
}

private class TransformingMutableList<F, T>(
    private val backing: MutableCollection<F>,
    private val transform: (F) -> T,
) : AbstractMutableList<T>() {
    override val size: Int get() = backing.size

    override fun get(index: Int): T = transform(backing.elementAt(index))

    override fun add(index: Int, element: T): Nothing = throw UnsupportedOperationException()

    override fun set(index: Int, element: T): Nothing = throw UnsupportedOperationException()

    override fun removeAt(index: Int): T {
        if (index !in 0 until size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
        val iterator = backing.iterator()
        repeat(index) {
            if (!iterator.hasNext()) throw IndexOutOfBoundsException("index: $index, size: $size")
            iterator.next()
        }
        if (!iterator.hasNext()) throw IndexOutOfBoundsException("index: $index, size: $size")
        val result = transform(iterator.next())
        iterator.remove()
        return result
    }
}

private class TransformingReadOnlyList<F, T>(
    private val backing: List<F>,
    private val transform: (F) -> T,
) : AbstractList<T>() {
    override val size: Int get() = backing.size
    override fun get(index: Int): T = transform(backing[index])
}

private fun <K, V> transformedEntry(key: K, value: V): Map.Entry<K, V> =
    object : Map.Entry<K, V> {
        override val key: K = key
        override val value: V = value
        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value
        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
        override fun toString(): String = "$key=$value"
    }
