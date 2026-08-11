package dev.guavakt.collect

internal class RangeSortedMultisetView<E>(
    private val delegate: SortedMultiset<E>,
    private val accepts: (E) -> Boolean,
) : AbstractSortedMultiset<E>() {
    override fun comparator(): Comparator<in E> = delegate.comparator()

    private fun elements(): List<E> = delegate.elementSet().filter(accepts)

    override val size: Int
        get() {
            var total = 0L
            for (element in elements()) total += delegate.count(element).toLong()
            return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }

    override fun count(element: Any?): Int {
        val candidate = representativeOf(element) ?: return 0
        return if (accepts(candidate)) delegate.count(candidate) else 0
    }

    private fun representativeOf(element: Any?): E? = try {
        @Suppress("UNCHECKED_CAST")
        val typed = element as E
        delegate.elementSet().firstOrNull { candidate -> comparator().compare(candidate, typed) == 0 }
    } catch (_: ClassCastException) {
        null
    } catch (_: NullPointerException) {
        null
    }

    override fun add(element: E, occurrences: Int): Int {
        require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
        if (occurrences == 0) return count(element)
        require(accepts(element)) { "element out of range: $element" }
        return delegate.add(element, occurrences)
    }

    override fun remove(element: Any?, occurrences: Int): Int {
        require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
        if (count(element) == 0) return 0
        return delegate.remove(element, occurrences)
    }

    override fun setCount(element: E, count: Int): Int {
        require(count >= 0) { "count cannot be negative: $count" }
        if (count == 0 && !accepts(element)) return 0
        require(accepts(element)) { "element out of range: $element" }
        return delegate.setCount(element, count)
    }

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private val entries = entrySet().iterator()
        private var current: Multiset.Entry<E>? = null
        private var remaining = 0
        private var canRemove = false

        override fun hasNext(): Boolean = remaining > 0 || entries.hasNext()
        override fun next(): E {
            if (remaining == 0) {
                current = entries.next()
                remaining = current!!.getCount()
            }
            remaining--
            canRemove = true
            return current!!.getElement()
        }

        override fun remove() {
            check(canRemove) { "no element to remove" }
            canRemove = false
            delegate.remove(current!!.getElement(), 1)
        }
    }

    override fun elementSet(): Set<E> = object : AbstractMutableSet<E>() {
        override val size: Int get() = elements().size
        override fun contains(element: E): Boolean = accepts(element) && delegate.count(element) > 0
        override fun iterator(): MutableIterator<E> = removalIterator(
            snapshot = elements(),
            remove = { element -> delegate.setCount(element, 0) },
        )
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun remove(element: E): Boolean {
            if (!contains(element)) return false
            delegate.setCount(element, 0)
            return true
        }
        override fun clear() = this@RangeSortedMultisetView.clear()
    }

    override fun entrySet(): Set<Multiset.Entry<E>> = object : AbstractMutableSet<Multiset.Entry<E>>() {
        override val size: Int get() = elements().size
        override fun contains(element: Multiset.Entry<E>): Boolean =
            element.getCount() > 0 && count(element.getElement()) == element.getCount()
        override fun iterator(): MutableIterator<Multiset.Entry<E>> = removalIterator(
            snapshot = elements().map { Multisets.immutableEntry(it, delegate.count(it)) },
            remove = { entry -> delegate.setCount(entry.getElement(), 0) },
        )
        override fun add(element: Multiset.Entry<E>): Boolean = throw UnsupportedOperationException()
        override fun remove(element: Multiset.Entry<E>): Boolean {
            if (!contains(element)) return false
            delegate.setCount(element.getElement(), 0)
            return true
        }
        override fun clear() = this@RangeSortedMultisetView.clear()
    }

    override fun clear() {
        for (element in elements()) delegate.setCount(element, 0)
    }

    override fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E> =
        RangeSortedMultisetView(this) { element ->
            val comparison = comparator().compare(element, upperBound)
            comparison < 0 || comparison == 0 && boundType == BoundType.CLOSED
        }

    override fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E> =
        RangeSortedMultisetView(this) { element ->
            val comparison = comparator().compare(element, lowerBound)
            comparison > 0 || comparison == 0 && boundType == BoundType.CLOSED
        }
}

