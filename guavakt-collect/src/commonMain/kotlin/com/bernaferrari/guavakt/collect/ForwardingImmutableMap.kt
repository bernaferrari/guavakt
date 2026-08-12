package com.bernaferrari.guavakt.collect

/** Guava ForwardingImmutableMap — forwards to [delegate]. */
abstract class ForwardingImmutableMap<K, V> : com.bernaferrari.guavakt.collect.ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>
}
