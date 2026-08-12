package com.bernaferrari.guavakt.collect

/** Insertion-ordered map-backed table with Guava-compatible live views. */
class HashBasedTable<R, C, V> private constructor(
    backing: MutableMap<R, MutableMap<C, V>>,
    rowFactory: () -> MutableMap<C, V>,
) : StandardTable<R, C, V>(backing, rowFactory) {
    companion object {
        /** Creates an empty insertion-ordered table with default capacities. */
        fun <R, C, V> create(): HashBasedTable<R, C, V> =
            HashBasedTable(LinkedHashMap(), { LinkedHashMap() })

        /**
         * Creates an empty table sized for the expected row and per-row cell counts.
         *
         * Capacities are allocation hints only; they never limit subsequent growth.
         */
        fun <R, C, V> create(
            expectedRows: Int,
            expectedCellsPerRow: Int,
        ): HashBasedTable<R, C, V> {
            require(expectedRows >= 0) { "expectedRows must be non-negative" }
            require(expectedCellsPerRow >= 0) { "expectedCellsPerRow must be non-negative" }
            return HashBasedTable(
                LinkedHashMap(expectedRows),
                { LinkedHashMap(expectedCellsPerRow) },
            )
        }

        /** Copies every cell from [table] while preserving its iteration order. */
        fun <R, C, V> create(table: Table<out R, out C, out V>): HashBasedTable<R, C, V> =
            create<R, C, V>().also { result -> result.putAll(table) }
    }
}
