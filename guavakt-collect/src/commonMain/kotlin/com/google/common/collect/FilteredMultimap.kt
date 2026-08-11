package dev.guavakt.collect

/** Guava FilteredMultimap — prefer [Multimaps.filterKeys] / [Multimaps.filterValues] / [Multimaps.filterEntries]. */
object FilteredMultimap {
    fun <K, V> filterKeys(unfiltered: Multimap<K, V>, keyPredicate: (K) -> Boolean): Multimap<K, V> =
        Multimaps.filterKeys(unfiltered, keyPredicate)
    fun <K, V> filterValues(unfiltered: Multimap<K, V>, valuePredicate: (V) -> Boolean): Multimap<K, V> =
        Multimaps.filterValues(unfiltered, valuePredicate)
    fun <K, V> filterEntries(
        unfiltered: Multimap<K, V>,
        entryPredicate: (Map.Entry<K, V>) -> Boolean,
    ): Multimap<K, V> = Multimaps.filterEntries(unfiltered, entryPredicate)
}
