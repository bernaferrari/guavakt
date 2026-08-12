package com.bernaferrari.guavakt.collect

/** Guava ForwardingTable — forwards table operations to [delegate]. */
abstract class ForwardingTable<R, C, V> : Table<R, C, V> {
    protected abstract fun delegate(): Table<R, C, V>
    override fun contains(rowKey: Any?, columnKey: Any?): Boolean = delegate().contains(rowKey, columnKey)
    override fun containsRow(rowKey: Any?): Boolean = delegate().containsRow(rowKey)
    override fun containsColumn(columnKey: Any?): Boolean = delegate().containsColumn(columnKey)
    override fun containsValue(value: Any?): Boolean = delegate().containsValue(value)
    override fun get(rowKey: Any?, columnKey: Any?): V? = delegate().get(rowKey, columnKey)
    override fun isEmpty(): Boolean = delegate().isEmpty()
    override fun size(): Int = delegate().size()
    override fun clear() = delegate().clear()
    override fun put(rowKey: R, columnKey: C, value: V): V? = delegate().put(rowKey, columnKey, value)
    override fun remove(rowKey: Any?, columnKey: Any?): V? = delegate().remove(rowKey, columnKey)
    override fun row(rowKey: R): Map<C, V> = delegate().row(rowKey)
    override fun column(columnKey: C): Map<R, V> = delegate().column(columnKey)
    override fun cellSet(): Set<Table.Cell<R, C, V>> = delegate().cellSet()
    override fun rowKeySet(): Set<R> = delegate().rowKeySet()
    override fun columnKeySet(): Set<C> = delegate().columnKeySet()
    override fun values(): Collection<V> = delegate().values()
    override fun rowMap(): Map<R, Map<C, V>> = delegate().rowMap()
    override fun columnMap(): Map<C, Map<R, V>> = delegate().columnMap()
    override fun equals(other: Any?): Boolean = delegate() == other
    override fun hashCode(): Int = delegate().hashCode()
}
