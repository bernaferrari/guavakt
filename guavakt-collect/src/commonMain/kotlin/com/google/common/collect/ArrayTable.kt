package dev.guavakt.collect

/**
 * Fixed-row, fixed-column table backed by a dense two-dimensional array.
 *
 * Every row/column pair is a cell, including cells whose value is `null`, so [size] is always
 * `rowKeySet().size * columnKeySet().size`. Kotlin callers that use empty cells should choose a
 * nullable value type, for example `ArrayTable<String, String, Int?>`.
 *
 * Row, column, cell, values, row-map, and column-map views are live. Existing cells can be updated
 * through row and column maps, but the fixed key space cannot be structurally changed. Use [erase]
 * or [eraseAll] to restore cells to `null`.
 */
class ArrayTable<R, C, V> private constructor(
    private val rowList: List<R>,
    private val columnList: List<C>,
    private val array: Array<Array<Any?>>,
) : AbstractTable<R, C, V>() {
    private val rowKeyToIndex = rowList.withIndex().associate { it.value to it.index }
    private val columnKeyToIndex = columnList.withIndex().associate { it.value to it.index }

    override fun contains(rowKey: Any?, columnKey: Any?): Boolean =
        rowKeyToIndex.containsKey(rowKey) && columnKeyToIndex.containsKey(columnKey)

    override fun containsRow(rowKey: Any?): Boolean = rowKeyToIndex.containsKey(rowKey)
    override fun containsColumn(columnKey: Any?): Boolean = columnKeyToIndex.containsKey(columnKey)

    override fun get(rowKey: Any?, columnKey: Any?): V? {
        val rowIndex = rowKeyToIndex[rowKey] ?: return null
        val columnIndex = columnKeyToIndex[columnKey] ?: return null
        return valueAt(rowIndex, columnIndex)
    }

    override fun size(): Int = rowList.size * columnList.size

    override fun put(rowKey: R, columnKey: C, value: V): V? {
        val rowIndex = rowKeyToIndex[rowKey]
            ?: throw IllegalArgumentException("Row $rowKey not in $rowList")
        val columnIndex = columnKeyToIndex[columnKey]
            ?: throw IllegalArgumentException("Column $columnKey not in $columnList")
        return set(rowIndex, columnIndex, value)
    }

    override fun remove(rowKey: Any?, columnKey: Any?): Nothing =
        throw UnsupportedOperationException("ArrayTable has a fixed cell set; use erase")

    override fun clear(): Nothing =
        throw UnsupportedOperationException("ArrayTable has a fixed cell set; use eraseAll")

    override fun cellSet(): Set<Table.Cell<R, C, V>> =
        object : AbstractMutableSet<Table.Cell<R, C, V>>() {
            override val size: Int get() = this@ArrayTable.size()
            override fun iterator(): MutableIterator<Table.Cell<R, C, V>> =
                fixedGridIterator { rowIndex, columnIndex -> ArrayCell(rowIndex, columnIndex) }
            override fun add(element: Table.Cell<R, C, V>): Nothing =
                throw UnsupportedOperationException()
            override fun remove(element: Table.Cell<R, C, V>): Nothing =
                throw UnsupportedOperationException()
            override fun clear(): Nothing = throw UnsupportedOperationException()
        }

    override fun rowKeySet(): Set<R> = FixedArrayTableSet(rowList)
    override fun columnKeySet(): Set<C> = FixedArrayTableSet(columnList)

    override fun values(): Collection<V> = object : AbstractMutableCollection<V>() {
        override val size: Int get() = this@ArrayTable.size()
        override fun iterator(): MutableIterator<V> =
            fixedGridIterator { rowIndex, columnIndex -> valueAt(rowIndex, columnIndex) as V }
        override fun add(element: V): Nothing = throw UnsupportedOperationException()
        override fun remove(element: V): Nothing = throw UnsupportedOperationException()
        override fun clear(): Nothing = throw UnsupportedOperationException()
    }

    override fun row(rowKey: R): Map<C, V> {
        val rowIndex = rowKeyToIndex[rowKey] ?: return emptyMap()
        return ArrayTableAxisMap(
            columnList,
            columnKeyToIndex,
            valueProvider = { columnIndex -> valueAt(rowIndex, columnIndex) as V },
            valueSetter = { columnIndex, value -> set(rowIndex, columnIndex, value) as V },
            invalidKeyMessage = { "Column $it not in $columnList" },
        )
    }

    override fun column(columnKey: C): Map<R, V> {
        val columnIndex = columnKeyToIndex[columnKey] ?: return emptyMap()
        return ArrayTableAxisMap(
            rowList,
            rowKeyToIndex,
            valueProvider = { rowIndex -> valueAt(rowIndex, columnIndex) as V },
            valueSetter = { rowIndex, value -> set(rowIndex, columnIndex, value) as V },
            invalidKeyMessage = { "Row $it not in $rowList" },
        )
    }

    override fun rowMap(): Map<R, Map<C, V>> =
        FixedArrayTableMap(rowList, rowKeyToIndex) { rowIndex -> row(rowList[rowIndex]) }

    override fun columnMap(): Map<C, Map<R, V>> =
        FixedArrayTableMap(columnList, columnKeyToIndex) { columnIndex ->
            column(columnList[columnIndex])
        }

    fun erase(rowKey: R, columnKey: C): V? {
        val rowIndex = rowKeyToIndex[rowKey] ?: return null
        val columnIndex = columnKeyToIndex[columnKey] ?: return null
        return set(rowIndex, columnIndex, null)
    }

    fun eraseAll() {
        for (rowIndex in array.indices) {
            for (columnIndex in array[rowIndex].indices) array[rowIndex][columnIndex] = null
        }
    }

    fun at(rowIndex: Int, columnIndex: Int): V? = valueAt(rowIndex, columnIndex)

    fun set(rowIndex: Int, columnIndex: Int, value: V?): V? {
        val previous = valueAt(rowIndex, columnIndex)
        array[rowIndex][columnIndex] = value
        return previous
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueAt(rowIndex: Int, columnIndex: Int): V? =
        array[rowIndex][columnIndex] as V?

    private fun <T> fixedGridIterator(
        transform: (rowIndex: Int, columnIndex: Int) -> T,
    ): MutableIterator<T> = object : MutableIterator<T> {
        private var index = 0
        override fun hasNext(): Boolean = index < size()
        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            val current = index++
            return transform(current / columnList.size, current % columnList.size)
        }
        override fun remove(): Nothing = throw UnsupportedOperationException()
    }

    private inner class ArrayCell(
        private val rowIndex: Int,
        private val columnIndex: Int,
    ) : Table.Cell<R, C, V> {
        override fun getRowKey(): R = rowList[rowIndex]
        override fun getColumnKey(): C = columnList[columnIndex]
        @Suppress("UNCHECKED_CAST")
        override fun getValue(): V = valueAt(rowIndex, columnIndex) as V
        override fun equals(other: Any?): Boolean =
            other is Table.Cell<*, *, *> &&
                getRowKey() == other.getRowKey() &&
                getColumnKey() == other.getColumnKey() &&
                getValue() == other.getValue()
        override fun hashCode(): Int =
            (getRowKey()?.hashCode() ?: 0) xor
                (getColumnKey()?.hashCode() ?: 0) xor
                (getValue()?.hashCode() ?: 0)
        override fun toString(): String = "(${getRowKey()},${getColumnKey()})=${getValue()}"
    }

    companion object {
        fun <R, C, V> create(rowKeys: Iterable<R>, columnKeys: Iterable<C>): ArrayTable<R, C, V> {
            val rows = rowKeys.toList()
            val columns = columnKeys.toList()
            require(rows.isNotEmpty()) { "rowKeys must not be empty" }
            require(columns.isNotEmpty()) { "columnKeys must not be empty" }
            rejectNullOrDuplicateKeys(rows, "row")
            rejectNullOrDuplicateKeys(columns, "column")
            return ArrayTable(rows, columns, Array(rows.size) { arrayOfNulls(columns.size) })
        }

        fun <R, C, V> create(table: Table<R, C, V>): ArrayTable<R, C, V> {
            if (table is ArrayTable) {
                val copy = create<R, C, V>(table.rowList, table.columnList)
                for (rowIndex in table.rowList.indices) {
                    for (columnIndex in table.columnList.indices) {
                        copy.array[rowIndex][columnIndex] = table.array[rowIndex][columnIndex]
                    }
                }
                return copy
            }
            val result = create<R, C, V>(table.rowKeySet(), table.columnKeySet())
            for (cell in table.cellSet()) {
                result.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue())
            }
            return result
        }

        private fun <K> rejectNullOrDuplicateKeys(keys: List<K>, axis: String) {
            val seen = HashSet<K>()
            for (key in keys) {
                if (key == null) throw NullPointerException("$axis key")
                require(seen.add(key)) { "Duplicate $axis key: $key" }
            }
        }
    }
}

