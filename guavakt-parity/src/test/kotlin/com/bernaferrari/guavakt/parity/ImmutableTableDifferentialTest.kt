package com.bernaferrari.guavakt.parity

import com.google.common.collect.ImmutableTable as GuavaImmutableTable
import com.bernaferrari.guavakt.collect.ImmutableTable as GuavaKtImmutableTable
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableTableDifferentialTest {
    @Test
    fun duplicateNullOrderingAndMutationTracesMatchGuava() {
        assertEquals(guavaFailureTrace(), guavaKtFailureTrace())
        assertEquals(guavaOrderedTrace(), guavaKtOrderedTrace())
    }

    private fun guavaFailureTrace(): List<Any?> = GuavaTableNullHarness.immutableTableFailures()

    private fun guavaKtFailureTrace(): List<Any?> = listOf(
        failureName {
            GuavaKtImmutableTable.builder<String, String, Int>()
                .put("r", "c", 1)
                .put("r", "c", 2)
                .buildOrThrow()
        },
        failureName { GuavaKtImmutableTable.builder<String?, String, Int>().put(null, "c", 1) },
        failureName { GuavaKtImmutableTable.builder<String, String?, Int>().put("r", null, 1) },
        failureName { GuavaKtImmutableTable.builder<String, String, Int?>().put("r", "c", null) },
    )

    private fun guavaOrderedTrace(): List<Any?> {
        val reverse = Comparator<Int> { left, right -> right.compareTo(left) }
        val table = GuavaImmutableTable.builder<Int, Int, String>()
            .put(1, 2, "a")
            .put(3, 1, "b")
            .put(1, 3, "c")
            .put(2, 2, "d")
            .orderRowsBy(reverse)
            .orderColumnsBy(reverse)
            .buildOrThrow()
        return immutableTrace(
            table.rowKeySet().toList(),
            table.row(1).keys.toList(),
            table.cellSet().map { Triple(it.rowKey, it.columnKey, it.value) },
            failureName { table.row(99).clear() },
            failureName { table.values().remove("missing") },
        )
    }

    private fun guavaKtOrderedTrace(): List<Any?> {
        val reverse = Comparator<Int> { left, right -> right.compareTo(left) }
        val table = GuavaKtImmutableTable.builder<Int, Int, String>()
            .put(1, 2, "a")
            .put(3, 1, "b")
            .put(1, 3, "c")
            .put(2, 2, "d")
            .orderRowsBy(reverse)
            .orderColumnsBy(reverse)
            .buildOrThrow()
        return immutableTrace(
            table.rowKeySet().toList(),
            table.row(1).keys.toList(),
            table.cellSet().map { Triple(it.getRowKey(), it.getColumnKey(), it.getValue()) },
            failureName { (table.row(99) as MutableMap<Int, String>).clear() },
            failureName { (table.values() as MutableCollection<String>).remove("missing") },
        )
    }

    private fun immutableTrace(
        rows: List<Int>,
        columns: List<Int>,
        cells: List<Triple<Int, Int, String>>,
        emptyRowMutation: String?,
        absentValueMutation: String?,
    ): List<Any?> = listOf(rows, columns, cells, emptyRowMutation, absentValueMutation)

    private fun failureName(block: () -> Unit): String? =
        try {
            block()
            null
        } catch (failure: Throwable) {
            failure::class.simpleName
        }
}
