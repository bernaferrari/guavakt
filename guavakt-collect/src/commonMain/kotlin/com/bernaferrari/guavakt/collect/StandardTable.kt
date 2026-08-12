package com.bernaferrari.guavakt.collect

/**
 * Guava [StandardTable]: a map-of-maps table whose collection and map accessors are live views.
 * Every mutation route updates the table's cell count and removes empty row maps.
 */
open class StandardTable<R, C, V>(
    protected val backingMap: MutableMap<R, MutableMap<C, V>>,
    private val factory: () -> MutableMap<C, V>,
) : AbstractTable<R, C, V>() {
    private var sizeField = backingMap.values.sumOf { it.size }

    override fun contains(rowKey: Any?, columnKey: Any?): Boolean =
        backingMap[rowKey]?.containsKey(columnKey) == true

    override fun containsColumn(columnKey: Any?): Boolean =
        backingMap.values.any { it.containsKey(columnKey) }

    override fun containsRow(rowKey: Any?): Boolean = backingMap.containsKey(rowKey)

    override fun containsValue(value: Any?): Boolean =
        backingMap.values.any { value in it.values }

    override fun get(rowKey: Any?, columnKey: Any?): V? = backingMap[rowKey]?.get(columnKey)
    override fun isEmpty(): Boolean = sizeField == 0
    override fun size(): Int = sizeField

    override fun clear() {
        backingMap.values.forEach { it.clear() }
        backingMap.clear()
        sizeField = 0
    }

    override fun put(rowKey: R, columnKey: C, value: V): V? {
        val row = backingMap.getOrPut(rowKey) { factory() }
        val hadCell = row.containsKey(columnKey)
        val previous = row.put(columnKey, value)
        if (!hadCell) sizeField++
        return previous
    }

    override fun remove(rowKey: Any?, columnKey: Any?): V? {
        val row = backingMap[rowKey] ?: return null
        if (!row.containsKey(columnKey)) return null
        val previous = row.remove(columnKey)
        sizeField--
        if (row.isEmpty()) backingMap.remove(rowKey)
        return previous
    }

    override fun row(rowKey: R): Map<C, V> = RowView(rowKey)
    override fun column(columnKey: C): Map<R, V> = ColumnView(columnKey)
    override fun cellSet(): Set<Table.Cell<R, C, V>> = CellSetView()
    override fun rowKeySet(): Set<R> = RowKeySetView()
    override fun columnKeySet(): Set<C> = ColumnKeySetView()
    override fun values(): Collection<V> = ValuesView()
    override fun rowMap(): Map<R, Map<C, V>> = RowMapView()
    override fun columnMap(): Map<C, Map<R, V>> = ColumnMapView()

    private fun removeRow(rowKey: Any?): Map<C, V>? {
        val row = backingMap.remove(rowKey) ?: return null
        val snapshot = LinkedHashMap(row)
        sizeField -= row.size
        row.clear()
        return snapshot
    }

    private fun removeColumn(columnKey: Any?): Map<R, V>? {
        val removed = LinkedHashMap<R, V>()
        // Snapshot row keys: Wasm's map-entry references are invalidated when a nested row map
        // mutates, even though the outer map is unchanged. Guava's observable result is the same.
        for (rowKey in backingMap.keys.toList()) {
            val row = backingMap[rowKey] ?: continue
            if (row.containsKey(columnKey)) {
                @Suppress("UNCHECKED_CAST")
                val typedColumn = columnKey as C
                @Suppress("UNCHECKED_CAST")
                removed[rowKey] = row.remove(typedColumn) as V
                sizeField--
                if (row.isEmpty()) backingMap.remove(rowKey)
            }
        }
        return removed.takeIf { it.isNotEmpty() }
    }

    private fun coordinates(): List<Pair<R, C>> = buildList(sizeField) {
        for ((rowKey, row) in backingMap) for (columnKey in row.keys) add(rowKey to columnKey)
    }

    private inner class RowView(private val rowKey: R) : AbstractMutableMap<C, V>() {
        override val size: Int get() = backingMap[rowKey]?.size ?: 0
        override fun containsKey(key: C): Boolean = backingMap[rowKey]?.containsKey(key) == true
        override fun get(key: C): V? = this@StandardTable.get(rowKey, key)
        override fun put(key: C, value: V): V? = this@StandardTable.put(rowKey, key, value)
        override fun remove(key: C): V? = this@StandardTable.remove(rowKey, key)
        override fun clear() { removeRow(rowKey) }

        override val entries: MutableSet<MutableMap.MutableEntry<C, V>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<C, V>>() {
                override val size: Int get() = this@RowView.size

                override fun iterator(): MutableIterator<MutableMap.MutableEntry<C, V>> {
                    val columns = backingMap[rowKey]?.keys?.toList().orEmpty().iterator()
                    var current: C? = null
                    var canRemove = false
                    return object : MutableIterator<MutableMap.MutableEntry<C, V>> {
                        override fun hasNext(): Boolean = columns.hasNext()
                        override fun next(): MutableMap.MutableEntry<C, V> {
                            val columnKey = columns.next()
                            current = columnKey
                            canRemove = true
                            return tableMapEntry(
                                columnKey,
                                { this@RowView[columnKey] as V },
                                { newValue -> this@RowView.put(columnKey, newValue) as V },
                            )
                        }
                        override fun remove() {
                            if (!canRemove) throw IllegalStateException()
                            this@RowView.remove(current as C)
                            canRemove = false
                        }
                    }
                }

                override fun add(element: MutableMap.MutableEntry<C, V>): Nothing =
                    throw UnsupportedOperationException()
                override fun clear() = this@RowView.clear()
            }
    }

    private inner class ColumnView(private val columnKey: C) : AbstractMutableMap<R, V>() {
        override val size: Int get() = backingMap.values.count { it.containsKey(columnKey) }
        override fun containsKey(key: R): Boolean = this@StandardTable.contains(key, columnKey)
        override fun get(key: R): V? = this@StandardTable.get(key, columnKey)
        override fun put(key: R, value: V): V? = this@StandardTable.put(key, columnKey, value)
        override fun remove(key: R): V? = this@StandardTable.remove(key, columnKey)
        override fun clear() { removeColumn(columnKey) }

        override val entries: MutableSet<MutableMap.MutableEntry<R, V>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<R, V>>() {
                override val size: Int get() = this@ColumnView.size

                override fun iterator(): MutableIterator<MutableMap.MutableEntry<R, V>> {
                    val rows = backingMap.keys.filter {
                        backingMap[it]?.containsKey(columnKey) == true
                    }.iterator()
                    var current: R? = null
                    var canRemove = false
                    return object : MutableIterator<MutableMap.MutableEntry<R, V>> {
                        override fun hasNext(): Boolean = rows.hasNext()
                        override fun next(): MutableMap.MutableEntry<R, V> {
                            val rowKey = rows.next()
                            current = rowKey
                            canRemove = true
                            return tableMapEntry(
                                rowKey,
                                { this@ColumnView[rowKey] as V },
                                { newValue -> this@ColumnView.put(rowKey, newValue) as V },
                            )
                        }
                        override fun remove() {
                            if (!canRemove) throw IllegalStateException()
                            this@ColumnView.remove(current as R)
                            canRemove = false
                        }
                    }
                }

                override fun add(element: MutableMap.MutableEntry<R, V>): Nothing =
                    throw UnsupportedOperationException()
                override fun clear() = this@ColumnView.clear()
            }
    }

    private inner class CellSetView : AbstractMutableSet<Table.Cell<R, C, V>>() {
        override val size: Int get() = sizeField

        override fun iterator(): MutableIterator<Table.Cell<R, C, V>> {
            val coordinates = coordinates().iterator()
            var current: Pair<R, C>? = null
            var canRemove = false
            return object : MutableIterator<Table.Cell<R, C, V>> {
                override fun hasNext(): Boolean = coordinates.hasNext()
                override fun next(): Table.Cell<R, C, V> {
                    val coordinate = coordinates.next()
                    current = coordinate
                    canRemove = true
                    return Tables.immutableCell(
                        coordinate.first,
                        coordinate.second,
                        this@StandardTable[coordinate.first, coordinate.second] as V,
                    )
                }
                override fun remove() {
                    if (!canRemove) throw IllegalStateException()
                    val coordinate = current!!
                    this@StandardTable.remove(coordinate.first, coordinate.second)
                    canRemove = false
                }
            }
        }

        override fun contains(element: Table.Cell<R, C, V>): Boolean =
            this@StandardTable.contains(element.getRowKey(), element.getColumnKey()) &&
                this@StandardTable[element.getRowKey(), element.getColumnKey()] == element.getValue()

        override fun remove(element: Table.Cell<R, C, V>): Boolean {
            if (!contains(element)) return false
            this@StandardTable.remove(element.getRowKey(), element.getColumnKey())
            return true
        }

        override fun add(element: Table.Cell<R, C, V>): Nothing = throw UnsupportedOperationException()
        override fun clear() = this@StandardTable.clear()
    }

    private inner class RowKeySetView : AbstractMutableSet<R>() {
        override val size: Int get() = backingMap.size
        override fun contains(element: R): Boolean = backingMap.containsKey(element)

        override fun iterator(): MutableIterator<R> {
            val rows = backingMap.keys.toList().iterator()
            var current: R? = null
            var canRemove = false
            return object : MutableIterator<R> {
                override fun hasNext(): Boolean = rows.hasNext()
                override fun next(): R = rows.next().also { current = it; canRemove = true }
                override fun remove() {
                    if (!canRemove) throw IllegalStateException()
                    removeRow(current)
                    canRemove = false
                }
            }
        }

        override fun remove(element: R): Boolean = removeRow(element) != null
        override fun add(element: R): Nothing = throw UnsupportedOperationException()
        override fun clear() = this@StandardTable.clear()
    }

    private inner class ColumnKeySetView : AbstractMutableSet<C>() {
        override val size: Int get() = currentColumnKeys().size
        override fun contains(element: C): Boolean = containsColumn(element)

        override fun iterator(): MutableIterator<C> {
            val columns = currentColumnKeys().iterator()
            var current: C? = null
            var canRemove = false
            return object : MutableIterator<C> {
                override fun hasNext(): Boolean = columns.hasNext()
                override fun next(): C = columns.next().also { current = it; canRemove = true }
                override fun remove() {
                    if (!canRemove) throw IllegalStateException()
                    removeColumn(current)
                    canRemove = false
                }
            }
        }

        override fun remove(element: C): Boolean = removeColumn(element) != null
        override fun add(element: C): Nothing = throw UnsupportedOperationException()
        override fun clear() = this@StandardTable.clear()
    }

    private inner class ValuesView : AbstractMutableCollection<V>() {
        override val size: Int get() = sizeField

        override fun iterator(): MutableIterator<V> {
            val cells = CellSetView().iterator()
            return object : MutableIterator<V> {
                override fun hasNext(): Boolean = cells.hasNext()
                override fun next(): V = cells.next().getValue()
                override fun remove() = cells.remove()
            }
        }

        override fun add(element: V): Nothing = throw UnsupportedOperationException()
        override fun clear() = this@StandardTable.clear()
    }

    private inner class RowMapView : AbstractMutableMap<R, Map<C, V>>() {
        override val size: Int get() = backingMap.size
        override fun containsKey(key: R): Boolean = backingMap.containsKey(key)
        override fun get(key: R): Map<C, V>? = if (containsKey(key)) RowView(key) else null
        override fun remove(key: R): Map<C, V>? = removeRow(key)
        override fun clear() = this@StandardTable.clear()
        override fun put(key: R, value: Map<C, V>): Map<C, V>? = throw UnsupportedOperationException()

        override val entries: MutableSet<MutableMap.MutableEntry<R, Map<C, V>>>
            get() = tableOuterEntries(
                keys = { this@StandardTable.rowKeySet() },
                value = { RowView(it) },
                remove = { removeRow(it) },
                clear = { this@StandardTable.clear() },
            )
    }

    private inner class ColumnMapView : AbstractMutableMap<C, Map<R, V>>() {
        override val size: Int get() = currentColumnKeys().size
        override fun containsKey(key: C): Boolean = containsColumn(key)
        override fun get(key: C): Map<R, V>? = if (containsKey(key)) ColumnView(key) else null
        override fun remove(key: C): Map<R, V>? = removeColumn(key)
        override fun clear() = this@StandardTable.clear()
        override fun put(key: C, value: Map<R, V>): Map<R, V>? = throw UnsupportedOperationException()

        override val entries: MutableSet<MutableMap.MutableEntry<C, Map<R, V>>>
            get() = tableOuterEntries(
                keys = { this@StandardTable.columnKeySet() },
                value = { ColumnView(it) },
                remove = { removeColumn(it) },
                clear = { this@StandardTable.clear() },
            )
    }

    private fun currentColumnKeys(): List<C> = buildList {
        val seen = LinkedHashSet<C>()
        for (row in backingMap.values) for (columnKey in row.keys) {
            if (seen.add(columnKey)) add(columnKey)
        }
    }
}

