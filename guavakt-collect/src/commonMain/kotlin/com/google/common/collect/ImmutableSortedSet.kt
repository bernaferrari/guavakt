package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Immutable comparator-backed sorted set.
 *
 * Comparator equivalence, rather than [Any.equals], defines duplicate elements and membership.
 * Kotlin common code has no `NavigableSet`, so navigation is exposed directly on this type.
 */
class ImmutableSortedSet<E> private constructor(
    private val elements: List<E>,
    private val ordering: Comparator<in E>,
) : AbstractMutableSet<E>() {
    private var descendingView: ImmutableSortedSet<E>? = null
    private var asListView: ImmutableList<E>? = null

    override val size: Int get() = elements.size

    override fun iterator(): MutableIterator<E> {
        val iterator = elements.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next()
            override fun remove(): Unit = throw UnsupportedOperationException("ImmutableSortedSet")
        }
    }

    override fun contains(element: E): Boolean {
        if (element == null) return false
        return try {
            indexOf(element) >= 0
        } catch (_: ClassCastException) {
            false
        }
    }

    override fun add(element: E): Boolean = throw UnsupportedOperationException("ImmutableSortedSet")
    override fun remove(element: E): Boolean = throw UnsupportedOperationException("ImmutableSortedSet")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableSortedSet")

    fun descendingIterator(): MutableIterator<E> = descendingSet().iterator()
    fun pollFirst(): E? = throw UnsupportedOperationException("ImmutableSortedSet")
    fun pollLast(): E? = throw UnsupportedOperationException("ImmutableSortedSet")

    fun comparator(): Comparator<in E> = ordering
    fun first(): E = elements.first()
    fun last(): E = elements.last()

    fun lower(element: E): E? = elementAtOrNull(lowerBound(nonNull(element)) - 1)
    fun floor(element: E): E? = elementAtOrNull(upperBound(nonNull(element)) - 1)
    fun ceiling(element: E): E? = elementAtOrNull(lowerBound(nonNull(element)))
    fun higher(element: E): E? = elementAtOrNull(upperBound(nonNull(element)))

    fun headSet(toElement: E): ImmutableSortedSet<E> = headSet(toElement, false)

    fun headSet(toElement: E, inclusive: Boolean): ImmutableSortedSet<E> =
        slice(0, if (inclusive) upperBound(nonNull(toElement)) else lowerBound(nonNull(toElement)))

    fun tailSet(fromElement: E): ImmutableSortedSet<E> = tailSet(fromElement, true)

    fun tailSet(fromElement: E, inclusive: Boolean): ImmutableSortedSet<E> =
        slice(if (inclusive) lowerBound(nonNull(fromElement)) else upperBound(nonNull(fromElement)), size)

    fun subSet(fromElement: E, toElement: E): ImmutableSortedSet<E> =
        subSet(fromElement, true, toElement, false)

    fun subSet(
        fromElement: E,
        fromInclusive: Boolean,
        toElement: E,
        toInclusive: Boolean,
    ): ImmutableSortedSet<E> {
        val from = nonNull(fromElement)
        val to = nonNull(toElement)
        require(ordering.compare(from, to) <= 0) { "expected fromElement <= toElement" }
        val lower = if (fromInclusive) lowerBound(from) else upperBound(from)
        val upper = if (toInclusive) upperBound(to) else lowerBound(to)
        return slice(lower.coerceAtMost(upper), upper)
    }

    fun descendingSet(): ImmutableSortedSet<E> {
        descendingView?.let { return it }
        val reversed = ImmutableSortedSet(elements.asReversed(), Comparator { a, b -> ordering.compare(b, a) })
        reversed.descendingView = this
        descendingView = reversed
        return reversed
    }

    fun asList(): ImmutableList<E> = asListView ?: ImmutableList.copyOf(elements).also { asListView = it }

    private fun slice(fromIndex: Int, toIndex: Int): ImmutableSortedSet<E> = when {
        fromIndex == 0 && toIndex == size -> this
        fromIndex == toIndex -> emptyFor(ordering)
        else -> ImmutableSortedSet(elements.subList(fromIndex, toIndex), ordering)
    }

    private fun elementAtOrNull(index: Int): E? = if (index in elements.indices) elements[index] else null

    private fun indexOf(element: E): Int = SortedLists.binarySearch(
        elements,
        element,
        ordering,
        SortedLists.KeyPresentBehavior.ANY_PRESENT,
        SortedLists.KeyAbsentBehavior.INVERTED_INSERTION_INDEX,
    )

    private fun lowerBound(element: E): Int = SortedLists.binarySearch(
        elements,
        element,
        ordering,
        SortedLists.KeyPresentBehavior.FIRST_PRESENT,
        SortedLists.KeyAbsentBehavior.NEXT_HIGHER,
    )

    private fun upperBound(element: E): Int = SortedLists.binarySearch(
        elements,
        element,
        ordering,
        SortedLists.KeyPresentBehavior.FIRST_AFTER,
        SortedLists.KeyAbsentBehavior.NEXT_HIGHER,
    )

    class Builder<E>(private val ordering: Comparator<in E>) {
        private val contents = ArrayList<E>()

        fun add(element: E): Builder<E> = apply { contents.add(nonNull(element)) }
        fun add(vararg elements: E): Builder<E> = apply { elements.forEach(::add) }
        fun addAll(elements: Iterable<E>): Builder<E> = apply { elements.forEach(::add) }
        fun addAll(elements: Iterator<E>): Builder<E> = apply { while (elements.hasNext()) add(elements.next()) }
        fun build(): ImmutableSortedSet<E> = copyOf(ordering, contents)
    }

    companion object {
        private object NaturalComparator : Comparator<Comparable<Any?>> {
            override fun compare(a: Comparable<Any?>, b: Comparable<Any?>): Int = a.compareTo(b)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <E : Comparable<E>> natural(): Comparator<E> = NaturalComparator as Comparator<E>

        private val EMPTY_NATURAL = ImmutableSortedSet<Comparable<Any?>>(emptyList(), NaturalComparator)

        @Suppress("UNCHECKED_CAST")
        private fun <E> emptyFor(ordering: Comparator<in E>): ImmutableSortedSet<E> =
            if (ordering === NaturalComparator) EMPTY_NATURAL as ImmutableSortedSet<E>
            else ImmutableSortedSet(emptyList(), ordering)

        @Suppress("UNCHECKED_CAST")
        fun <E : Comparable<E>> of(): ImmutableSortedSet<E> = EMPTY_NATURAL as ImmutableSortedSet<E>
        fun <E : Comparable<E>> of(element: E): ImmutableSortedSet<E> = copyOf(listOf(element))
        fun <E : Comparable<E>> of(e1: E, e2: E): ImmutableSortedSet<E> = copyOf(listOf(e1, e2))
        fun <E : Comparable<E>> of(e1: E, e2: E, e3: E): ImmutableSortedSet<E> = copyOf(listOf(e1, e2, e3))
        fun <E : Comparable<E>> of(e1: E, e2: E, e3: E, e4: E): ImmutableSortedSet<E> =
            copyOf(listOf(e1, e2, e3, e4))
        fun <E : Comparable<E>> of(e1: E, e2: E, e3: E, e4: E, e5: E): ImmutableSortedSet<E> =
            copyOf(listOf(e1, e2, e3, e4, e5))
        fun <E : Comparable<E>> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, vararg rest: E): ImmutableSortedSet<E> =
            copyOf(listOf(e1, e2, e3, e4, e5, e6, *rest))

        fun <E : Comparable<E>> copyOf(elements: Iterable<E>): ImmutableSortedSet<E> = copyOf(natural(), elements)
        fun <E : Comparable<E>> copyOf(elements: Iterator<E>): ImmutableSortedSet<E> =
            copyOf(natural(), Iterable { elements })
        fun <E : Comparable<E>> copyOf(elements: Array<out E>): ImmutableSortedSet<E> = copyOf(elements.asList())

        fun <E> copyOf(ordering: Comparator<in E>, elements: Iterable<E>): ImmutableSortedSet<E> {
            Preconditions.checkNotNull(ordering)
            if (elements is ImmutableSortedSet<*> && elements.ordering == ordering) {
                @Suppress("UNCHECKED_CAST")
                return elements as ImmutableSortedSet<E>
            }
            val sorted = elements.map(::nonNull).sortedWith(ordering)
            if (sorted.isEmpty()) return emptyFor(ordering)
            val deduped = ArrayList<E>(sorted.size)
            for (element in sorted) {
                if (deduped.isEmpty() || ordering.compare(deduped.last(), element) != 0) deduped.add(element)
            }
            return ImmutableSortedSet(deduped, ordering)
        }

        fun <E : Comparable<E>> naturalOrder(): Builder<E> = Builder(natural())
        fun <E : Comparable<E>> reverseOrder(): Builder<E> = Builder(natural<E>().reversed())
        fun <E> orderedBy(ordering: Comparator<in E>): Builder<E> = Builder(Preconditions.checkNotNull(ordering))

        private fun <T> nonNull(value: T): T = value ?: throw NullPointerException("null element")
    }
}
