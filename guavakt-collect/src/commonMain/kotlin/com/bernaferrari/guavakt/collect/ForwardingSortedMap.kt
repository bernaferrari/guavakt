package com.bernaferrari.guavakt.collect

/** Guava ForwardingSortedMap — forwards sorted operations to [delegate]. */
abstract class ForwardingSortedMap<K, V> : ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>

    open fun comparator(): Comparator<in K>? = null

    open fun firstKey(): K {
        val d = delegate()
        if (d is ComparatorTreeMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return (d as ComparatorTreeMap<K, V>).firstKey()
        }
        val keys = d.keys
        if (keys.isEmpty()) throw NoSuchElementException()
        var best = keys.first()
        for (k in keys) {
            if (compareKeys(k, best) < 0) best = k
        }
        return best
    }

    open fun lastKey(): K {
        val d = delegate()
        if (d is ComparatorTreeMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return (d as ComparatorTreeMap<K, V>).lastKey()
        }
        val keys = d.keys
        if (keys.isEmpty()) throw NoSuchElementException()
        var best = keys.first()
        for (k in keys) {
            if (compareKeys(k, best) > 0) best = k
        }
        return best
    }

    private fun compareKeys(a: K, b: K): Int {
        val c = comparator()
        return if (c != null) c.compare(a, b)
        else {
            @Suppress("UNCHECKED_CAST")
            (a as Comparable<K>).compareTo(b)
        }
    }
}
