package com.bernaferrari.guavakt.collect

/** Explicit wrapper form of [Multisets.unmodifiableSortedMultiset]. */
class UnmodifiableSortedMultiset<E> private constructor(
    delegate: SortedMultiset<E>,
) : ForwardingSortedMultiset<E>() {
    private val readOnly = Multisets.unmodifiableSortedMultiset(delegate)
    override fun delegate(): SortedMultiset<E> = readOnly

    companion object {
        fun <E> create(multiset: SortedMultiset<E>): UnmodifiableSortedMultiset<E> =
            UnmodifiableSortedMultiset(multiset)

        fun <E : Comparable<E>> create(): UnmodifiableSortedMultiset<E> =
            UnmodifiableSortedMultiset(TreeMultiset.create<E>())

        fun <E : Comparable<E>> create(elements: Iterable<E>): UnmodifiableSortedMultiset<E> =
            UnmodifiableSortedMultiset(TreeMultiset.create(elements))
    }
}
