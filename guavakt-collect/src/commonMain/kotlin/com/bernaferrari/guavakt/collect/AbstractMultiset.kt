package com.bernaferrari.guavakt.collect

/** Guava AbstractMultiset — skeletal multiset with count-based value semantics. */
abstract class AbstractMultiset<E> : AbstractMutableCollection<E>(), Multiset<E> {
    override fun add(element: E): Boolean {
        add(element, 1)
        return true
    }

    override fun add(element: E, occurrences: Int): Int {
        require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
        if (occurrences == 0) return count(element)
        throw UnsupportedOperationException()
    }

    override fun remove(element: E): Boolean = remove(element as Any?, 1) > 0

    override fun remove(element: Any?, occurrences: Int): Int {
        require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
        val oldCount = count(element)
        if (oldCount == 0 || occurrences == 0) return oldCount
        @Suppress("UNCHECKED_CAST")
        setCount(element as E, maxOf(0, oldCount - occurrences))
        return oldCount
    }

    override fun setCount(element: E, count: Int): Int {
        require(count >= 0) { "count cannot be negative: $count" }
        val oldCount = count(element)
        if (count > oldCount) add(element, count - oldCount)
        else if (count < oldCount) remove(element, oldCount - count)
        return oldCount
    }

    override fun contains(element: E): Boolean = count(element) > 0

    override fun clear() {
        val iterator = entrySet().iterator()
        if (iterator is MutableIterator<*>) {
            while (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        } else {
            for (element in elementSet().toList()) setCount(element, 0)
        }
    }

    override fun equals(other: Any?): Boolean = Multisets.equalsImpl(this, other)
    override fun hashCode(): Int = entrySet().hashCode()
    override fun toString(): String = entrySet().toString()
}
