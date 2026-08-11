package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Guava SortedLists — binary search on sorted lists with key-present / key-absent behaviors.
 */
internal object SortedLists {
    enum class KeyPresentBehavior {
        ANY_PRESENT,
        LAST_PRESENT,
        FIRST_PRESENT,
        FIRST_AFTER,
        LAST_BEFORE,
    }

    enum class KeyAbsentBehavior {
        NEXT_LOWER,
        NEXT_HIGHER,
        INVERTED_INSERTION_INDEX,
    }

    fun <E> binarySearch(
        list: List<out E>,
        key: E,
        comparator: Comparator<in E>,
        presentBehavior: KeyPresentBehavior,
        absentBehavior: KeyAbsentBehavior,
    ): Int {
        Preconditions.checkNotNull(list)
        Preconditions.checkNotNull(comparator)
        var lower = 0
        var upper = list.size - 1
        while (lower <= upper) {
            val middle = (lower + upper) ushr 1
            val c = comparator.compare(key, list[middle])
            when {
                c < 0 -> upper = middle - 1
                c > 0 -> lower = middle + 1
                else -> {
                    // found
                    return when (presentBehavior) {
                        KeyPresentBehavior.ANY_PRESENT -> middle
                        KeyPresentBehavior.FIRST_PRESENT -> {
                            var i = middle
                            while (i > 0 && comparator.compare(key, list[i - 1]) == 0) i--
                            i
                        }
                        KeyPresentBehavior.LAST_PRESENT -> {
                            var i = middle
                            while (i < list.size - 1 && comparator.compare(key, list[i + 1]) == 0) i++
                            i
                        }
                        KeyPresentBehavior.FIRST_AFTER -> {
                            var i = middle
                            while (i < list.size - 1 && comparator.compare(key, list[i + 1]) == 0) i++
                            i + 1
                        }
                        KeyPresentBehavior.LAST_BEFORE -> {
                            var i = middle
                            while (i > 0 && comparator.compare(key, list[i - 1]) == 0) i--
                            i - 1
                        }
                    }
                }
            }
        }
        // not found — lower is insertion point
        return when (absentBehavior) {
            KeyAbsentBehavior.NEXT_HIGHER -> lower
            KeyAbsentBehavior.NEXT_LOWER -> lower - 1
            KeyAbsentBehavior.INVERTED_INSERTION_INDEX -> lower.inv()
        }
    }

    fun <E : Comparable<E>> binarySearch(
        list: List<out E>,
        key: E,
        presentBehavior: KeyPresentBehavior,
        absentBehavior: KeyAbsentBehavior,
    ): Int = binarySearch(list, key, Comparator { a, b -> a.compareTo(b) }, presentBehavior, absentBehavior)
}
