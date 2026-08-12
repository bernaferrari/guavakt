package com.bernaferrari.guavakt.collect

/** Guava RegularImmutableTable — multi-cell immutable table. */
internal class RegularImmutableTable<R, C, V>(
    private val cellList: List<Table.Cell<R, C, V>>,
) : ImmutableTable<R, C, V>() {
    private val backing: Map<R, Map<C, V>> = run {
        val m = LinkedHashMap<R, MutableMap<C, V>>()
        for (cell in cellList) {
            m.getOrPut(cell.getRowKey()) { LinkedHashMap() }[cell.getColumnKey()] = cell.getValue()
        }
        val immutableRows = LinkedHashMap<R, Map<C, V>>()
        for ((rowKey, row) in m) immutableRows[rowKey] = unmodifiableMutableMap(row)
        unmodifiableMutableMap(immutableRows)
    }

    override fun get(rowKey: Any?, columnKey: Any?): V? = backing[rowKey]?.get(columnKey)
    override fun size(): Int = cellList.size
    override fun cellSet(): Set<Table.Cell<R, C, V>> =
        unmodifiableMutableSet(LinkedHashSet(cellList))
    override fun rowKeySet(): Set<R> = unmodifiableMutableSet(backing.keys)
    override fun columnKeySet(): Set<C> = unmodifiableMutableSet(
        buildSet { for (row in backing.values) addAll(row.keys) },
    )
    override fun values(): Collection<V> =
        unmodifiableMutableCollection(cellList.map { it.getValue() })
    override fun row(rowKey: R): Map<C, V> =
        backing[rowKey] ?: unmodifiableMutableMap(emptyMap())
    override fun column(columnKey: C): Map<R, V> {
        val result = LinkedHashMap<R, V>()
        for ((r, cols) in backing) cols[columnKey]?.let { result[r] = it }
        return unmodifiableMutableMap(result)
    }
    override fun rowMap(): Map<R, Map<C, V>> = backing
    override fun columnMap(): Map<C, Map<R, V>> {
        val result = LinkedHashMap<C, MutableMap<R, V>>()
        for ((r, cols) in backing) for ((c, v) in cols) {
            result.getOrPut(c) { LinkedHashMap() }[r] = v
        }
        val immutableColumns = LinkedHashMap<C, Map<R, V>>()
        for ((columnKey, column) in result) {
            immutableColumns[columnKey] = unmodifiableMutableMap(column)
        }
        return unmodifiableMutableMap(immutableColumns)
    }
}
