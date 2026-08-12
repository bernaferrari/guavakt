package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreeBasedTableOrderingTest {
    @Test
    fun rowsColumnsAndNestedMapsStayGloballySortedAndLive() {
        val table: RowSortedTable<Int, Int, String> = TreeBasedTable.create()
        table.put(2, 5, "a")
        table.put(1, 9, "b")
        table.put(3, 1, "c")
        table.put(1, 3, "d")

        assertEquals(listOf(1, 2, 3), table.rowKeySet().toList())
        assertEquals(listOf(1, 3, 5, 9), table.columnKeySet().toList())
        assertEquals(listOf(3, 9), table.row(1).keys.toList())
        assertEquals(listOf(1, 2, 3), table.rowMap().keys.toList())
        assertEquals(listOf(1, 3, 5, 9), table.columnMap().keys.toList())
        assertEquals(table.rowKeySet(), table.rowKeySetSorted())
        assertEquals(table.rowMap(), table.rowMapSorted())

        assertTrue((table.columnKeySet() as MutableSet<Int>).remove(5))
        assertEquals(listOf(1, 3, 9), table.columnKeySet().toList())
        assertEquals(3, table.size())
    }

    @Test
    fun explicitReverseComparatorsControlBothAxes() {
        val reverse = Comparator<Int> { left, right -> right.compareTo(left) }
        val table = TreeBasedTable.create<Int, Int, String>(reverse, reverse)
        table.put(1, 2, "a")
        table.put(3, 1, "b")
        table.put(2, 3, "c")

        assertEquals(listOf(3, 2, 1), table.rowKeySet().toList())
        assertEquals(listOf(3, 2, 1), table.columnKeySet().toList())
        assertTrue(table.rowComparator().compare(3, 1) < 0)
        assertTrue(table.columnComparator().compare(3, 1) < 0)
    }
}
