package com.bernaferrari.guavakt.collect

/**
 * Guava ImmutableSortedAsList — read-only sorted list.
 */
class ImmutableSortedAsList<E : Comparable<E>> private constructor(
    private val delegate: List<E>,
) : List<E> by delegate {

    override fun equals(other: Any?): Boolean =
        other === this || (other is List<*> && size == other.size && indices.all { this[it] == other[it] })

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    companion object {
        private val EMPTY = ImmutableSortedAsList<Nothing>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <E : Comparable<E>> of(): ImmutableSortedAsList<E> = EMPTY as ImmutableSortedAsList<E>

        fun <E : Comparable<E>> copyOf(elements: Collection<E>): ImmutableSortedAsList<E> {
            if (elements.isEmpty()) return of()
            return ImmutableSortedAsList(elements.sorted())
        }

        fun <E : Comparable<E>> create(): ImmutableSortedAsList<E> = of()
        fun <E : Comparable<E>> create(elements: Collection<out E>): ImmutableSortedAsList<E> =
            copyOf(elements.toList())
    }
}
