package dev.guavakt.collect

/** Guava SingletonImmutableTable — one-cell immutable table. */
internal class SingletonImmutableTable<R, C, V>(
    private val singleRowKey: R,
    private val singleColumnKey: C,
    private val singleValue: V,
) : ImmutableTable<R, C, V>() {
    override fun get(rowKey: Any?, columnKey: Any?): V? =
        if (singleRowKey == rowKey && singleColumnKey == columnKey) singleValue else null
    override fun size(): Int = 1
    override fun cellSet(): Set<Table.Cell<R, C, V>> =
        unmodifiableMutableSet(setOf(Tables.immutableCell(singleRowKey, singleColumnKey, singleValue)))
    override fun rowKeySet(): Set<R> = unmodifiableMutableSet(setOf(singleRowKey))
    override fun columnKeySet(): Set<C> = unmodifiableMutableSet(setOf(singleColumnKey))
    override fun values(): Collection<V> = unmodifiableMutableCollection(listOf(singleValue))
    override fun row(rowKey: R): Map<C, V> =
        unmodifiableMutableMap(
            if (rowKey == singleRowKey) mapOf(singleColumnKey to singleValue) else emptyMap(),
        )
    override fun column(columnKey: C): Map<R, V> =
        unmodifiableMutableMap(
            if (columnKey == singleColumnKey) mapOf(singleRowKey to singleValue) else emptyMap(),
        )
    override fun rowMap(): Map<R, Map<C, V>> =
        unmodifiableMutableMap(mapOf(singleRowKey to row(singleRowKey)))
    override fun columnMap(): Map<C, Map<R, V>> =
        unmodifiableMutableMap(mapOf(singleColumnKey to column(singleColumnKey)))
}
