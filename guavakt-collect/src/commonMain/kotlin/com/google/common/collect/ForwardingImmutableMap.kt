package dev.guavakt.collect

/** Guava ForwardingImmutableMap — forwards to [delegate]. */
abstract class ForwardingImmutableMap<K, V> : dev.guavakt.collect.ForwardingMap<K, V>() {
    protected abstract override fun delegate(): MutableMap<K, V>
}