private class ArrayTableAxisMap<K, V>(
    private val orderedKeys: List<K>,
    private val keyToIndex: Map<K, Int>,
    private val valueProvider: (Int) -> V,
    private val valueSetter: (Int, V) -> V,
    private val invalidKeyMessage: (K) -> String,
) : AbstractMutableMap<K, V>() {
    override val size: Int get() = orderedKeys.size
    override fun containsKey(key: K): Boolean = keyToIndex.containsKey(key)
    override fun get(key: K): V? = keyToIndex[key]?.let(valueProvider)
    override fun put(key: K, value: V): V {
        val index = keyToIndex[key] ?: throw IllegalArgumentException(invalidKeyMessage(key))
        return valueSetter(index, value)
    }
    override fun remove(key: K): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = orderedKeys.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
                object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var index = 0
                    override fun hasNext(): Boolean = index < orderedKeys.size
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        if (!hasNext()) throw NoSuchElementException()
                        val entryIndex = index++
                        return arrayTableEntry(
                            orderedKeys[entryIndex],
                            { valueProvider(entryIndex) },
                            { value -> valueSetter(entryIndex, value) },
                        )
                    }
                    override fun remove(): Nothing = throw UnsupportedOperationException()
                }
            override fun add(element: MutableMap.MutableEntry<K, V>): Nothing =
                throw UnsupportedOperationException()
            override fun remove(element: MutableMap.MutableEntry<K, V>): Nothing =
                throw UnsupportedOperationException()
            override fun clear(): Nothing = throw UnsupportedOperationException()
        }
}

