package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrayTableLiveViewTest {
    @Test
    fun emptySlotsAreRealCellsAndAllViewsUseGridOrder() {
        val table = ArrayTable.create<String, String, Int?>(
            listOf("r1", "r2"),
            listOf("c1", "c2"),
        )

        assertEquals(4, table.size())
        assertTrue(table.contains("r1", "c1"))
        assertTrue(table.containsValue(null))
        assertEquals(listOf(null, null, null, null), table.values().toList())
        assertEquals(
            listOf(
                Triple("r1", "c1", null),
                Triple("r1", "c2", null),
                Triple("r2", "c1", null),
                Triple("r2", "c2", null),
            ),
            table.cellSet().map { Triple(it.getRowKey(), it.getColumnKey(), it.getValue()) },
        )
        assertEquals(mapOf("c1" to null, "c2" to null), table.row("r1"))
        assertEquals(mapOf("r1" to null, "r2" to null), table.column("c1"))
    }

    @Test
    fun rowColumnCellAndNestedMapViewsAreLive() {
        val table = ArrayTable.create<String, String, Int?>(
            listOf("r1", "r2"),
            listOf("c1", "c2"),
        )
        val row = table.row("r1") as MutableMap<String, Int?>
        val column = table.column("c1") as MutableMap<String, Int?>
        val firstCell = table.cellSet().first()
        val rowMap = table.rowMap()

        assertNull(row.entries.first().setValue(1))
        assertEquals(1, firstCell.getValue())
        assertNull(row.put("c2", 2))
        assertNull(column.put("r2", 3))
        assertEquals(mapOf("c1" to 1, "c2" to 2), rowMap["r1"])
        assertEquals(mapOf("c1" to 3, "c2" to null), rowMap["r2"])
        assertEquals(3, table.erase("r2", "c1"))
        assertNull(column["r2"])
    }

    @Test
    fun fixedGridRejectsStructuralMutationAndBadKeys() {
        val table = ArrayTable.create<String, String, Int?>(listOf("r"), listOf("c"))
        val row = table.row("r") as MutableMap<String, Int?>
        assertFailsWith<UnsupportedOperationException> { table.remove("r", "c") }
        assertFailsWith<UnsupportedOperationException> { table.clear() }
        assertFailsWith<UnsupportedOperationException> { row.remove("c") }
        assertFailsWith<IllegalArgumentException> { row["missing"] = 1 }
        assertFailsWith<UnsupportedOperationException> {
            val iterator = table.cellSet().iterator() as MutableIterator<*>
            iterator.next()
            iterator.remove()
        }
        assertFailsWith<UnsupportedOperationException> {
            (table.rowKeySet() as MutableSet<String>).remove("r")
        }
    }

    @Test
    fun constructionRejectsEmptyNullAndDuplicateAxes() {
        assertFailsWith<IllegalArgumentException> {
            ArrayTable.create<String, String, Int?>(emptyList(), listOf("c"))
        }
        assertFailsWith<IllegalArgumentException> {
            ArrayTable.create<String, String, Int?>(listOf("r", "r"), listOf("c"))
        }
        assertFailsWith<NullPointerException> {
            ArrayTable.create<String?, String, Int?>(listOf(null), listOf("c"))
        }
    }
}
