package dev.guavakt.collect

object Multisets {
    fun <E> immutableEntry(element: E, count: Int): Multiset.Entry<E> {
        require(count >= 0)
        return object : Multiset.Entry<E> {
            override fun getElement(): E = element
            override fun getCount(): Int = count
            override fun equals(other: Any?): Boolean =
                other is Multiset.Entry<*> && element == other.getElement() && count == other.getCount()
            override fun hashCode(): Int = (element?.hashCode() ?: 0) xor count
            override fun toString(): String = if (count == 1) "$element" else "$element x $count"
        }
    }

    fun <E> frequency(multiset: Multiset<E>, element: Any?): Int = multiset.count(element)

    fun equalsImpl(multiset: Multiset<*>, object_: Any?): Boolean {
        if (object_ === multiset) return true
        if (object_ !is Multiset<*>) return false
        if (multiset.size != object_.size) return false
        for (e in multiset.elementSet()) {
            if (multiset.count(e) != object_.count(e)) return false
        }
        return true
    }

    fun <E> union(multiset1: Multiset<E>, multiset2: Multiset<out E>): Multiset<E> {
        return ReadOnlyComputedMultiset(
            elements = { unionElements(multiset1, multiset2) },
            counter = { element -> maxOf(multiset1.count(element), multiset2.count(element)) },
        )
    }

    fun <E> intersection(multiset1: Multiset<E>, multiset2: Multiset<*>): Multiset<E> {
        return ReadOnlyComputedMultiset(
            elements = { multiset1.elementSet().filter { multiset2.count(it) > 0 } },
            counter = { element -> minOf(multiset1.count(element), multiset2.count(element)) },
        )
    }

    fun <E> sum(multiset1: Multiset<out E>, multiset2: Multiset<out E>): Multiset<E> {
        return ReadOnlyComputedMultiset(
            elements = { unionElements(multiset1, multiset2) },
            counter = { element -> saturatedAdd(multiset1.count(element), multiset2.count(element)) },
        )
    }

    fun <E> difference(multiset1: Multiset<E>, multiset2: Multiset<*>): Multiset<E> {
        return ReadOnlyComputedMultiset(
            elements = { multiset1.elementSet().filter { multiset1.count(it) > multiset2.count(it) } },
            counter = { element -> maxOf(0, multiset1.count(element) - multiset2.count(element)) },
        )
    }

    fun <E> containsOccurrences(superMultiset: Multiset<E>, subMultiset: Multiset<*>): Boolean {
        for (e in subMultiset.elementSet()) {
            if (superMultiset.count(e) < subMultiset.count(e)) return false
        }
        return true
    }

    fun <E> removeOccurrences(multisetToModify: Multiset<E>, occurrencesToRemove: Multiset<*>): Boolean {
        var changed = false
        for (e in occurrencesToRemove.elementSet().toList()) {
            @Suppress("UNCHECKED_CAST")
            val element = e as E
            if (multisetToModify.remove(element, occurrencesToRemove.count(e)) > 0) changed = true
        }
        return changed
    }

    fun <E> retainOccurrences(multisetToModify: Multiset<E>, multisetToRetain: Multiset<*>): Boolean {
        var changed = false
        for (e in multisetToModify.elementSet().toList()) {
            val retain = multisetToRetain.count(e)
            if (retain < multisetToModify.count(e)) {
                multisetToModify.setCount(e, retain)
                changed = true
            }
        }
        return changed
    }

    fun <E> filter(unfiltered: Multiset<E>, predicate: (E) -> Boolean): Multiset<E> {
        return FilteredMultisetView(unfiltered, predicate)
    }

    fun <E> copyHighestCountFirst(multiset: Multiset<E>): Multiset<E> {
        val sorted = multiset.entrySet().sortedByDescending { it.getCount() }
        val result = HashMultiset.create<E>()
        for (entry in sorted) result.setCount(entry.getElement(), entry.getCount())
        return result
    }

    /** Returns a live read-only view of [multiset]. */
    fun <E> unmodifiableMultiset(multiset: Multiset<out E>): Multiset<E> {
        @Suppress("UNCHECKED_CAST")
        return UnmodifiableMultisetView(multiset as Multiset<E>)
    }

