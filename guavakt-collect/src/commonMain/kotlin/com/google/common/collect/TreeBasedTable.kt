package dev.guavakt.collect

/**
 * Guava TreeBasedTable — Table with sorted rows and columns (TreeMap-backed).
 */
class TreeBasedTable<R, C, V> private constructor(
    private val rowComparator: Comparator<in R>,
    private val columnComparator: Comparator<in C>,
    backing: MutableMap<R, MutableMap<C, V>>,
) : StandardRowSortedTable<R, C, V>(backing, {
    ComparatorTreeMap<C, V>(columnComparator)
}) {
    fun rowComparator(): Comparator<in R> = rowComparator
    fun columnComparator(): Comparator<in C> = columnComparator

    /** Globally column-sorted live view, even when different rows contain disjoint columns. */
    override fun columnKeySet(): Set<C> =
        ComparatorOrderedTableSet(super.columnKeySet(), columnComparator)

    companion object {
        fun <R : Comparable<R>, C : Comparable<C>, V> create(): TreeBasedTable<R, C, V> {
            val rows = Comparator<R> { left, right -> left.compareTo(right) }
            val columns = Comparator<C> { left, right -> left.compareTo(right) }
            return TreeBasedTable(rows, columns, ComparatorTreeMap(rows))
        }

        fun <R, C, V> create(
            rowComparator: Comparator<in R>,
            columnComparator: Comparator<in C>,
        ): TreeBasedTable<R, C, V> {
            val rows: MutableMap<R, MutableMap<C, V>> = ComparatorTreeMap(rowComparator)
            return TreeBasedTable(rowComparator, columnComparator, rows)
        }

        fun <R, C, V> create(table: TreeBasedTable<R, C, V>): TreeBasedTable<R, C, V> {
            val result = create<R, C, V>(table.rowComparator(), table.columnComparator())
            result.putAll(table)
            return result
        }
    }
}

private class ComparatorOrderedTableSet<E>(
    private val source: Set<E>,
    private val comparator: Comparator<in E>,
) : AbstractMutableSet<E>() {
    override val size: Int get() = source.size
    override fun contains(element: E): Boolean = source.contains(element)
    override fun iterator(): MutableIterator<E> {
        val iterator = source.sortedWith(comparator).iterator()
        var current: E? = null
        var canRemove = false
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E {
                val value = iterator.next()
                current = value
                canRemove = true
                return value
            }
            override fun remove() {
                if (!canRemove) throw IllegalStateException()
                @Suppress("UNCHECKED_CAST")
                (source as MutableSet<E>).remove(current as E)
                canRemove = false
            }
        }
    }
    override fun add(element: E): Nothing = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (source as MutableSet<E>).remove(element)
    }
    override fun clear() {
        @Suppress("UNCHECKED_CAST")
        (source as MutableSet<E>).clear()
    }
}
