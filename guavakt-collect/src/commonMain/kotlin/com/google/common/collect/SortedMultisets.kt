package dev.guavakt.collect

/** Internal-style helpers shared by sorted-multiset implementations. */
object SortedMultisets {
    fun <E> elementSet(multiset: SortedMultiset<E>): Set<E> = multiset.elementSet()

    fun <E> entryOrNull(element: E?, count: Int): Multiset.Entry<E>? =
        if (element == null || count == 0) null else Multisets.immutableEntry(element, count)
}
