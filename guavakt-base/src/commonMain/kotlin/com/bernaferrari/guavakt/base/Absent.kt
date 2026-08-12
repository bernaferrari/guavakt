package com.bernaferrari.guavakt.base

internal object Absent : Optional<Any>() {
    override fun isPresent(): Boolean = false
    override fun isAbsent(): Boolean = true
    override fun get(): Any =
        throw IllegalStateException("Optional.get() cannot be called on an absent value")
    override fun or(defaultValue: Any): Any = Preconditions.checkNotNull(defaultValue)
    override fun or(supplier: Supplier<Any>): Any = Preconditions.checkNotNull(supplier.get())
    @Suppress("UNCHECKED_CAST")
    override fun or(secondChoice: Optional<out Any>): Optional<Any> =
        Preconditions.checkNotNull(secondChoice) as Optional<Any>
    override fun orNull(): Any? = null
    override fun asSet(): Set<Any> = emptySet()
    override fun <V : Any> transform(function: Function<in Any, V>): Optional<V> {
        Preconditions.checkNotNull(function)
        return absent()
    }
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0x598df91c
    override fun toString(): String = "Optional.absent()"
}
