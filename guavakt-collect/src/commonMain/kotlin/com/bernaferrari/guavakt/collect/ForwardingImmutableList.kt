package com.bernaferrari.guavakt.collect

/** Guava ForwardingImmutableList — forwards to [delegate]. */
abstract class ForwardingImmutableList<E> : com.bernaferrari.guavakt.collect.ForwardingList<E>() {
    protected abstract override fun delegate(): MutableList<E>
}
