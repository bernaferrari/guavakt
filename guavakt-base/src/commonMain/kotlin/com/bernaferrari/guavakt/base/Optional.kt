package com.bernaferrari.guavakt.base

import com.bernaferrari.guavakt.annotations.GwtCompatible

/**
 * Guava [Optional] — presence container. **Prefer Kotlin `T?` in new code.** Kept so Guava-shaped APIs compile; not a substitute for null-safety.
 */
@GwtCompatible(serializable = true)
sealed class Optional<T : Any> {
    abstract fun isPresent(): Boolean
    abstract fun isAbsent(): Boolean
    abstract fun get(): T
    abstract fun or(defaultValue: T): T
    abstract fun or(supplier: Supplier<T>): T
    abstract fun or(secondChoice: Optional<out T>): Optional<T>
    abstract fun orNull(): T?
    abstract fun asSet(): Set<T>
    abstract fun <V : Any> transform(function: Function<in T, V>): Optional<V>
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
    abstract override fun toString(): String

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> absent(): Optional<T> = Absent as Optional<T>

        fun <T : Any> of(reference: T): Optional<T> = Present(Preconditions.checkNotNull(reference))

        fun <T : Any> fromNullable(nullableReference: T?): Optional<T> =
            if (nullableReference == null) absent() else of(nullableReference)

        fun <T : Any> presentInstances(optionals: Iterable<Optional<out T>>): Iterable<T> =
            Iterable {
                object : AbstractIterator<T>() {
                    private val iterator = optionals.iterator()
                    override fun computeNext(): T? {
                        while (iterator.hasNext()) {
                            val optional = iterator.next()
                            if (optional.isPresent()) return optional.get()
                        }
                        return endOfData()
                    }
                }
            }
    }
}