private class FixedArrayTableMap<K, V>(
    private val orderedKeys: List<K>,
    private val keyToIndex: Map<K, Int>,
    private val valueProvider: (Int) -> V,
) : AbstractMutableMap<K, V>() {
    override val size: Int get() = orderedKeys.size
    override fun containsKey(key: K): Boolean = keyToIndex.containsKey(key)
    override fun get(key: K): V? = keyToIndex[key]?.let(valueProvider)
    override fun put(key: K, value: V): Nothing = throw UnsupportedOperationException()
    override fun putAll(from: Map<out K, V>): Nothing = throw UnsupportedOperationException()
    override fun remove(key: K): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = orderedKeys.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
                object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var index = 0
                    override fun hasNext(): Boolean = index < orderedKeys.size
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        if (!hasNext()) throw NoSuchElementException()
                        val entryIndex = index++
                        return arrayTableEntry(orderedKeys[entryIndex], { valueProvider(entryIndex) })
                    }
                    override fun remove(): Nothing = throw UnsupportedOperationException()
                }
            override fun add(element: MutableMap.MutableEntry<K, V>): Nothing =
                throw UnsupportedOperationException()
            override fun remove(element: MutableMap.MutableEntry<K, V>): Nothing =
                throw UnsupportedOperationException()
            override fun clear(): Nothing = throw UnsupportedOperationException()
        }
}

private class FixedArrayTableSet<E>(
    private val elements: List<E>,
) : AbstractMutableSet<E>() {
    override val size: Int get() = elements.size
    override fun contains(element: E): Boolean = elements.contains(element)
    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private val iterator = elements.iterator()
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): E = iterator.next()
        override fun remove(): Nothing = throw UnsupportedOperationException()
    }
    override fun add(element: E): Nothing = throw UnsupportedOperationException()
    override fun remove(element: E): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()
}

private fun <K, V> arrayTableEntry(
    key: K,
    valueProvider: () -> V,
    valueSetter: ((V) -> V)? = null,
): MutableMap.MutableEntry<K, V> = object : MutableMap.MutableEntry<K, V> {
    override val key: K = key
    override val value: V get() = valueProvider()
    override fun setValue(newValue: V): V {
        val setter = valueSetter ?: throw UnsupportedOperationException()
        return setter(newValue)
    }
    override fun equals(other: Any?): Boolean =
        other is Map.Entry<*, *> && key == other.key && value == other.value
    override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
    override fun toString(): String = "$key=$value"
}
