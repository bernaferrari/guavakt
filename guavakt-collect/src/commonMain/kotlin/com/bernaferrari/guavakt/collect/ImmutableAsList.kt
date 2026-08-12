package com.bernaferrari.guavakt.collect

/**
 * Guava ImmutableAsList — read-only list view typically backed by an immutable collection's elements.
 */
class ImmutableAsList<E> private constructor(
    private val delegate: List<E>,
) : List<E> by delegate {

    override fun equals(other: Any?): Boolean =
        other === this || (other is List<*> && size == other.size && indices.all { this[it] == other[it] })

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    companion object {
        private val EMPTY = ImmutableAsList<Nothing>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <E> of(): ImmutableAsList<E> = EMPTY as ImmutableAsList<E>

        fun <E> copyOf(elements: Collection<E>): ImmutableAsList<E> {
            if (elements.isEmpty()) return of()
            return ImmutableAsList(elements.toList())
        }

        fun <E> create(): ImmutableAsList<E> = of()
        fun <E> create(elements: Collection<out E>): ImmutableAsList<E> = copyOf(elements.toList())
    }
}
