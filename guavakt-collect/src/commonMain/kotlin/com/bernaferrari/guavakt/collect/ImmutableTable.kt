package com.bernaferrari.guavakt.collect

/**
 * Guava ImmutableTable — immutable Table with builder.
 */
abstract class ImmutableTable<R, C, V> : AbstractTable<R, C, V>() {
    override fun put(rowKey: R, columnKey: C, value: V): V? =
        throw UnsupportedOperationException()
    override fun remove(rowKey: Any?, columnKey: Any?): V? =
        throw UnsupportedOperationException()
    override fun clear() = throw UnsupportedOperationException()

    companion object {
        private val EMPTY: ImmutableTable<Any?, Any?, Any?> = EmptyImmutableTable

        @Suppress("UNCHECKED_CAST")
        fun <R, C, V> of(): ImmutableTable<R, C, V> = EMPTY as ImmutableTable<R, C, V>

        fun <R, C, V> of(rowKey: R, columnKey: C, value: V): ImmutableTable<R, C, V> {
            rejectNullCellPart(rowKey, "rowKey")
            rejectNullCellPart(columnKey, "columnKey")
            rejectNullCellPart(value, "value")
            return SingletonImmutableTable(rowKey, columnKey, value)
        }

        fun <R, C, V> copyOf(table: Table<out R, out C, out V>): ImmutableTable<R, C, V> {
            if (table is ImmutableTable) {
                @Suppress("UNCHECKED_CAST")
                return table as ImmutableTable<R, C, V>
            }
            val builder = builder<R, C, V>()
            for (cell in table.cellSet()) builder.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue())
            return builder.build()
        }

        fun <R, C, V> builder(): Builder<R, C, V> = Builder()

        private fun rejectNullCellPart(value: Any?, label: String) {
            if (value == null) throw NullPointerException(label)
        }
    }

    class Builder<R, C, V> {
        private val cells = ArrayList<Table.Cell<R, C, V>>()
        private var rowComparator: Comparator<in R>? = null
        private var columnComparator: Comparator<in C>? = null

        fun put(rowKey: R, columnKey: C, value: V): Builder<R, C, V> {
            rejectNullCellPart(rowKey, "rowKey")
            rejectNullCellPart(columnKey, "columnKey")
            rejectNullCellPart(value, "value")
            cells.add(Tables.immutableCell(rowKey, columnKey, value))
            return this
        }
        fun put(cell: Table.Cell<out R, out C, out V>): Builder<R, C, V> {
            cells.add(Tables.immutableCell(cell.getRowKey(), cell.getColumnKey(), cell.getValue()))
            return this
        }
        fun putAll(table: Table<out R, out C, out V>): Builder<R, C, V> {
            for (c in table.cellSet()) put(c)
            return this
        }

        fun orderRowsBy(comparator: Comparator<in R>): Builder<R, C, V> = apply {
            rowComparator = comparator
        }

        fun orderColumnsBy(comparator: Comparator<in C>): Builder<R, C, V> = apply {
            columnComparator = comparator
        }

        fun build(): ImmutableTable<R, C, V> {
            val ordered = validatedAndOrderedCells()
            return when (ordered.size) {
            0 -> of()
            1 -> SingletonImmutableTable(
                ordered[0].getRowKey(),
                ordered[0].getColumnKey(),
                ordered[0].getValue(),
            )
            else -> RegularImmutableTable(ordered)
            }
        }

        fun buildOrThrow(): ImmutableTable<R, C, V> = build()

        private fun validatedAndOrderedCells(): List<Table.Cell<R, C, V>> {
            val coordinates = HashSet<Pair<R, C>>()
            val rowEncounterOrder = LinkedHashMap<R, Int>()
            for (cell in cells) {
                val coordinate = cell.getRowKey() to cell.getColumnKey()
                require(coordinates.add(coordinate)) {
                    "Duplicate cell (${cell.getRowKey()}, ${cell.getColumnKey()})"
                }
                if (cell.getRowKey() !in rowEncounterOrder) {
                    rowEncounterOrder[cell.getRowKey()] = rowEncounterOrder.size
                }
            }
            if (rowComparator == null && columnComparator == null) return cells.toList()
            return cells.sortedWith { left, right ->
                val rowOrder = rowComparator?.compare(left.getRowKey(), right.getRowKey())
                    ?: rowEncounterOrder.getValue(left.getRowKey())
                        .compareTo(rowEncounterOrder.getValue(right.getRowKey()))
                if (rowOrder != 0) rowOrder
                else columnComparator?.compare(left.getColumnKey(), right.getColumnKey()) ?: 0
            }
        }
    }
}

private object EmptyImmutableTable : ImmutableTable<Any?, Any?, Any?>() {
    override fun get(rowKey: Any?, columnKey: Any?): Any? = null
    override fun size(): Int = 0
    override fun cellSet(): Set<Table.Cell<Any?, Any?, Any?>> = unmodifiableMutableSet(emptySet())
    override fun rowKeySet(): Set<Any?> = unmodifiableMutableSet(emptySet())
    override fun columnKeySet(): Set<Any?> = unmodifiableMutableSet(emptySet())
    override fun values(): Collection<Any?> = unmodifiableMutableCollection(emptyList())
    override fun row(rowKey: Any?): Map<Any?, Any?> = unmodifiableMutableMap(emptyMap())
    override fun column(columnKey: Any?): Map<Any?, Any?> = unmodifiableMutableMap(emptyMap())
    override fun rowMap(): Map<Any?, Map<Any?, Any?>> = unmodifiableMutableMap(emptyMap())
    override fun columnMap(): Map<Any?, Map<Any?, Any?>> = unmodifiableMutableMap(emptyMap())
}
