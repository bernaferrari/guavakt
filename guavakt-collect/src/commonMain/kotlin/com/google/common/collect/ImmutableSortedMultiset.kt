package dev.guavakt.collect

class ImmutableSortedMultiset<E> private constructor(
    private val counts: Map<E, Int>,
    private val sortedElements: List<E>,
    private val ordering: Comparator<in E>,
) : AbstractMultiset<E>(), SortedMultiset<E> {
    private var descendingView: ImmutableSortedMultiset<E>? = null
    private val totalSize: Int = counts.values.fold(0L) { total, count -> total + count }
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    override val size: Int get() = totalSize
    override fun comparator(): Comparator<in E> = ordering

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private val elements = sortedElements.iterator()
        private var current: E? = null
        private var remaining = 0
        override fun hasNext(): Boolean = remaining > 0 || elements.hasNext()
        override fun next(): E {
            if (remaining == 0) {
                current = elements.next()
                remaining = counts[current] ?: 0
            }
            remaining--
            @Suppress("UNCHECKED_CAST")
            return current as E
        }
        override fun remove() = throw UnsupportedOperationException("ImmutableSortedMultiset")
    }

    override fun count(element: Any?): Int = try {
        @Suppress("UNCHECKED_CAST")
        val typed = element as E
        val representative = sortedElements.firstOrNull { ordering.compare(it, typed) == 0 }
        if (representative == null) 0 else counts[representative] ?: 0
    } catch (_: ClassCastException) {
        0
    } catch (_: NullPointerException) {
        0
    }
    override fun add(element: E): Boolean = throw UnsupportedOperationException("ImmutableSortedMultiset")
    override fun add(element: E, occurrences: Int): Int = throw UnsupportedOperationException("ImmutableSortedMultiset")
    override fun remove(element: Any?, occurrences: Int): Int = throw UnsupportedOperationException("ImmutableSortedMultiset")
    override fun setCount(element: E, count: Int): Int = throw UnsupportedOperationException("ImmutableSortedMultiset")
    override fun clear() = throw UnsupportedOperationException("ImmutableSortedMultiset")
    override fun elementSet(): Set<E> = ImmutableSortedSet.copyOf(ordering, sortedElements)
    override fun entrySet(): Set<Multiset.Entry<E>> = ImmutableSet.copyOf(
        sortedElements.map { element -> Multisets.immutableEntry(element, counts[element] ?: 0) },
    )
    override fun pollFirstEntry(): Multiset.Entry<E>? = throw UnsupportedOperationException("ImmutableSortedMultiset")
    override fun pollLastEntry(): Multiset.Entry<E>? = throw UnsupportedOperationException("ImmutableSortedMultiset")

    override fun headMultiset(upperBound: E, boundType: BoundType): ImmutableSortedMultiset<E> =
        subset { element ->
            val comparison = ordering.compare(element, upperBound)
            comparison < 0 || comparison == 0 && boundType == BoundType.CLOSED
        }

    override fun tailMultiset(lowerBound: E, boundType: BoundType): ImmutableSortedMultiset<E> =
        subset { element ->
            val comparison = ordering.compare(element, lowerBound)
            comparison > 0 || comparison == 0 && boundType == BoundType.CLOSED
        }

    override fun subMultiset(
        lowerBound: E,
        lowerBoundType: BoundType,
        upperBound: E,
        upperBoundType: BoundType,
    ): ImmutableSortedMultiset<E> {
        require(ordering.compare(lowerBound, upperBound) <= 0) {
            "lowerBound ($lowerBound) must be <= upperBound ($upperBound)"
        }
        return tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType)
    }

    override fun descendingMultiset(): ImmutableSortedMultiset<E> {
        descendingView?.let { return it }
        val reversed = ImmutableSortedMultiset(counts, sortedElements.asReversed(), ordering.reversed())
        reversed.descendingView = this
        descendingView = reversed
        return reversed
    }

    private fun subset(predicate: (E) -> Boolean): ImmutableSortedMultiset<E> {
        val elements = sortedElements.filter(predicate)
        if (elements.isEmpty()) return ImmutableSortedMultiset(emptyMap(), emptyList(), ordering)
        return ImmutableSortedMultiset(elements.associateWith { counts[it] ?: 0 }, elements, ordering)
    }

    fun asList(): List<E> = buildList {
        for (element in sortedElements) repeat(counts[element] ?: 0) { add(element) }
    }

    companion object {
        fun <E : Comparable<E>> of(): ImmutableSortedMultiset<E> =
            ImmutableSortedMultiset(emptyMap(), emptyList(), naturalComparator())
        fun <E : Comparable<E>> of(element: E): ImmutableSortedMultiset<E> = copyOf(listOf(element))
        fun <E : Comparable<E>> of(e1: E, e2: E): ImmutableSortedMultiset<E> = copyOf(listOf(e1, e2))
        fun <E : Comparable<E>> copyOf(elements: Iterable<E>): ImmutableSortedMultiset<E> =
            copyOf(naturalComparator(), elements)

        fun <E> copyOf(
            comparator: Comparator<in E>,
            elements: Iterable<E>,
        ): ImmutableSortedMultiset<E> {
            val multiset = TreeMultiset.create(comparator)
            for (element in elements) multiset.add(element)
            val sorted = multiset.elementSet().toList()
            return ImmutableSortedMultiset(
                sorted.associateWith { multiset.count(it) },
                sorted,
                comparator,
            )
        }
        fun <E : Comparable<E>> copyOf(elements: Array<out E>): ImmutableSortedMultiset<E> = copyOf(elements.asList())
        fun <E : Comparable<E>> builder(): Builder<E> = naturalOrder()
        fun <E : Comparable<E>> naturalOrder(): Builder<E> = Builder(naturalComparator())
        fun <E : Comparable<E>> reverseOrder(): Builder<E> = Builder(naturalComparator<E>().reversed())
        fun <E> orderedBy(comparator: Comparator<in E>): Builder<E> = Builder(comparator)
        private fun <E : Comparable<E>> naturalComparator(): Comparator<E> =
            Comparator { first, second -> first.compareTo(second) }
    }

    class Builder<E> internal constructor(
        private val ordering: Comparator<in E>,
    ) {
        private val multiset = TreeMultiset.create(ordering)
        fun add(element: E): Builder<E> = apply { multiset.add(element) }
        fun add(vararg elements: E): Builder<E> = apply { for (element in elements) multiset.add(element) }
        fun addCopies(element: E, occurrences: Int): Builder<E> = apply { multiset.add(element, occurrences) }
        fun setCount(element: E, count: Int): Builder<E> = apply { multiset.setCount(element, count) }
        fun addAll(elements: Iterable<E>): Builder<E> = apply { for (element in elements) multiset.add(element) }
        fun build(): ImmutableSortedMultiset<E> = copyOf(ordering, multiset)
    }
}
