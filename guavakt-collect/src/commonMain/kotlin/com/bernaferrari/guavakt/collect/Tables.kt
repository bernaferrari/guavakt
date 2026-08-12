package com.bernaferrari.guavakt.collect

/** Guava Table factories and live views. */
object Tables {
    fun <R, C, V> immutableCell(rowKey: R, columnKey: C, value: V): Table.Cell<R, C, V> =
        ImmutableCell(rowKey, columnKey, value)

    /** Live axis-swapping view. Transposing an existing transpose returns its original table. */
    fun <R, C, V> transpose(table: Table<R, C, V>): Table<C, R, V> {
        if (table is TransposeTable<*, *, *>) {
            @Suppress("UNCHECKED_CAST")
            return table.original as Table<C, R, V>
        }
        return TransposeTable(table)
    }

    /** Lazy live value transformation. Removal is supported; addition has no inverse. */
    fun <R, C, V1, V2> transformValues(
        fromTable: Table<R, C, V1>,
        function: (V1) -> V2,
    ): Table<R, C, V2> = TransformedTable(fromTable, function)

    fun <R, C, V> newCustomTable(
        backingMap: MutableMap<R, MutableMap<C, V>> = LinkedHashMap(),
        factory: () -> MutableMap<C, V> = { LinkedHashMap() },
    ): Table<R, C, V> = StandardTable(backingMap, factory)

    fun <R, C, V> unmodifiableTable(table: Table<out R, out C, out V>): Table<R, C, V> {
        @Suppress("UNCHECKED_CAST")
        return UnmodifiableTable(table as Table<R, C, V>)
    }
}

private data class ImmutableCell<R, C, V>(
    private val rowKey: R,
    private val columnKey: C,
    private val value: V,
) : Table.Cell<R, C, V> {
    override fun getRowKey(): R = rowKey
    override fun getColumnKey(): C = columnKey
    override fun getValue(): V = value

    override fun equals(other: Any?): Boolean =
        other is Table.Cell<*, *, *> &&
            rowKey == other.getRowKey() &&
            columnKey == other.getColumnKey() &&
            value == other.getValue()

    override fun hashCode(): Int =
        (rowKey?.hashCode() ?: 0) xor
            (columnKey?.hashCode() ?: 0) xor
            (value?.hashCode() ?: 0)

    override fun toString(): String = "($rowKey,$columnKey)=$value"
}

private class TransposeTable<R, C, V>(
    internal val original: Table<R, C, V>,
) : AbstractTable<C, R, V>() {
    override fun contains(rowKey: Any?, columnKey: Any?): Boolean =
        original.contains(columnKey, rowKey)
    override fun containsRow(rowKey: Any?): Boolean = original.containsColumn(rowKey)
    override fun containsColumn(columnKey: Any?): Boolean = original.containsRow(columnKey)
    override fun containsValue(value: Any?): Boolean = original.containsValue(value)
    override fun get(rowKey: Any?, columnKey: Any?): V? = original[columnKey, rowKey]
    override fun isEmpty(): Boolean = original.isEmpty()
    override fun size(): Int = original.size()
    override fun clear() = original.clear()
    override fun put(rowKey: C, columnKey: R, value: V): V? =
        original.put(columnKey, rowKey, value)
    override fun remove(rowKey: Any?, columnKey: Any?): V? =
        original.remove(columnKey, rowKey)
    override fun row(rowKey: C): Map<R, V> = original.column(rowKey)
    override fun column(columnKey: R): Map<C, V> = original.row(columnKey)

    override fun cellSet(): Set<Table.Cell<C, R, V>> =
        object : AbstractMutableSet<Table.Cell<C, R, V>>() {
            override val size: Int get() = original.size()
            override fun iterator(): MutableIterator<Table.Cell<C, R, V>> {
                val iterator = original.cellSet().iterator()
                return object : MutableIterator<Table.Cell<C, R, V>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): Table.Cell<C, R, V> {
                        val cell = iterator.next()
                        return Tables.immutableCell(
                            cell.getColumnKey(),
                            cell.getRowKey(),
                            cell.getValue(),
                        )
                    }
                    override fun remove() {
                        @Suppress("UNCHECKED_CAST")
                        (iterator as MutableIterator<Table.Cell<R, C, V>>).remove()
                    }
                }
            }
            override fun add(element: Table.Cell<C, R, V>): Nothing =
                throw UnsupportedOperationException()
            override fun clear() = original.clear()
        }

    override fun rowKeySet(): Set<C> = original.columnKeySet()
    override fun columnKeySet(): Set<R> = original.rowKeySet()
    override fun values(): Collection<V> = original.values()
    override fun rowMap(): Map<C, Map<R, V>> = original.columnMap()
    override fun columnMap(): Map<R, Map<C, V>> = original.rowMap()
}

