package com.bernaferrari.guavakt.parity

import com.google.common.collect.HashBasedTable as GuavaHashBasedTable
import com.google.common.collect.Tables as GuavaTables
import com.bernaferrari.guavakt.collect.HashBasedTable as GuavaKtHashBasedTable
import com.bernaferrari.guavakt.collect.Table as GuavaKtTable
import com.bernaferrari.guavakt.collect.Tables as GuavaKtTables
import kotlin.test.Test
import kotlin.test.assertEquals

class TableViewDifferentialTest {
    @Test
    fun hashBasedTableCapacityFactoryMatchesGuava() {
        val guava = GuavaHashBasedTable.create<String, String, Int>(1, 1)
        val guavaKt = GuavaKtHashBasedTable.create<String, String, Int>(1, 1)
        repeat(10) { index ->
            guava.put("r$index", "c$index", index)
            guavaKt.put("r$index", "c$index", index)
        }
        assertEquals(guava.size(), guavaKt.size())
        assertEquals(
            failureName { GuavaHashBasedTable.create<String, String, Int>(-1, 1) },
            failureName { GuavaKtHashBasedTable.create<String, String, Int>(-1, 1) },
        )
        assertEquals(
            failureName { GuavaHashBasedTable.create<String, String, Int>(1, -1) },
            failureName { GuavaKtHashBasedTable.create<String, String, Int>(1, -1) },
        )
    }

    @Test
    fun standardLiveViewMutationTraceMatchesGuava() {
        assertEquals(guavaStandardTrace(), guavaKtStandardTrace())
    }

    @Test
    fun transposeTraceMatchesGuava() {
        val guavaSource = GuavaHashBasedTable.create<String, String, Int>()
        val guava = GuavaTables.transpose(guavaSource)
        val guavaTrace = mutableListOf<Any?>()
        guavaSource.put("r1", "c1", 1)
        guavaTrace.add(guava.row("c1").toMap())
        guava.put("c2", "r1", 2)
        guavaTrace.add(guavaSource.row("r1").toMap())
        guavaTrace.add(GuavaTables.transpose(guava) === guavaSource)
        guavaTrace.add(guava.remove("c1", "r1"))
        guavaTrace.add(guavaSource.isEmpty)

        val guavaKtSource = GuavaKtHashBasedTable.create<String, String, Int>()
        val guavaKt = GuavaKtTables.transpose(guavaKtSource)
        val guavaKtTrace = mutableListOf<Any?>()
        guavaKtSource.put("r1", "c1", 1)
        guavaKtTrace.add(guavaKt.row("c1").toMap())
        guavaKt.put("c2", "r1", 2)
        guavaKtTrace.add(guavaKtSource.row("r1").toMap())
        guavaKtTrace.add(GuavaKtTables.transpose(guavaKt) === guavaKtSource)
        guavaKtTrace.add(guavaKt.remove("c1", "r1"))
        guavaKtTrace.add(guavaKtSource.isEmpty())

        assertEquals(guavaTrace, guavaKtTrace)
    }

    @Test
    fun transformedTableLazinessAndMutationTraceMatchesGuava() {
        val guavaSource = GuavaHashBasedTable.create<String, String, Int>()
        guavaSource.put("r", "a", 1)
        guavaSource.put("r", "b", 2)
        var guavaCalls = 0
        val guava = GuavaTables.transformValues(guavaSource) {
            guavaCalls++
            it * 10
        }
        val guavaTrace = mutableListOf<Any?>()
        guavaTrace.add(guavaCalls)
        guavaTrace.add(guava.size())
        guavaTrace.add(guavaCalls)
        guavaTrace.add(guava.get("r", "a"))
        guavaTrace.add(guavaCalls)
        guavaSource.put("r", "c", 3)
        guavaTrace.add(guava.row("r").toMap())
        guavaTrace.add(guava.remove("r", "b"))
        guavaTrace.add(guavaSource.row("r").toMap())
        guavaTrace.add(failureName { guava.put("r", "z", 90) })

        val guavaKtSource = GuavaKtHashBasedTable.create<String, String, Int>()
        guavaKtSource.put("r", "a", 1)
        guavaKtSource.put("r", "b", 2)
        var guavaKtCalls = 0
        val guavaKt = GuavaKtTables.transformValues(guavaKtSource) {
            guavaKtCalls++
            it * 10
        }
        val guavaKtTrace = mutableListOf<Any?>()
        guavaKtTrace.add(guavaKtCalls)
        guavaKtTrace.add(guavaKt.size())
        guavaKtTrace.add(guavaKtCalls)
        guavaKtTrace.add(guavaKt["r", "a"])
        guavaKtTrace.add(guavaKtCalls)
        guavaKtSource.put("r", "c", 3)
        guavaKtTrace.add(guavaKt.row("r").toMap())
        guavaKtTrace.add(guavaKt.remove("r", "b"))
        guavaKtTrace.add(guavaKtSource.row("r").toMap())
        guavaKtTrace.add(failureName { guavaKt.put("r", "z", 90) })

        assertEquals(guavaTrace, guavaKtTrace)
    }