private fun <K, V> tableMapEntry(
    key: K,
    valueProvider: () -> V,
    valueSetter: (V) -> V,
): MutableMap.MutableEntry<K, V> = object : MutableMap.MutableEntry<K, V> {
    override val key: K = key
    override val value: V get() = valueProvider()
    override fun setValue(newValue: V): V = valueSetter(newValue)
    override fun equals(other: Any?): Boolean =
        other is Map.Entry<*, *> && key == other.key && value == other.value
    override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
    override fun toString(): String = "$key=$value"
}

private fun <K, V> tableOuterEntries(
    keys: () -> Set<K>,
    value: (K) -> V,
    remove: (K) -> Any?,
    clear: () -> Unit,
): MutableSet<MutableMap.MutableEntry<K, V>> =
    object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = keys().size
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val iterator = keys().iterator()
            var current: K? = null
            var canRemove = false
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): MutableMap.MutableEntry<K, V> {
                    val key = iterator.next()
                    current = key
                    canRemove = true
                    return tableMapEntry(key, { value(key) }) { throw UnsupportedOperationException() }
                }
                override fun remove() {
                    if (!canRemove) throw IllegalStateException()
                    @Suppress("UNCHECKED_CAST")
                    remove(current as K)
                    canRemove = false
                }
            }
        }
        override fun add(element: MutableMap.MutableEntry<K, V>): Nothing =
            throw UnsupportedOperationException()
        override fun clear() = clear.invoke()
    }