    /** Returns a live read-only sorted view of [multiset]. */
    fun <E> unmodifiableSortedMultiset(multiset: SortedMultiset<E>): SortedMultiset<E> =
        UnmodifiableSortedMultisetView(multiset)

    private class UnmodifiableMultisetView<E>(private val delegate: Multiset<E>) :
        AbstractMultiset<E>() {
        override val size: Int get() = delegate.size
        override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
            private val it = delegate.iterator()
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): E = it.next()
            override fun remove() = throw UnsupportedOperationException()
        }
        override fun count(element: Any?): Int = delegate.count(element)
        override fun add(element: E, occurrences: Int): Int = throw UnsupportedOperationException()
        override fun remove(element: Any?, occurrences: Int): Int = throw UnsupportedOperationException()
        override fun setCount(element: E, count: Int): Int = throw UnsupportedOperationException()
        override fun elementSet(): Set<E> = object : AbstractMutableSet<E>() {
            override val size: Int get() = delegate.elementSet().size
            override fun contains(element: E): Boolean = delegate.elementSet().contains(element)
            override fun iterator(): MutableIterator<E> = readOnlyIterator(delegate.elementSet().iterator())
            override fun add(element: E): Boolean = throw UnsupportedOperationException()
            override fun remove(element: E): Boolean = throw UnsupportedOperationException()
            override fun clear(): Unit = throw UnsupportedOperationException()
        }
        override fun entrySet(): Set<Multiset.Entry<E>> = object : AbstractMutableSet<Multiset.Entry<E>>() {
            override val size: Int get() = delegate.entrySet().size
            override fun contains(element: Multiset.Entry<E>): Boolean = delegate.entrySet().contains(element)
            override fun iterator(): MutableIterator<Multiset.Entry<E>> =
                readOnlyIterator(delegate.entrySet().iterator())
            override fun add(element: Multiset.Entry<E>): Boolean = throw UnsupportedOperationException()
            override fun remove(element: Multiset.Entry<E>): Boolean = throw UnsupportedOperationException()
            override fun clear(): Unit = throw UnsupportedOperationException()
        }
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun clear() = throw UnsupportedOperationException()

        private fun <T> readOnlyIterator(iterator: Iterator<T>): MutableIterator<T> =
            object : MutableIterator<T> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): T = iterator.next()
                override fun remove(): Unit = throw UnsupportedOperationException()
            }
    }

    private class UnmodifiableSortedMultisetView<E>(
        private val delegate: SortedMultiset<E>,
    ) : AbstractSortedMultiset<E>() {
        private var descendingView: SortedMultiset<E>? = null
        override val size: Int get() = delegate.size
        override fun comparator(): Comparator<in E> = delegate.comparator()
        override fun count(element: Any?): Int = delegate.count(element)
        override fun add(element: E, occurrences: Int): Int = throw UnsupportedOperationException()
        override fun remove(element: Any?, occurrences: Int): Int = throw UnsupportedOperationException()
        override fun setCount(element: E, count: Int): Int = throw UnsupportedOperationException()
        override fun clear(): Unit = throw UnsupportedOperationException()
        override fun pollFirstEntry(): Multiset.Entry<E>? = throw UnsupportedOperationException()
        override fun pollLastEntry(): Multiset.Entry<E>? = throw UnsupportedOperationException()

        override fun iterator(): MutableIterator<E> = readOnlyIterator(delegate.iterator())

        override fun elementSet(): Set<E> = readOnlyLiveSet(
            size = { delegate.elementSet().size },
            contains = { element -> delegate.elementSet().contains(element) },
            iterator = { delegate.elementSet().iterator() },
        )

        override fun entrySet(): Set<Multiset.Entry<E>> = readOnlyLiveSet(
            size = { delegate.entrySet().size },
            contains = { entry -> delegate.entrySet().contains(entry) },
            iterator = { delegate.entrySet().iterator() },
        )

        override fun firstEntry(): Multiset.Entry<E>? = delegate.firstEntry()
        override fun lastEntry(): Multiset.Entry<E>? = delegate.lastEntry()
        override fun descendingMultiset(): SortedMultiset<E> {
            descendingView?.let { return it }
            val reversed = UnmodifiableSortedMultisetView(delegate.descendingMultiset())
            reversed.descendingView = this
            descendingView = reversed
            return reversed
        }
        override fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E> =
            UnmodifiableSortedMultisetView(delegate.headMultiset(upperBound, boundType))
        override fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E> =
            UnmodifiableSortedMultisetView(delegate.tailMultiset(lowerBound, boundType))
        override fun subMultiset(
            lowerBound: E,
            lowerBoundType: BoundType,
            upperBound: E,
            upperBoundType: BoundType,
        ): SortedMultiset<E> = UnmodifiableSortedMultisetView(
            delegate.subMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType),
        )

        private fun <T> readOnlyIterator(iterator: Iterator<T>): MutableIterator<T> =
            object : MutableIterator<T> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): T = iterator.next()
                override fun remove(): Unit = throw UnsupportedOperationException()
            }
    }

    private class ReadOnlyComputedMultiset<E>(
        private val elements: () -> List<E>,
        private val counter: (Any?) -> Int,
    ) : AbstractMultiset<E>() {
        override val size: Int
            get() {
                var total = 0L
                for (element in elements()) total += counter(element).toLong()
                return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }

        override fun count(element: Any?): Int = counter(element)
        override fun add(element: E, occurrences: Int): Int = throw UnsupportedOperationException()
        override fun remove(element: Any?, occurrences: Int): Int = throw UnsupportedOperationException()
        override fun setCount(element: E, count: Int): Int = throw UnsupportedOperationException()
        override fun clear(): Unit = throw UnsupportedOperationException()

        override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
            private val entries = entrySet().iterator()
            private var current: Multiset.Entry<E>? = null
            private var remaining = 0
            override fun hasNext(): Boolean = remaining > 0 || entries.hasNext()
            override fun next(): E {
                if (remaining == 0) {
                    current = entries.next()
                    remaining = current!!.getCount()
                }
                remaining--
                return current!!.getElement()
            }
            override fun remove(): Unit = throw UnsupportedOperationException()
        }

        override fun elementSet(): Set<E> = readOnlyLiveSet(
            size = { elements().size },
            contains = { candidate -> counter(candidate) > 0 },
            iterator = { elements().iterator() },
        )

        override fun entrySet(): Set<Multiset.Entry<E>> = readOnlyLiveSet(
            size = { elements().size },
            contains = { candidate ->
                candidate.getCount() > 0 && counter(candidate.getElement()) == candidate.getCount()
            },
            iterator = {
                elements().map { element -> immutableEntry(element, counter(element)) }.iterator()
            },
        )
    }

    private class FilteredMultisetView<E>(
        private val delegate: Multiset<E>,
        private val predicate: (E) -> Boolean,
    ) : AbstractMultiset<E>() {
        private fun matches(candidate: Any?): Boolean {
            for (element in delegate.elementSet()) {
                if (element == candidate && predicate(element)) return true
            }
            return false
        }

        private fun filteredElements(): List<E> = delegate.elementSet().filter(predicate)

        override val size: Int
            get() {
                var total = 0L
                for (element in filteredElements()) total += delegate.count(element).toLong()
                return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }

        override fun count(element: Any?): Int {
            if (!matches(element)) return 0
            return delegate.count(element)
        }

        override fun add(element: E, occurrences: Int): Int {
            require(predicate(element)) { "element does not match predicate: $element" }
            return delegate.add(element, occurrences)
        }

        override fun remove(element: Any?, occurrences: Int): Int {
            require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
            if (!matches(element)) return 0
            return delegate.remove(element, occurrences)
        }

        override fun setCount(element: E, count: Int): Int {
            require(count >= 0) { "count cannot be negative: $count" }
            if (count > 0) require(predicate(element)) { "element does not match predicate: $element" }
            return if (predicate(element)) delegate.setCount(element, count) else 0
        }

        override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
            private val entries = filteredElements().iterator()
            private var current: E? = null
            private var remaining = 0
            private var canRemove = false
            override fun hasNext(): Boolean = remaining > 0 || entries.hasNext()
            override fun next(): E {
                if (remaining == 0) {
                    current = entries.next()
                    remaining = delegate.count(current)
                }
                remaining--
                canRemove = true
                @Suppress("UNCHECKED_CAST")
                return current as E
            }
            override fun remove() {
                check(canRemove) { "no element to remove" }
                canRemove = false
                delegate.remove(current, 1)
            }
        }

        override fun elementSet(): Set<E> = mutableFilteredSet(
            elements = ::filteredElements,
            removeAll = { element -> delegate.setCount(element, 0) > 0 },
        )

        override fun entrySet(): Set<Multiset.Entry<E>> = object : AbstractMutableSet<Multiset.Entry<E>>() {
            override val size: Int get() = filteredElements().size
            override fun contains(element: Multiset.Entry<E>): Boolean =
                element.getCount() > 0 && count(element.getElement()) == element.getCount()
            override fun iterator(): MutableIterator<Multiset.Entry<E>> {
                val snapshot = filteredElements().map { immutableEntry(it, delegate.count(it)) }.iterator()
                var current: Multiset.Entry<E>? = null
                var canRemove = false
                return object : MutableIterator<Multiset.Entry<E>> {
                    override fun hasNext(): Boolean = snapshot.hasNext()
                    override fun next(): Multiset.Entry<E> {
                        current = snapshot.next()
                        canRemove = true
                        return current!!
                    }
                    override fun remove() {
                        check(canRemove) { "no entry to remove" }
                        canRemove = false
                        delegate.setCount(current!!.getElement(), 0)
                    }
                }
            }
            override fun add(element: Multiset.Entry<E>): Boolean = throw UnsupportedOperationException()
            override fun remove(element: Multiset.Entry<E>): Boolean {
                if (!contains(element)) return false
                delegate.setCount(element.getElement(), 0)
                return true
            }
            override fun clear() = this@FilteredMultisetView.clear()
        }

        override fun clear() {
            for (element in filteredElements()) delegate.setCount(element, 0)
        }
    }

    private fun <E> unionElements(first: Multiset<out E>, second: Multiset<out E>): List<E> {
        val elements = LinkedHashSet<E>()
        elements.addAll(first.elementSet())
        elements.addAll(second.elementSet())
        return elements.toList()
    }

    private fun saturatedAdd(first: Int, second: Int): Int =
        (first.toLong() + second.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun <E> readOnlyLiveSet(
        size: () -> Int,
        contains: (E) -> Boolean,
        iterator: () -> Iterator<E>,
    ): Set<E> = object : AbstractMutableSet<E>() {
        override val size: Int get() = size()
        override fun contains(element: E): Boolean = contains(element)
        override fun iterator(): MutableIterator<E> {
            val source = iterator()
            return object : MutableIterator<E> {
                override fun hasNext(): Boolean = source.hasNext()
                override fun next(): E = source.next()
                override fun remove(): Unit = throw UnsupportedOperationException()
            }
        }
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun remove(element: E): Boolean = throw UnsupportedOperationException()
        override fun clear(): Unit = throw UnsupportedOperationException()
    }

    private fun <E> mutableFilteredSet(
        elements: () -> List<E>,
        removeAll: (E) -> Boolean,
    ): Set<E> = object : AbstractMutableSet<E>() {
        override val size: Int get() = elements().size
        override fun contains(element: E): Boolean = elements().contains(element)
        override fun iterator(): MutableIterator<E> {
            val snapshot = elements().iterator()
            var current: E? = null
            var canRemove = false
            return object : MutableIterator<E> {
                override fun hasNext(): Boolean = snapshot.hasNext()
                override fun next(): E {
                    current = snapshot.next()
                    canRemove = true
                    @Suppress("UNCHECKED_CAST")
                    return current as E
                }
                override fun remove() {
                    check(canRemove) { "no element to remove" }
                    canRemove = false
                    @Suppress("UNCHECKED_CAST")
                    removeAll(current as E)
                }
            }
        }
        override fun add(element: E): Boolean = throw UnsupportedOperationException()
        override fun remove(element: E): Boolean = removeAll(element)
        override fun clear() {
            for (element in elements()) removeAll(element)
        }
    }
}
