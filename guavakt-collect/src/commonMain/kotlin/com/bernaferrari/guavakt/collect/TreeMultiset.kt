package com.bernaferrari.guavakt.collect

/** Comparator-backed mutable sorted multiset with live range and descending views. */
class TreeMultiset<E> private constructor(
    private val ordering: Comparator<in E>,
) : AbstractMapBasedMultiset<E>(ComparatorTreeMap(ordering)), SortedMultiset<E> {
    override fun comparator(): Comparator<in E> = ordering

    override fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E> =
        RangeSortedMultisetView(this) { element ->
            val comparison = ordering.compare(element, upperBound)
            comparison < 0 || comparison == 0 && boundType == BoundType.CLOSED
        }

    override fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E> =
        RangeSortedMultisetView(this) { element ->
            val comparison = ordering.compare(element, lowerBound)
            comparison > 0 || comparison == 0 && boundType == BoundType.CLOSED
        }

    companion object {
        fun <E : Comparable<E>> create(): TreeMultiset<E> = TreeMultiset(naturalComparator())
        fun <E> create(comparator: Comparator<in E>): TreeMultiset<E> = TreeMultiset(comparator)
        fun <E : Comparable<E>> create(elements: Iterable<E>): TreeMultiset<E> =
            create<E>().also { multiset -> for (element in elements) multiset.add(element) }

        private fun <E : Comparable<E>> naturalComparator(): Comparator<E> =
            Comparator { first, second -> first.compareTo(second) }
    }
}
