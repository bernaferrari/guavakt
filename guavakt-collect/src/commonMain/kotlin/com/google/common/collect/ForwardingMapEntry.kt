package dev.guavakt.collect

/** Guava ForwardingMapEntry — forwards to [delegate]. */
abstract class ForwardingMapEntry<K, V> : dev.guavakt.collect.ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>
}
