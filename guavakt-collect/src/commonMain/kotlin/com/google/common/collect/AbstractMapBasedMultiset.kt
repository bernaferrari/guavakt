package dev.guavakt.collect

/**
 * Map-backed multiset skeleton with Guava-compatible count and live-view semantics.
 *
 * The backing map must be empty when this class is constructed and must not be
 * mutated except through this multiset or the views returned by it.
 */
abstract class AbstractMapBasedMultiset<E> protected constructor(
    private val backingMap: MutableMap<E, Int> = LinkedHashMap(),
) : AbstractMultiset<E>() {

    private var totalOccurrences: Long = 0

    override val size: Int
        get() = totalOccurrences.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    override fun count(element: Any?): Int = backingMap[element] ?: 0

    override fun add(element: E, occurrences: Int): Int {
        require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
        val oldCount = backingMap[element] ?: 0
        if (occurrences == 0) return oldCount
        require(occurrences <= Int.MAX_VALUE - oldCount) {
            "too many occurrences: ${oldCount.toLong() + occurrences}"
        }
        backingMap[element] = oldCount + occurrences
        totalOccurrences += occurrences.toLong()
        return oldCount
    }

    override fun remove(element: Any?, occurrences: Int): Int {
        require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
        val oldCount = backingMap[element] ?: return 0
        if (occurrences == 0) return oldCount
        val removed = minOf(oldCount, occurrences)
        if (removed == oldCount) {
            backingMap.remove(element)
        } else {
            @Suppress("UNCHECKED_CAST")
            backingMap[element as E] = oldCount - removed
        }
        totalOccurrences -= removed.toLong()
        return oldCount
    }

    override fun setCount(element: E, count: Int): Int {
        require(count >= 0) { "count cannot be negative: $count" }
        val oldCount = backingMap[element] ?: 0
        if (count == 0) backingMap.remove(element) else backingMap[element] = count
        totalOccurrences += count.toLong() - oldCount.toLong()
        return oldCount
    }

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private val entries = backingMap.entries.iterator()
        private var current: MutableMap.MutableEntry<E, Int>? = null
        private var remaining = 0
        private var canRemove = false

        override fun hasNext(): Boolean = remaining > 0 || entries.hasNext()

        override fun next(): E {
            if (remaining == 0) {
                current = entries.next()
                remaining = current!!.value
            }
            remaining--
            canRemove = true
            return current!!.key
        }

        override fun remove() {
            check(canRemove) { "no element to remove" }
            canRemove = false
            val entry = current!!
            val oldCount = entry.value
            if (oldCount == 1) {
                entries.remove()
            } else {
                entry.setValue(oldCount - 1)
            }
            totalOccurrences--
        }
    }

    override fun elementSet(): Set<E> = object : AbstractMutableSet<E>() {
        override val size: Int get() = backingMap.size
        override fun contains(element: E): Boolean = backingMap.containsKey(element)

        override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
            private val entries = backingMap.entries.iterator()
            private var currentCount = 0
            private var canRemove = false

            override fun hasNext(): Boolean = entries.hasNext()
            override fun next(): E {
                val entry = entries.next()
                currentCount = entry.value
                canRemove = true
                return entry.key
            }

            override fun remove() {
                check(canRemove) { "no element to remove" }
                canRemove = false
                entries.remove()
                totalOccurrences -= currentCount.toLong()
            }
        }

        override fun add(element: E): Boolean = throw UnsupportedOperationException()

        override fun remove(element: E): Boolean {
            val oldCount = backingMap.remove(element) ?: return false
            totalOccurrences -= oldCount.toLong()
            return true
        }

        override fun clear() = this@AbstractMapBasedMultiset.clear()
    }

    override fun entrySet(): Set<Multiset.Entry<E>> = object : AbstractMutableSet<Multiset.Entry<E>>() {
        override val size: Int get() = backingMap.size

        override fun contains(element: Multiset.Entry<E>): Boolean {
            val count = element.getCount()
            return count > 0 && this@AbstractMapBasedMultiset.count(element.getElement()) == count
        }

        override fun iterator(): MutableIterator<Multiset.Entry<E>> = object : MutableIterator<Multiset.Entry<E>> {
            private val entries = backingMap.entries.iterator()
            private var currentCount = 0
            private var canRemove = false

            override fun hasNext(): Boolean = entries.hasNext()
            override fun next(): Multiset.Entry<E> {
                val entry = entries.next()
                currentCount = entry.value
                canRemove = true
                return Multisets.immutableEntry(entry.key, entry.value)
            }

            override fun remove() {
                check(canRemove) { "no entry to remove" }
                canRemove = false
                entries.remove()
                totalOccurrences -= currentCount.toLong()
            }
        }

        override fun add(element: Multiset.Entry<E>): Boolean = throw UnsupportedOperationException()

        override fun remove(element: Multiset.Entry<E>): Boolean {
            if (!contains(element)) return false
            val oldCount = backingMap.remove(element.getElement()) ?: return false
            totalOccurrences -= oldCount.toLong()
            return true
        }

        override fun clear() = this@AbstractMapBasedMultiset.clear()
    }

    override fun clear() {
        backingMap.clear()
        totalOccurrences = 0
    }
}
