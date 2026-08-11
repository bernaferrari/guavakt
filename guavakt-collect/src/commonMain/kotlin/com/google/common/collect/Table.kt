package dev.guavakt.collect

interface Table<R, C, V> {
    fun contains(rowKey: Any?, columnKey: Any?): Boolean
    fun containsRow(rowKey: Any?): Boolean
    fun containsColumn(columnKey: Any?): Boolean
    fun containsValue(value: Any?): Boolean
    operator fun get(rowKey: Any?, columnKey: Any?): V?
    fun isEmpty(): Boolean
    fun size(): Int
    fun clear()
    fun put(rowKey: R, columnKey: C, value: V): V?
    fun remove(rowKey: Any?, columnKey: Any?): V?
    fun row(rowKey: R): Map<C, V>
    fun column(columnKey: C): Map<R, V>
    fun cellSet(): Set<Cell<R, C, V>>
    fun rowKeySet(): Set<R>
    fun columnKeySet(): Set<C>
    fun values(): Collection<V>
    fun rowMap(): Map<R, Map<C, V>>
    fun columnMap(): Map<C, Map<R, V>>
    interface Cell<R, C, V> {
        fun getRowKey(): R
        fun getColumnKey(): C
        fun getValue(): V
    }
}
