package com.bernaferrari.guavakt.collect

/**
 * Portable sorted map (Guava TreeMap-like). Keys ordered by [comparator] or natural order.
 * Iteration and navigable ops use comparator order (stdlib-friendly sorted snapshots).
 */
open class ComparatorTreeMap<K, V>(private val cmp: Comparator<in K>?) : AbstractMutableMap<K, V>() {
    private val data = LinkedHashMap<K, V>()

    private fun keyCompare(a: K, b: K): Int =
        if (cmp != null) cmp.compare(a, b)
        else {
            @Suppress("UNCHECKED_CAST")
            (a as Comparable<K>).compareTo(b)
        }

    fun comparator(): Comparator<in K>? = cmp

    override val size: Int get() = data.size

    override fun put(key: K, value: V): V? {
        for (entry in data.entries) {
            if (keyCompare(entry.key, key) == 0) return entry.setValue(value)
        }
        return data.put(key, value)
    }

    override fun get(key: K): V? {
        for (entry in data.entries) if (keyCompare(entry.key, key) == 0) return entry.value
        return null
    }

    override fun containsKey(key: K): Boolean {
        for (stored in data.keys) if (keyCompare(stored, key) == 0) return true
        return false
    }

    override fun remove(key: K): V? {
        val iterator = data.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (keyCompare(entry.key, key) == 0) {
                val old = entry.value
                iterator.remove()
                return old
            }
        }
        return null
    }
    override fun clear() = data.clear()

    private fun sortedKeys(): List<K> = data.keys.sortedWith { a, b -> keyCompare(a, b) }

    private fun sortedEntries(): List<MutableMap.MutableEntry<K, V>> =
        data.entries.sortedWith { a, b -> keyCompare(a.key, b.key) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = data.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val list = sortedEntries().toMutableList()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private val it = list.iterator()
                    private var last: MutableMap.MutableEntry<K, V>? = null
                    override fun hasNext() = it.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        last = it.next(); return last!!
                    }
                    override fun remove() {
                        val e = last ?: throw IllegalStateException()
                        data.remove(e.key); it.remove(); last = null
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                put(element.key, element.value); return true
            }
            override fun clear() = data.clear()
        }

    fun firstKey(): K = sortedKeys().first()
    fun lastKey(): K = sortedKeys().last()
    fun lowerKey(key: K): K? = sortedKeys().lastOrNull { keyCompare(it, key) < 0 }
    fun floorKey(key: K): K? = sortedKeys().lastOrNull { keyCompare(it, key) <= 0 }
    fun ceilingKey(key: K): K? = sortedKeys().firstOrNull { keyCompare(it, key) >= 0 }
    fun higherKey(key: K): K? = sortedKeys().firstOrNull { keyCompare(it, key) > 0 }

    fun firstEntry(): Map.Entry<K, V>? = sortedKeys().firstOrNull()?.let { entryOf(it) }
    fun lastEntry(): Map.Entry<K, V>? = sortedKeys().lastOrNull()?.let { entryOf(it) }
    fun lowerEntry(key: K): Map.Entry<K, V>? = lowerKey(key)?.let { entryOf(it) }
    fun floorEntry(key: K): Map.Entry<K, V>? = floorKey(key)?.let { entryOf(it) }
    fun ceilingEntry(key: K): Map.Entry<K, V>? = ceilingKey(key)?.let { entryOf(it) }
    fun higherEntry(key: K): Map.Entry<K, V>? = higherKey(key)?.let { entryOf(it) }

    private fun entryOf(key: K): Map.Entry<K, V> = object : Map.Entry<K, V> {
        override val key: K get() = key
        override val value: V get() = data[key] as V
    }

    fun navigableKeySet(): Set<K> = sortedKeys().toSet()

    fun descendingMap(): Map<K, V> {
        val keys = sortedKeys().asReversed()
        return keys.associateWith { data[it] as V }
    }

    fun descendingKeySet(): Set<K> = sortedKeys().asReversed().toSet()

    fun headMap(toKey: K, inclusive: Boolean = false): Map<K, V> {
        val keys = sortedKeys().filter {
            val c = keyCompare(it, toKey)
            if (inclusive) c <= 0 else c < 0
        }
        return keys.associateWith { data[it] as V }
    }

    fun tailMap(fromKey: K, inclusive: Boolean = true): Map<K, V> {
        val keys = sortedKeys().filter {
            val c = keyCompare(it, fromKey)
            if (inclusive) c >= 0 else c > 0
        }
        return keys.associateWith { data[it] as V }
    }

    fun subMap(fromKey: K, toKey: K, fromInclusive: Boolean = true, toInclusive: Boolean = false): Map<K, V> {
        val keys = sortedKeys().filter {
            val fromOk = if (fromInclusive) keyCompare(it, fromKey) >= 0 else keyCompare(it, fromKey) > 0
            val toOk = if (toInclusive) keyCompare(it, toKey) <= 0 else keyCompare(it, toKey) < 0
            fromOk && toOk
        }
        return keys.associateWith { data[it] as V }
    }
}