    @Test
    fun unmodifiableTableIsDeeplyUnmodifiableAndLiveLikeGuava() {
        val guavaSource = GuavaHashBasedTable.create<String, String, Int>()
        guavaSource.put("r", "a", 1)
        val guava = GuavaTables.unmodifiableTable(guavaSource)
        val guavaRow = guava.row("r")
        val guavaTrace = mutableListOf<Any?>()
        guavaSource.put("r", "b", 2)
        guavaSource.put("s", "a", 3)
        guavaTrace.add(guavaRow.toMap())
        guavaTrace.add(guava.values().toList())
        guavaTrace.add(guava.rowMap().mapValues { it.value.toMap() })
        guavaTrace.add(failureName { guavaRow.remove("missing") })
        guavaTrace.add(failureName { guavaRow.putAll(emptyMap()) })
        guavaTrace.add(failureName { guava.values().remove(999) })
        guavaTrace.add(failureName { guava.cellSet().clear() })
        guavaTrace.add(failureName { guava.rowMap()["r"]!!.remove("a") })

        val guavaKtSource = GuavaKtHashBasedTable.create<String, String, Int>()
        guavaKtSource.put("r", "a", 1)
        val guavaKt = GuavaKtTables.unmodifiableTable(guavaKtSource)
        @Suppress("UNCHECKED_CAST")
        val guavaKtRow = guavaKt.row("r") as MutableMap<String, Int>
        val guavaKtTrace = mutableListOf<Any?>()
        guavaKtSource.put("r", "b", 2)
        guavaKtSource.put("s", "a", 3)
        guavaKtTrace.add(guavaKtRow.toMap())
        guavaKtTrace.add(guavaKt.values().toList())
        guavaKtTrace.add(guavaKt.rowMap().mapValues { it.value.toMap() })
        guavaKtTrace.add(failureName { guavaKtRow.remove("missing") })
        guavaKtTrace.add(failureName { guavaKtRow.putAll(emptyMap()) })
        @Suppress("UNCHECKED_CAST")
        guavaKtTrace.add(failureName { (guavaKt.values() as MutableCollection<Int>).remove(999) })
        @Suppress("UNCHECKED_CAST")
        guavaKtTrace.add(failureName {
            (guavaKt.cellSet() as MutableSet<GuavaKtTable.Cell<String, String, Int>>).clear()
        })
        @Suppress("UNCHECKED_CAST")
        guavaKtTrace.add(failureName {
            (guavaKt.rowMap()["r"] as MutableMap<String, Int>).remove("a")
        })

        assertEquals(guavaTrace, guavaKtTrace)
    }

    private fun guavaStandardTrace(): List<Any?> {
        val table = GuavaHashBasedTable.create<String, String, Int>()
        table.put("r1", "c1", 1)
        table.put("r1", "c2", 2)
        table.put("r2", "c1", 3)
        val row = table.row("r1")
        val column = table.column("c1")
        val cells = table.cellSet()
        val rowMap = table.rowMap()
        val trace = mutableListOf<Any?>()
        table.put("r1", "c3", 4)
        trace.add(row.toMap())
        trace.add(cells.map { Triple(it.rowKey, it.columnKey, it.value) }.toSet())
        row["c4"] = 5
        column["r3"] = 6
        trace.add(table.size())
        trace.add(column.remove("r2"))
        trace.add(table.values().remove(2))
        trace.add(table.rowKeySet().remove("r3"))
        trace.add(table.columnKeySet().remove("c4"))
        trace.add(rowMap.mapValues { it.value.toMap() })
        val entry = row.entries.first()
        trace.add(entry.setValue(7))
        trace.add(table.get("r1", entry.key))
        val iterator = cells.iterator()
        iterator.next()
        iterator.remove()
        trace.add(table.size())
        return trace
    }

    private fun guavaKtStandardTrace(): List<Any?> {
        val table = GuavaKtHashBasedTable.create<String, String, Int>()
        table.put("r1", "c1", 1)
        table.put("r1", "c2", 2)
        table.put("r2", "c1", 3)
        @Suppress("UNCHECKED_CAST")
        val row = table.row("r1") as MutableMap<String, Int>
        @Suppress("UNCHECKED_CAST")
        val column = table.column("c1") as MutableMap<String, Int>
        @Suppress("UNCHECKED_CAST")
        val cells = table.cellSet() as MutableSet<GuavaKtTable.Cell<String, String, Int>>
        val rowMap = table.rowMap()
        val trace = mutableListOf<Any?>()
        table.put("r1", "c3", 4)
        trace.add(row.toMap())
        trace.add(cells.map { Triple(it.getRowKey(), it.getColumnKey(), it.getValue()) }.toSet())
        row["c4"] = 5
        column["r3"] = 6
        trace.add(table.size())
        trace.add(column.remove("r2"))
        @Suppress("UNCHECKED_CAST")
        trace.add((table.values() as MutableCollection<Int>).remove(2))
        @Suppress("UNCHECKED_CAST")
        trace.add((table.rowKeySet() as MutableSet<String>).remove("r3"))
        @Suppress("UNCHECKED_CAST")
        trace.add((table.columnKeySet() as MutableSet<String>).remove("c4"))
        trace.add(rowMap.mapValues { it.value.toMap() })
        val entry = row.entries.first()
        trace.add(entry.setValue(7))
        trace.add(table["r1", entry.key])
        val iterator = cells.iterator()
        iterator.next()
        iterator.remove()
        trace.add(table.size())
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
