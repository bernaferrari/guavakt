package com.bernaferrari.guavakt.collect

/** Guava DenseImmutableTable — array-backed dense immutable table (used when density is high). */
internal class DenseImmutableTable<R, C, V>(
    private val rowKeys: List<R>,
    private val columnKeys: List<C>,
    private val values: Array<Array<V?>>,
    private val cellCount: Int,
) : ImmutableTable<R, C, V>() {
    private val rowIndex = rowKeys.withIndex().associate { it.value to it.index }
    private val columnIndex = columnKeys.withIndex().associate { it.value to it.index }

    override fun get(rowKey: Any?, columnKey: Any?): V? {
        val ri = rowIndex[rowKey] ?: return null
        val ci = columnIndex[columnKey] ?: return null
        return values[ri][ci]
    }
    override fun size(): Int = cellCount
    override fun cellSet(): Set<Table.Cell<R, C, V>> {
        val result = LinkedHashSet<Table.Cell<R, C, V>>()
        for (r in rowKeys.indices) for (c in columnKeys.indices) {
            values[r][c]?.let { result.add(Tables.immutableCell(rowKeys[r], columnKeys[c], it)) }
        }
        return result
    }
    override fun rowKeySet(): Set<R> = rowKeys.toSet()
    override fun columnKeySet(): Set<C> = columnKeys.toSet()
    override fun values(): Collection<V> = cellSet().map { it.getValue() }
    override fun row(rowKey: R): Map<C, V> {
        val ri = rowIndex[rowKey] ?: return emptyMap()
        val result = LinkedHashMap<C, V>()
        for (c in columnKeys.indices) values[ri][c]?.let { result[columnKeys[c]] = it }
        return result
    }
    override fun column(columnKey: C): Map<R, V> {
        val ci = columnIndex[columnKey] ?: return emptyMap()
        val result = LinkedHashMap<R, V>()
        for (r in rowKeys.indices) values[r][ci]?.let { result[rowKeys[r]] = it }
        return result
    }
    override fun rowMap(): Map<R, Map<C, V>> = rowKeys.associateWith { row(it) }
    override fun columnMap(): Map<C, Map<R, V>> = columnKeys.associateWith { column(it) }
}
