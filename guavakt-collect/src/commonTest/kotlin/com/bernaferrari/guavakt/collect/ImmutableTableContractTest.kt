package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImmutableTableContractTest {
    @Test
    fun builderRejectsNullPartsAndDuplicateCoordinates() {
        assertFailsWith<NullPointerException> {
            ImmutableTable.builder<String?, String, Int>().put(null, "c", 1)
        }
        assertFailsWith<NullPointerException> {
            ImmutableTable.builder<String, String?, Int>().put("r", null, 1)
        }
        assertFailsWith<NullPointerException> {
            ImmutableTable.builder<String, String, Int?>().put("r", "c", null)
        }
        assertFailsWith<IllegalArgumentException> {
            ImmutableTable.builder<String, String, Int>()
                .put("r", "c", 1)
                .put("r", "c", 2)
                .buildOrThrow()
        }
    }

    @Test
    fun rowAndColumnOrderingIsConfigurableAndStable() {
        val reverse = Comparator<Int> { left, right -> right.compareTo(left) }
        val table = ImmutableTable.builder<Int, Int, String>()
            .put(1, 2, "a")
            .put(3, 1, "b")
            .put(1, 3, "c")
            .put(2, 2, "d")
            .orderRowsBy(reverse)
            .orderColumnsBy(reverse)
            .buildOrThrow()

        assertEquals(listOf(3, 2, 1), table.rowKeySet().toList())
        assertEquals(listOf(3, 2), table.row(1).keys.toList())
        assertEquals(
            listOf(Triple(3, 1, "b"), Triple(2, 2, "d"), Triple(1, 3, "c"), Triple(1, 2, "a")),
            table.cellSet().map { Triple(it.getRowKey(), it.getColumnKey(), it.getValue()) },
        )
    }

    @Test
    fun everyMultiCellViewRejectsMutation() {
        val table = ImmutableTable.builder<String, String, Int>()
            .put("r1", "c1", 1)
            .put("r1", "c2", 2)
            .put("r2", "c1", 3)
            .buildOrThrow()

        assertFailsWith<UnsupportedOperationException> {
            (table.row("r1") as MutableMap<String, Int>)["c1"] = 9
        }
        assertFailsWith<UnsupportedOperationException> {
            (table.column("c1") as MutableMap<String, Int>).remove("r1")
        }
        assertFailsWith<UnsupportedOperationException> {
            (table.rowMap() as MutableMap<String, Map<String, Int>>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (table.rowMap()["r1"] as MutableMap<String, Int>).remove("c1")
        }
        assertFailsWith<UnsupportedOperationException> {
            (table.cellSet() as MutableSet<Table.Cell<String, String, Int>>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (table.values() as MutableCollection<Int>).remove(99)
        }
    }
}
