package com.bernaferrari.guavakt.collect

/**
 * Guava ImmutableEnumSet — read-only set (enum elements preferred).
 */
class ImmutableEnumSet<E> private constructor(
    private val delegate: Set<E>,
) : Set<E> by delegate {

    companion object {
        private val EMPTY = ImmutableEnumSet<Nothing>(emptySet())

        @Suppress("UNCHECKED_CAST")
        fun <E> of(): ImmutableEnumSet<E> = EMPTY as ImmutableEnumSet<E>

        fun <E> of(element: E): ImmutableEnumSet<E> = ImmutableEnumSet(setOf(element))

        fun <E> copyOf(elements: Collection<E>): ImmutableEnumSet<E> {
            if (elements is ImmutableEnumSet) return elements
            if (elements.isEmpty()) return of()
            return ImmutableEnumSet(LinkedHashSet(elements))
        }

        fun <E> create(): ImmutableEnumSet<E> = of()
        fun <E> create(elements: Collection<out E>): ImmutableEnumSet<E> = copyOf(elements.toList())
    }
}
