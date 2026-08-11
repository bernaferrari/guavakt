package dev.guavakt.parity

import com.google.common.collect.ArrayTable as GuavaArrayTable
import dev.guavakt.collect.ArrayTable as GuavaKtArrayTable
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayTableDifferentialTest {
    @Test
    fun fixedGridLiveViewTraceMatchesGuava() {
        val guava = GuavaArrayTable.create<String, String, Int>(
            listOf("r1", "r2"),
            listOf("c1", "c2"),
        )
        val guavaKt = GuavaKtArrayTable.create<String, String, Int?>(
            listOf("r1", "r2"),
            listOf("c1", "c2"),
        )
        assertEquals(guavaTrace(guava), guavaKtTrace(guavaKt))
    }

    @Test
    fun constructionFailureTypesMatchGuava() {
        val guavaFailures = GuavaTableNullHarness.arrayTableConstructionFailures()
        val guavaKtFailures = listOf(
            failureName { GuavaKtArrayTable.create<String, String, Int?>(emptyList(), listOf("c")) },
            failureName { GuavaKtArrayTable.create<String, String, Int?>(listOf("r", "r"), listOf("c")) },
            failureName { GuavaKtArrayTable.create<String?, String, Int?>(listOf(null), listOf("c")) },
        )
        assertEquals(guavaFailures, guavaKtFailures)
    }

    private fun guavaTrace(table: GuavaArrayTable<String, String, Int>): List<Any?> {
        val trace = mutableListOf<Any?>()
        val row = table.row("r1")
        val column = table.column("c1")
        val firstCell = table.cellSet().first()
        trace.add(table.size())
        trace.add(table.contains("r1", "c1"))
        trace.add(table.containsValue(null))
        trace.add(table.values().toList())
        trace.add(table.cellSet().map { Triple(it.rowKey, it.columnKey, it.value) })
        trace.add(row.toMap())
        trace.add(row.entries.first().setValue(1))
        trace.add(firstCell.value)
        trace.add(row.put("c2", 2))
        trace.add(column.put("r2", 3))
        trace.add(table.rowMap().mapValues { it.value.toMap() })
        trace.add(table.erase("r2", "c1"))
        trace.add(column.toMap())
        trace.add(failureName { table.remove("r1", "c1") })
        trace.add(failureName { table.clear() })
        trace.add(failureName { row.remove("c1") })
        trace.add(failureName { row["missing"] = 4 })
        trace.add(failureName {
            val iterator = table.cellSet().iterator()
            iterator.next()
            iterator.remove()
        })
        trace.add(failureName { table.rowKeySet().remove("r1") })
        return trace
    }

    private fun guavaKtTrace(table: GuavaKtArrayTable<String, String, Int?>): List<Any?> {
        val trace = mutableListOf<Any?>()
        val row = table.row("r1") as MutableMap<String, Int?>
        val column = table.column("c1") as MutableMap<String, Int?>
        val firstCell = table.cellSet().first()
        trace.add(table.size())
        trace.add(table.contains("r1", "c1"))
        trace.add(table.containsValue(null))
        trace.add(table.values().toList())
        trace.add(table.cellSet().map { Triple(it.getRowKey(), it.getColumnKey(), it.getValue()) })
        trace.add(row.toMap())
        trace.add(row.entries.first().setValue(1))
        trace.add(firstCell.getValue())
        trace.add(row.put("c2", 2))
        trace.add(column.put("r2", 3))
        trace.add(table.rowMap().mapValues { it.value.toMap() })
        trace.add(table.erase("r2", "c1"))
        trace.add(column.toMap())
        trace.add(failureName { table.remove("r1", "c1") })
        trace.add(failureName { table.clear() })
        trace.add(failureName { row.remove("c1") })
        trace.add(failureName { row["missing"] = 4 })
        trace.add(failureName {
            val iterator = table.cellSet().iterator() as MutableIterator<*>
            iterator.next()
            iterator.remove()
        })
        trace.add(failureName { (table.rowKeySet() as MutableSet<String>).remove("r1") })
        return trace
    }

    private fun failureName(block: () -> Unit): String? =
        try {
            block()
            null
        } catch (failure: Throwable) {
            failure::class.simpleName
        }
}
