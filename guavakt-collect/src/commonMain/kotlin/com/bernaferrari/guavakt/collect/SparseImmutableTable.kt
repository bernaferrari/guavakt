package com.bernaferrari.guavakt.collect

/** Guava SparseImmutableTable — list-of-cells sparse immutable table. */
internal class SparseImmutableTable<R, C, V>(
    cellList: List<Table.Cell<R, C, V>>,
) : ImmutableTable<R, C, V>() {
    private val delegate = RegularImmutableTable(cellList)
    override fun get(rowKey: Any?, columnKey: Any?): V? = delegate.get(rowKey, columnKey)
    override fun size(): Int = delegate.size()
    override fun cellSet(): Set<Table.Cell<R, C, V>> = delegate.cellSet()
    override fun rowKeySet(): Set<R> = delegate.rowKeySet()
    override fun columnKeySet(): Set<C> = delegate.columnKeySet()
    override fun values(): Collection<V> = delegate.values()
    override fun row(rowKey: R): Map<C, V> = delegate.row(rowKey)
    override fun column(columnKey: C): Map<R, V> = delegate.column(columnKey)
    override fun rowMap(): Map<R, Map<C, V>> = delegate.rowMap()
    override fun columnMap(): Map<C, Map<R, V>> = delegate.columnMap()
}
