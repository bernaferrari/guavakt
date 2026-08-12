package com.bernaferrari.guavakt.base

internal class Present<T : Any>(private val reference: T) : Optional<T>() {
    override fun isPresent(): Boolean = true
    override fun isAbsent(): Boolean = false
    override fun get(): T = reference
    override fun or(defaultValue: T): T {
        Preconditions.checkNotNull(defaultValue)
        return reference
    }
    override fun or(supplier: Supplier<T>): T {
        Preconditions.checkNotNull(supplier)
        return reference
    }
    override fun or(secondChoice: Optional<out T>): Optional<T> {
        Preconditions.checkNotNull(secondChoice)
        return this
    }
    override fun orNull(): T = reference
    override fun asSet(): Set<T> = setOf(reference)
    override fun <V : Any> transform(function: Function<in T, V>): Optional<V> =
        of(Preconditions.checkNotNull(function.apply(reference)))
    override fun equals(other: Any?): Boolean =
        other is Present<*> && reference == other.reference
    override fun hashCode(): Int = 0x598df91c + reference.hashCode()
    override fun toString(): String = "Optional.of($reference)"
}
