package com.bernaferrari.guavakt.collect

/**
 * Guava ForwardingNavigableMap — forwards navigable operations to a delegate map.
 * KMP: navigable ops over sorted snapshot of delegate keys (no java.util.NavigableMap).
 */
abstract class ForwardingNavigableMap<K, V> : ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>

    private fun sortedKeys(): List<K> {
        val keys = delegate().keys.toList()
        return keys.sortedWith { a, b ->
            @Suppress("UNCHECKED_CAST")
            (a as Comparable<Any>).compareTo(b as Any)
        }
    }

    fun firstKey(): K = sortedKeys().first()
    fun lastKey(): K = sortedKeys().last()
    fun lowerKey(key: K): K? {
        val keys = sortedKeys()
        return keys.lastOrNull {
            @Suppress("UNCHECKED_CAST")
            (it as Comparable<Any>).compareTo(key as Any) < 0
        }
    }
    fun floorKey(key: K): K? {
        val keys = sortedKeys()
        return keys.lastOrNull {
            @Suppress("UNCHECKED_CAST")
            (it as Comparable<Any>).compareTo(key as Any) <= 0
        }
    }
    fun ceilingKey(key: K): K? {
        val keys = sortedKeys()
        return keys.firstOrNull {
            @Suppress("UNCHECKED_CAST")
            (it as Comparable<Any>).compareTo(key as Any) >= 0
        }
    }
    fun higherKey(key: K): K? {
        val keys = sortedKeys()
        return keys.firstOrNull {
            @Suppress("UNCHECKED_CAST")
            (it as Comparable<Any>).compareTo(key as Any) > 0
        }
    }
    fun pollFirstEntry(): Map.Entry<K, V>? {
        val k = sortedKeys().firstOrNull() ?: return null
        val v = delegate().remove(k) ?: return null
        return object : Map.Entry<K, V> {
            override val key: K get() = k
            override val value: V get() = v
        }
    }
    fun pollLastEntry(): Map.Entry<K, V>? {
        val k = sortedKeys().lastOrNull() ?: return null
        val v = delegate().remove(k) ?: return null
        return object : Map.Entry<K, V> {
            override val key: K get() = k
            override val value: V get() = v
        }
    }
}
