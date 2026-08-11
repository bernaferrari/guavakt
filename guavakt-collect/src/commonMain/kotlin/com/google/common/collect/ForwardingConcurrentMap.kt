package dev.guavakt.collect

/** Guava ForwardingConcurrentMap — forwards to [delegate]. */
abstract class ForwardingConcurrentMap<K, V> : dev.guavakt.collect.ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>
}
