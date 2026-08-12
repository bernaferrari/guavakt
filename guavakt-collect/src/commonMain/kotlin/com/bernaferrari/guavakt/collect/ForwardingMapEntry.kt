package com.bernaferrari.guavakt.collect

/** Guava ForwardingMapEntry — forwards to [delegate]. */
abstract class ForwardingMapEntry<K, V> : com.bernaferrari.guavakt.collect.ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>
}
