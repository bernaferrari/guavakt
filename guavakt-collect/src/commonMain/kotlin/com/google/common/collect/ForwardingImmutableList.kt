package dev.guavakt.collect

/** Guava ForwardingImmutableList — forwards to [delegate]. */
abstract class ForwardingImmutableList<E> : dev.guavakt.collect.ForwardingList<E>() {
    protected abstract override fun delegate(): MutableList<E>
}
