package dev.guavakt.collect

/**
 * A [Table] whose row-key and outer row-map iteration follow one stable comparator order.
 *
 * Common Kotlin has no portable `SortedSet` or `SortedMap` interface, so the Guava return types
 * are represented as ordinary read-only interfaces with a documented ordering contract. The
 * returned collections remain live and preserve the mutation behavior of the underlying table.
 */
interface RowSortedTable<R, C, V> : Table<R, C, V> {
    fun rowKeySetSorted(): Set<R>
    fun rowMapSorted(): Map<R, Map<C, V>>
}
