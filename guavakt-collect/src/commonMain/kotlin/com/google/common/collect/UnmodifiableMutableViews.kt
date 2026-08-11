package dev.guavakt.collect

/** Guava-style: immutable multimap views still typed as mutable but reject mutation. */
internal fun <E> unmodifiableMutableList(list: List<E>): MutableList<E> =
    object : AbstractMutableList<E>() {
        override val size: Int get() = list.size
        override fun get(index: Int): E = list[index]
        override fun add(index: Int, element: E) = throw UnsupportedOperationException()
        override fun removeAt(index: Int): E = throw UnsupportedOperationException()
        override fun set(index: Int, element: E): E = throw UnsupportedOperationException()
    }

internal fun <E> unmodifiableMutableSet(set: Set<E>): MutableSet<E> =
    object : AbstractMutableSet<E>() {
        override val size: Int get() = set.size
        override fun iterator(): MutableIterator<E> {
            val it = set.iterator()
            return object : MutableIterator<E> {
                override fun hasNext() = it.hasNext()
                override fun next() = it.next()
                override fun remove() = throw UnsupportedOperationException()
            }
        }
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
        override fun remove(element: E): Boolean = throw UnsupportedOperationException()
        override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
        override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
        override fun clear() = throw UnsupportedOperationException()
        override fun equals(other: Any?): Boolean = set == other
        override fun hashCode(): Int = set.hashCode()
        override fun toString(): String = set.toString()
    }

internal fun <E> unmodifiableMutableCollection(col: Collection<E>): MutableCollection<E> =
    object : AbstractMutableCollection<E>() {
        override val size: Int get() = col.size
        override fun iterator(): MutableIterator<E> {
            val it = col.iterator()
            return object : MutableIterator<E> {
                override fun hasNext() = it.hasNext()
                override fun next() = it.next()
                override fun remove() = throw UnsupportedOperationException()
            }
        }
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
        override fun remove(element: E): Boolean = throw UnsupportedOperationException()
        override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
        override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
        override fun clear() = throw UnsupportedOperationException()
        override fun equals(other: Any?): Boolean = col == other
        override fun hashCode(): Int = col.hashCode()
        override fun toString(): String = col.toString()
    }

/** Read-through map facade whose direct, entry, key, and value mutation routes all fail. */
internal fun <K, V> unmodifiableMutableMap(map: Map<K, V>): MutableMap<K, V> =
    object : AbstractMutableMap<K, V>() {
        override val size: Int get() = map.size
        override fun containsKey(key: K): Boolean = map.containsKey(key)
        override fun get(key: K): V? = map[key]
        override fun put(key: K, value: V): V? = throw UnsupportedOperationException()
        override fun putAll(from: Map<out K, V>) = throw UnsupportedOperationException()
        override fun remove(key: K): V? = throw UnsupportedOperationException()
        override fun clear() = throw UnsupportedOperationException()

        override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
                override val size: Int get() = map.size
                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                    val iterator = map.entries.iterator()
                    return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                        override fun hasNext(): Boolean = iterator.hasNext()
                        override fun next(): MutableMap.MutableEntry<K, V> {
                            val entry = iterator.next()
                            return object : MutableMap.MutableEntry<K, V> {
                                override val key: K = entry.key
                                override val value: V get() = entry.value
                                override fun setValue(newValue: V): V =
                                    throw UnsupportedOperationException()
                                override fun equals(other: Any?): Boolean =
                                    other is Map.Entry<*, *> && key == other.key && value == other.value
                                override fun hashCode(): Int =
                                    (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
                                override fun toString(): String = "$key=$value"
                            }
                        }
                        override fun remove() = throw UnsupportedOperationException()
                    }
                }
                override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
                    throw UnsupportedOperationException()
                override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean =
                    throw UnsupportedOperationException()
                override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean =
                    throw UnsupportedOperationException()
                override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean =
                    throw UnsupportedOperationException()
                override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean =
                    throw UnsupportedOperationException()
                override fun clear() = throw UnsupportedOperationException()
            }
    }
