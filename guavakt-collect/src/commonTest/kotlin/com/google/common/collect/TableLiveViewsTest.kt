package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TableLiveViewsTest {
    @Test
    fun allStandardTableViewsAreLiveAndRemovalCapable() {
        val table = HashBasedTable.create<String, String, Int>()
        table.put("r1", "c1", 1)
        table.put("r1", "c2", 2)
        table.put("r2", "c1", 3)

        val row = table.row("r1") as MutableMap<String, Int>
        val column = table.column("c1") as MutableMap<String, Int>
        val cells = table.cellSet() as MutableSet<Table.Cell<String, String, Int>>
        val rowKeys = table.rowKeySet() as MutableSet<String>
        val columnKeys = table.columnKeySet() as MutableSet<String>
        val values = table.values() as MutableCollection<Int>
        val rowMap = table.rowMap() as MutableMap<String, Map<String, Int>>
        val columnMap = table.columnMap() as MutableMap<String, Map<String, Int>>

        table.put("r1", "c3", 4)
        assertEquals(setOf("c1", "c2", "c3"), row.keys)
        assertEquals(4, cells.size)
        assertTrue("c3" in columnKeys)
        assertEquals(4, rowMap["r1"]?.get("c3"))
        assertEquals(4, columnMap["c3"]?.get("r1"))

        row["c4"] = 5
        column["r3"] = 6
        assertEquals(6, table.size())
        assertEquals(5, table["r1", "c4"])
        assertEquals(6, table["r3", "c1"])

        assertEquals(3, column.remove("r2"))
        assertTrue(values.remove(2))
        assertTrue(rowKeys.remove("r3"))
        assertTrue(columnKeys.remove("c4"))
        assertEquals(mapOf("r1" to mapOf("c1" to 1, "c3" to 4)), table.rowMap())

        val iterator = cells.iterator()
        iterator.next()
        iterator.remove()
        assertEquals(1, table.size())
    }

    @Test
    fun rowAndColumnEntrySetValueUpdatesTheTableAndCellEqualityIsValueBased() {
        val table = HashBasedTable.create<String, String, Int>()
        table.put("r", "c", 1)

        val rowEntry = table.row("r").entries.first() as MutableMap.MutableEntry<String, Int>
        assertEquals(1, rowEntry.setValue(2))
        assertEquals(2, table["r", "c"])
        val columnEntry = table.column("c").entries.first() as MutableMap.MutableEntry<String, Int>
        assertEquals(2, columnEntry.setValue(3))
        assertEquals(3, table["r", "c"])

        assertEquals(setOf(Tables.immutableCell("r", "c", 3)), table.cellSet())
        assertTrue((table.cellSet() as MutableSet).remove(Tables.immutableCell("r", "c", 3)))
        assertTrue(table.isEmpty())
    }

    @Test
    fun transposeIsAReversibleLiveView() {
        val table = HashBasedTable.create<String, String, Int>()
        table.put("r1", "c1", 1)
        val transpose = Tables.transpose(table)

        table.put("r2", "c1", 2)
        assertEquals(mapOf("r1" to 1, "r2" to 2), transpose.row("c1"))
        transpose.put("c2", "r1", 3)
        assertEquals(3, table["r1", "c2"])
        (transpose.row("c1") as MutableMap<String, Int>).remove("r1")
        assertFalse(table.contains("r1", "c1"))
        assertSame(table, Tables.transpose(transpose))
    }

    @Test
    fun transformedTableIsLazyLiveAndSupportsRemovalButNotAddition() {
        val table = HashBasedTable.create<String, String, Int>()
        table.put("r", "a", 1)
        table.put("r", "b", 2)
        var calls = 0
        val transformed = Tables.transformValues(table) {
            calls++
            it * 10
        }

        assertEquals(0, calls)
        assertEquals(2, transformed.size())
        assertEquals(0, calls)
        assertEquals(10, transformed["r", "a"])
        assertEquals(1, calls)
        table.put("r", "c", 3)
        assertEquals(mapOf("a" to 10, "b" to 20, "c" to 30), transformed.row("r"))

        assertEquals(20, (transformed.row("r") as MutableMap<String, Int>).remove("b"))
        assertFalse(table.contains("r", "b"))
        val iterator = transformed.values().iterator() as MutableIterator<Int>
        iterator.next()
        iterator.remove()
        assertEquals(1, table.size())
        assertFailsWith<UnsupportedOperationException> { transformed.put("r", "z", 90) }
    }

    @Test
    fun customTableUsesTheSuppliedBackingMapAndFactory() {
        val backing = LinkedHashMap<String, MutableMap<String, Int>>()
        var factoryCalls = 0
        val table = Tables.newCustomTable(backing) {
            factoryCalls++
            LinkedHashMap()
        }

        table.put("r", "c", 1)
        assertEquals(1, factoryCalls)
        assertEquals(1, backing["r"]?.get("c"))
        (table.row("r") as MutableMap<String, Int>)["d"] = 2
        assertEquals(2, backing["r"]?.get("d"))
    }

    @Test
    fun hashBasedTableCapacityHintsValidateAndDoNotLimitGrowth() {
        val table = HashBasedTable.create<String, String, Int>(1, 1)
        repeat(10) { index -> table.put("r$index", "c$index", index) }
        assertEquals(10, table.size())
        assertFailsWith<IllegalArgumentException> {
            HashBasedTable.create<String, String, Int>(-1, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            HashBasedTable.create<String, String, Int>(1, -1)
        }
    }

    @Test
    fun unmodifiableTableViewsStayLiveAndRejectDeepMutation() {
        val source = HashBasedTable.create<String, String, Int>()
        source.put("r", "a", 1)
        val table = Tables.unmodifiableTable(source)
        val row = table.row("r") as MutableMap<String, Int>
        val values = table.values() as MutableCollection<Int>
        val cells = table.cellSet() as MutableSet<Table.Cell<String, String, Int>>
        val rowMap = table.rowMap() as MutableMap<String, Map<String, Int>>

        source.put("r", "b", 2)
        source.put("s", "a", 3)
        assertEquals(mapOf("a" to 1, "b" to 2), row)
        assertEquals(listOf(1, 2, 3), values.toList())
        assertEquals(3, cells.size)
        assertEquals(3, rowMap["s"]?.get("a"))

        assertFailsWith<UnsupportedOperationException> { table.put("r", "c", 4) }
        assertFailsWith<UnsupportedOperationException> { row["c"] = 4 }
        assertFailsWith<UnsupportedOperationException> { row.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { row.putAll(emptyMap()) }
        assertFailsWith<UnsupportedOperationException> {
            (rowMap["r"] as MutableMap<String, Int>).remove("a")
        }
        assertFailsWith<UnsupportedOperationException> { values.remove(999) }
        assertFailsWith<UnsupportedOperationException> { cells.clear() }
        assertFailsWith<UnsupportedOperationException> {
            val iterator = table.rowKeySet().iterator() as MutableIterator<String>
            iterator.next()
            iterator.remove()
        }
    }
}
