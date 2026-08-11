package dev.guavakt.parity

import com.google.common.collect.TreeBasedTable as GuavaTreeBasedTable
import dev.guavakt.collect.TreeBasedTable as GuavaKtTreeBasedTable
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeBasedTableDifferentialTest {
    @Test
    fun naturalAndReverseOrderingMutationTracesMatchGuava() {
        val guavaNatural = GuavaTreeBasedTable.create<Int, Int, String>()
        val guavaKtNatural = GuavaKtTreeBasedTable.create<Int, Int, String>()
        populate(guavaNatural)
        populate(guavaKtNatural)
        assertEquals(trace(guavaNatural), trace(guavaKtNatural))

        val reverse = Comparator<Int> { left, right -> right.compareTo(left) }
        val guavaReverse = GuavaTreeBasedTable.create<Int, Int, String>(reverse, reverse)
        val guavaKtReverse = GuavaKtTreeBasedTable.create<Int, Int, String>(reverse, reverse)
        populate(guavaReverse)
        populate(guavaKtReverse)
        assertEquals(trace(guavaReverse), trace(guavaKtReverse))
    }

    private fun populate(table: com.google.common.collect.Table<Int, Int, String>) {
        table.put(2, 5, "a")
        table.put(1, 9, "b")
        table.put(3, 1, "c")
        table.put(1, 3, "d")
    }

    private fun populate(table: dev.guavakt.collect.Table<Int, Int, String>) {
        table.put(2, 5, "a")
        table.put(1, 9, "b")
        table.put(3, 1, "c")
        table.put(1, 3, "d")
    }

    private fun trace(table: GuavaTreeBasedTable<Int, Int, String>): List<Any?> {
        val trace = mutableListOf<Any?>()
        trace.add(table.rowKeySet().toList())
        trace.add(table.columnKeySet().toList())
        trace.add(table.row(1).keys.toList())
        trace.add(table.rowMap().keys.toList())
        trace.add(table.columnMap().keys.toList())
        trace.add(table.rowComparator().compare(3, 1))
        trace.add(table.columnComparator().compare(3, 1))
        trace.add(table.columnKeySet().remove(5))
        trace.add(table.cellSet().map { Triple(it.rowKey, it.columnKey, it.value) })
        return trace
    }

    private fun trace(table: GuavaKtTreeBasedTable<Int, Int, String>): List<Any?> {
        val trace = mutableListOf<Any?>()
        trace.add(table.rowKeySet().toList())
        trace.add(table.columnKeySet().toList())
        trace.add(table.row(1).keys.toList())
        trace.add(table.rowMap().keys.toList())
        trace.add(table.columnMap().keys.toList())
        trace.add(table.rowComparator().compare(3, 1))
        trace.add(table.columnComparator().compare(3, 1))
        trace.add((table.columnKeySet() as MutableSet<Int>).remove(5))
        trace.add(table.cellSet().map { Triple(it.getRowKey(), it.getColumnKey(), it.getValue()) })
        return trace
    }
}