internal class DescendingSortedMultisetView<E>(
    private val forward: SortedMultiset<E>,
) : AbstractSortedMultiset<E>() {
    private val reverseOrdering = Comparator<E> { first, second ->
        forward.comparator().compare(second, first)
    }

    override fun comparator(): Comparator<in E> = reverseOrdering
    override val size: Int get() = forward.size
    override fun count(element: Any?): Int = forward.count(element)
    override fun add(element: E, occurrences: Int): Int = forward.add(element, occurrences)
    override fun remove(element: Any?, occurrences: Int): Int = forward.remove(element, occurrences)
    override fun setCount(element: E, count: Int): Int = forward.setCount(element, count)
    override fun clear() {
        for (element in forward.elementSet().toList()) forward.setCount(element, 0)
    }

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private val entries = entrySet().iterator()
        private var current: Multiset.Entry<E>? = null
        private var remaining = 0
        private var canRemove = false
        override fun hasNext(): Boolean = remaining > 0 || entries.hasNext()
        override fun next(): E {
            if (remaining == 0) {
                current = entries.next()
                remaining = current!!.getCount()
            }
            remaining--
            canRemove = true
            return current!!.getElement()
        }
        override fun remove() {
            check(canRemove) { "no element to remove" }
            canRemove = false
            forward.remove(current!!.getElement(), 1)
        }
    }

    override fun elementSet(): Set<E> = object : AbstractMutableSet<E>() {
        override val size: Int get() = forward.elementSet().size
        override fun contains(element: E): Boolean = forward.elementSet().contains(element)
        override fun iterator(): MutableIterator<E> = removalIterator(
            snapshot = forward.elementSet().toList().asReversed(),
            remove = { element -> forward.setCount(element, 0) },
        )
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun remove(element: E): Boolean {
            val old = forward.count(element)
            if (old == 0) return false
            forward.setCount(element, 0)
            return true
        }
        override fun clear() = this@DescendingSortedMultisetView.clear()
    }

    override fun entrySet(): Set<Multiset.Entry<E>> = object : AbstractMutableSet<Multiset.Entry<E>>() {
        override val size: Int get() = forward.entrySet().size
        override fun contains(element: Multiset.Entry<E>): Boolean = forward.entrySet().contains(element)
        override fun iterator(): MutableIterator<Multiset.Entry<E>> = removalIterator(
            snapshot = forward.entrySet().toList().asReversed(),
            remove = { entry -> forward.setCount(entry.getElement(), 0) },
        )
        override fun add(element: Multiset.Entry<E>): Boolean = throw UnsupportedOperationException()
        override fun remove(element: Multiset.Entry<E>): Boolean {
            val entries = forward.entrySet() as? MutableSet<Multiset.Entry<E>>
                ?: throw UnsupportedOperationException()
            return entries.remove(element)
        }
        override fun clear() = this@DescendingSortedMultisetView.clear()
    }

    override fun firstEntry(): Multiset.Entry<E>? = forward.lastEntry()
    override fun lastEntry(): Multiset.Entry<E>? = forward.firstEntry()
    override fun pollFirstEntry(): Multiset.Entry<E>? = forward.pollLastEntry()
    override fun pollLastEntry(): Multiset.Entry<E>? = forward.pollFirstEntry()
    override fun descendingMultiset(): SortedMultiset<E> = forward

    override fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E> =
        forward.tailMultiset(upperBound, boundType).descendingMultiset()

    override fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E> =
        forward.headMultiset(lowerBound, boundType).descendingMultiset()

    override fun subMultiset(
        lowerBound: E,
        lowerBoundType: BoundType,
        upperBound: E,
        upperBoundType: BoundType,
    ): SortedMultiset<E> {
        require(comparator().compare(lowerBound, upperBound) <= 0) {
            "lowerBound ($lowerBound) must be <= upperBound ($upperBound)"
        }
        return forward.subMultiset(upperBound, upperBoundType, lowerBound, lowerBoundType)
            .descendingMultiset()
    }
}

private fun <T> removalIterator(
    snapshot: List<T>,
    remove: (T) -> Unit,
): MutableIterator<T> = object : MutableIterator<T> {
    private val iterator = snapshot.iterator()
    private var current: T? = null
    private var canRemove = false
    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): T {
        current = iterator.next()
        canRemove = true
        @Suppress("UNCHECKED_CAST")
        return current as T
    }
    override fun remove() {
        check(canRemove) { "no element to remove" }
        canRemove = false
        @Suppress("UNCHECKED_CAST")
        remove(current as T)
    }
}