private class TransformedTable<R, C, V1, V2>(
    private val fromTable: Table<R, C, V1>,
    private val function: (V1) -> V2,
) : AbstractTable<R, C, V2>() {
    override fun contains(rowKey: Any?, columnKey: Any?): Boolean =
        fromTable.contains(rowKey, columnKey)
    override fun containsRow(rowKey: Any?): Boolean = fromTable.containsRow(rowKey)
    override fun containsColumn(columnKey: Any?): Boolean = fromTable.containsColumn(columnKey)
    override fun containsValue(value: Any?): Boolean = values().any { it == value }

    override fun get(rowKey: Any?, columnKey: Any?): V2? {
        if (!fromTable.contains(rowKey, columnKey)) return null
        @Suppress("UNCHECKED_CAST")
        return function(fromTable[rowKey, columnKey] as V1)
    }

    override fun isEmpty(): Boolean = fromTable.isEmpty()
    override fun size(): Int = fromTable.size()
    override fun clear() = fromTable.clear()
    override fun put(rowKey: R, columnKey: C, value: V2): Nothing =
        throw UnsupportedOperationException()

    override fun remove(rowKey: Any?, columnKey: Any?): V2? {
        if (!fromTable.contains(rowKey, columnKey)) return null
        @Suppress("UNCHECKED_CAST")
        return function(fromTable.remove(rowKey, columnKey) as V1)
    }

    override fun row(rowKey: R): Map<C, V2> =
        TransformingTableMap(fromTable.row(rowKey)) { _, value -> function(value) }

    override fun column(columnKey: C): Map<R, V2> =
        TransformingTableMap(fromTable.column(columnKey)) { _, value -> function(value) }

    override fun cellSet(): Set<Table.Cell<R, C, V2>> =
        object : AbstractMutableSet<Table.Cell<R, C, V2>>() {
            override val size: Int get() = fromTable.size()
            override fun iterator(): MutableIterator<Table.Cell<R, C, V2>> {
                val iterator = fromTable.cellSet().iterator()
                return object : MutableIterator<Table.Cell<R, C, V2>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): Table.Cell<R, C, V2> {
                        val cell = iterator.next()
                        return Tables.immutableCell(
                            cell.getRowKey(),
                            cell.getColumnKey(),
                            function(cell.getValue()),
                        )
                    }
                    override fun remove() {
                        @Suppress("UNCHECKED_CAST")
                        (iterator as MutableIterator<Table.Cell<R, C, V1>>).remove()
                    }
                }
            }
            override fun add(element: Table.Cell<R, C, V2>): Nothing =
                throw UnsupportedOperationException()
            override fun clear() = fromTable.clear()
        }

    override fun rowKeySet(): Set<R> = fromTable.rowKeySet()
    override fun columnKeySet(): Set<C> = fromTable.columnKeySet()

    override fun values(): Collection<V2> = object : AbstractMutableCollection<V2>() {
        override val size: Int get() = fromTable.size()
        override fun iterator(): MutableIterator<V2> {
            val iterator = fromTable.values().iterator()
            return object : MutableIterator<V2> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): V2 = function(iterator.next())
                override fun remove() {
                    @Suppress("UNCHECKED_CAST")
                    (iterator as MutableIterator<V1>).remove()
                }
            }
        }
        override fun add(element: V2): Nothing = throw UnsupportedOperationException()
        override fun clear() = fromTable.clear()
    }

    override fun rowMap(): Map<R, Map<C, V2>> =
        TransformingTableMap(fromTable.rowMap()) { _, row ->
            TransformingTableMap(row) { _, value -> function(value) }
        }

    override fun columnMap(): Map<C, Map<R, V2>> =
        TransformingTableMap(fromTable.columnMap()) { _, column ->
            TransformingTableMap(column) { _, value -> function(value) }
        }
}

