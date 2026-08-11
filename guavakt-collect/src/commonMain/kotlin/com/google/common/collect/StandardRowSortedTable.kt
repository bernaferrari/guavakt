package dev.guavakt.collect

/**
 * Guava StandardRowSortedTable — StandardTable with sorted row keys.
 */
open class StandardRowSortedTable<R, C, V>(
    backingMap: MutableMap<R, MutableMap<C, V>>,
    factory: () -> MutableMap<C, V>,
) : StandardTable<R, C, V>(backingMap, factory), RowSortedTable<R, C, V> {
    override fun rowKeySetSorted(): Set<R> = rowKeySet()
    override fun rowMapSorted(): Map<R, Map<C, V>> = rowMap()
}
