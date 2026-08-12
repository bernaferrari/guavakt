package com.bernaferrari.guavakt.hash

/** Guava ImmutableSupplier — supplier of a constant instance. */
internal class ImmutableSupplier<T>(private val instance: T) {
    fun get(): T = instance
}