private class TransformingTableMap<K, V1, V2>(
    private val source: Map<K, V1>,
    private val transformer: (K, V1) -> V2,
) : AbstractMutableMap<K, V2>() {
    override val size: Int get() = source.size
    override fun containsKey(key: K): Boolean = source.containsKey(key)
    override fun get(key: K): V2? {
        if (!source.containsKey(key)) return null
        @Suppress("UNCHECKED_CAST")
        return transformer(key, source[key] as V1)
    }
    override fun put(key: K, value: V2): Nothing = throw UnsupportedOperationException()
    override fun remove(key: K): V2? {
        if (!source.containsKey(key)) return null
        @Suppress("UNCHECKED_CAST")
        val mutable = source as MutableMap<K, V1>
        @Suppress("UNCHECKED_CAST")
        return transformer(key, mutable.remove(key) as V1)
    }
    override fun clear() {
        @Suppress("UNCHECKED_CAST")
        (source as MutableMap<K, V1>).clear()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V2>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V2>>() {
            override val size: Int get() = source.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V2>> {
                val iterator = source.entries.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V2>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V2> {
                        val entry = iterator.next()
                        return transformedTableEntry(entry.key) {
                            transformer(entry.key, entry.value)
                        }
                    }
                    override fun remove() {
                        @Suppress("UNCHECKED_CAST")
                        (iterator as MutableIterator<Map.Entry<K, V1>>).remove()
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V2>): Nothing =
                throw UnsupportedOperationException()
            override fun clear() = this@TransformingTableMap.clear()
        }
}

private fun <K, V> transformedTableEntry(
    key: K,
    valueProvider: () -> V,
): MutableMap.MutableEntry<K, V> = object : MutableMap.MutableEntry<K, V> {
    override val key: K = key
    override val value: V get() = valueProvider()
    override fun setValue(newValue: V): Nothing = throw UnsupportedOperationException()
    override fun equals(other: Any?): Boolean =
        other is Map.Entry<*, *> && key == other.key && value == other.value
    override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
    override fun toString(): String = "$key=$value"
}

private class UnmodifiableTable<R, C, V>(
    private val delegate: Table<R, C, V>,
) : AbstractTable<R, C, V>() {
    override fun contains(rowKey: Any?, columnKey: Any?): Boolean = delegate.contains(rowKey, columnKey)
    override fun containsRow(rowKey: Any?): Boolean = delegate.containsRow(rowKey)
    override fun containsColumn(columnKey: Any?): Boolean = delegate.containsColumn(columnKey)
    override fun containsValue(value: Any?): Boolean = delegate.containsValue(value)
    override fun get(rowKey: Any?, columnKey: Any?): V? = delegate[rowKey, columnKey]
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun size(): Int = delegate.size()
    override fun put(rowKey: R, columnKey: C, value: V): Nothing = throw UnsupportedOperationException()
    override fun remove(rowKey: Any?, columnKey: Any?): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()
    override fun row(rowKey: R): Map<C, V> = UnmodifiableLiveMap(delegate.row(rowKey)) { _, value -> value }
    override fun column(columnKey: C): Map<R, V> =
        UnmodifiableLiveMap(delegate.column(columnKey)) { _, value -> value }
    override fun cellSet(): Set<Table.Cell<R, C, V>> = UnmodifiableLiveSet(delegate.cellSet())
    override fun rowKeySet(): Set<R> = UnmodifiableLiveSet(delegate.rowKeySet())
    override fun columnKeySet(): Set<C> = UnmodifiableLiveSet(delegate.columnKeySet())
    override fun values(): Collection<V> = UnmodifiableLiveCollection(delegate.values())
    override fun rowMap(): Map<R, Map<C, V>> =
        UnmodifiableLiveMap(delegate.rowMap()) { _, row ->
            UnmodifiableLiveMap(row) { _, value -> value }
        }
    override fun columnMap(): Map<C, Map<R, V>> =
        UnmodifiableLiveMap(delegate.columnMap()) { _, column ->
            UnmodifiableLiveMap(column) { _, value -> value }
        }
}

private class UnmodifiableLiveMap<K, V1, V2>(
    private val source: Map<K, V1>,
    private val transformer: (K, V1) -> V2,
) : AbstractMutableMap<K, V2>() {
    override val size: Int get() = source.size
    override fun containsKey(key: K): Boolean = source.containsKey(key)
    override fun get(key: K): V2? {
        if (!source.containsKey(key)) return null
        @Suppress("UNCHECKED_CAST")
        return transformer(key, source[key] as V1)
    }
    override fun put(key: K, value: V2): Nothing = throw UnsupportedOperationException()
    override fun putAll(from: Map<out K, V2>): Nothing = throw UnsupportedOperationException()
    override fun remove(key: K): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V2>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V2>>() {
            override val size: Int get() = source.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V2>> {
                val iterator = source.entries.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V2>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V2> {
                        val entry = iterator.next()
                        return transformedTableEntry(entry.key) {
                            transformer(entry.key, entry.value)
                        }
                    }
                    override fun remove(): Nothing = throw UnsupportedOperationException()
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V2>): Nothing =
                throw UnsupportedOperationException()
            override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V2>>): Nothing =
                throw UnsupportedOperationException()
            override fun remove(element: MutableMap.MutableEntry<K, V2>): Nothing =
                throw UnsupportedOperationException()
            override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V2>>): Nothing =
                throw UnsupportedOperationException()
            override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V2>>): Nothing =
                throw UnsupportedOperationException()
            override fun clear(): Nothing = throw UnsupportedOperationException()
        }
}

private class UnmodifiableLiveSet<E>(
    private val source: Set<E>,
) : AbstractMutableSet<E>() {
    override val size: Int get() = source.size
    override fun contains(element: E): Boolean = source.contains(element)
    override fun iterator(): MutableIterator<E> {
        val iterator = source.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next()
            override fun remove(): Nothing = throw UnsupportedOperationException()
        }
    }
    override fun add(element: E): Nothing = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Nothing = throw UnsupportedOperationException()
    override fun remove(element: E): Nothing = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<E>): Nothing = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<E>): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()
}

private class UnmodifiableLiveCollection<E>(
    private val source: Collection<E>,
) : AbstractMutableCollection<E>() {
    override val size: Int get() = source.size
    override fun contains(element: E): Boolean = source.contains(element)
    override fun iterator(): MutableIterator<E> {
        val iterator = source.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next()
            override fun remove(): Nothing = throw UnsupportedOperationException()
        }
    }
    override fun add(element: E): Nothing = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Nothing = throw UnsupportedOperationException()
    override fun remove(element: E): Nothing = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<E>): Nothing = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<E>): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()
}
