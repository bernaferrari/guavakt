package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.internal.platformIdentityHashCode

/**
 * Portable identity hash map (=== semantics), Guava Maps.newIdentityHashMap stand-in.
 */
internal class IdentityHashMapKmp<K, V> : AbstractMutableMap<K, V>() {
    private val buckets = LinkedHashMap<Int, MutableList<MutableEntry>>()

    private inner class MutableEntry(override var key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val old = value
            value = newValue
            return old
        }
    }

    override val size: Int
        get() = buckets.values.sumOf { it.size }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> =
        object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = this@IdentityHashMapKmp.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val all = buckets.values.flatten().toMutableList()
                val it = all.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var last: MutableEntry? = null
                    override fun hasNext() = it.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        val e = it.next()
                        last = e
                        return e
                    }
                    override fun remove() {
                        val e = last ?: throw IllegalStateException()
                        removeKey(e.key)
                        it.remove()
                        last = null
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                put(element.key, element.value)
                return true
            }
        }

    private fun bucket(key: K): Int = platformIdentityHashCode(key as Any)

    private fun find(key: K): MutableEntry? {
        val b = buckets[bucket(key)] ?: return null
        return b.firstOrNull { it.key === key }
    }

    override fun get(key: K): V? = find(key)?.value

    override fun containsKey(key: K): Boolean = find(key) != null

    override fun put(key: K, value: V): V? {
        val h = bucket(key)
        val list = buckets.getOrPut(h) { ArrayList() }
        val existing = list.firstOrNull { it.key === key }
        if (existing != null) {
            val old = existing.value
            existing.value = value
            return old
        }
        list.add(MutableEntry(key, value))
        return null
    }

    override fun remove(key: K): V? = removeKey(key)

    private fun removeKey(key: K): V? {
        val h = bucket(key)
        val list = buckets[h] ?: return null
        val idx = list.indexOfFirst { it.key === key }
        if (idx < 0) return null
        val old = list.removeAt(idx).value
        if (list.isEmpty()) buckets.remove(h)
        return old
    }

    override fun clear() = buckets.clear()
}
