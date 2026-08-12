package com.bernaferrari.guavakt.collect

/**
 * Guava AbstractTable — skeletal Table implementation.
 */
abstract class AbstractTable<R, C, V> : Table<R, C, V> {
    override fun contains(rowKey: Any?, columnKey: Any?): Boolean = get(rowKey, columnKey) != null
    override fun containsRow(rowKey: Any?): Boolean = rowKeySet().contains(rowKey)
    override fun containsColumn(columnKey: Any?): Boolean = columnKeySet().contains(columnKey)
    override fun containsValue(value: Any?): Boolean = values().contains(value)
    override fun isEmpty(): Boolean = size() == 0

    override fun clear() {
        for (cell in cellSet().toList()) remove(cell.getRowKey(), cell.getColumnKey())
    }

    fun putAll(table: Table<out R, out C, out V>) {
        for (cell in table.cellSet()) put(cell.getRowKey(), cell.getColumnKey(), cell.getValue())
    }

    override fun remove(rowKey: Any?, columnKey: Any?): V? {
        // Default: subclasses should override; best-effort via row map if mutable
        return null
    }

    override fun put(rowKey: R, columnKey: C, value: V): V? {
        throw UnsupportedOperationException()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Table<*, *, *>) return false
        return cellSet() == other.cellSet()
    }

    override fun hashCode(): Int = cellSet().hashCode()

    override fun toString(): String = rowMap().toString()
}
