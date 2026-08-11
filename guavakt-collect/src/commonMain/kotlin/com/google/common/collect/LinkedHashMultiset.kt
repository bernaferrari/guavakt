package dev.guavakt.collect

/** Guava LinkedHashMultiset — multiset with first-insertion-order iteration. */
class LinkedHashMultiset<E> private constructor(
    backingMap: MutableMap<E, Int>,
) : AbstractMapBasedMultiset<E>(backingMap) {
    companion object {
        fun <E> create(): LinkedHashMultiset<E> = LinkedHashMultiset(LinkedHashMap())

        fun <E> create(distinctElements: Int): LinkedHashMultiset<E> {
            CollectPreconditions.checkNonnegative(distinctElements, "distinctElements")
            return LinkedHashMultiset(LinkedHashMap(maxOf(16, distinctElements * 4 / 3 + 1)))
        }

        fun <E> create(elements: Iterable<out E>): LinkedHashMultiset<E> =
            create<E>().also { multiset -> for (element in elements) multiset.add(element) }
    }
}
