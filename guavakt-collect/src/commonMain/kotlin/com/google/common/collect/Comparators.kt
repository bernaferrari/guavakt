package dev.guavakt.collect

import dev.guavakt.base.Preconditions

object Comparators {
    fun <T> lexicographical(comparator: Comparator<T>): Comparator<Iterable<T>> =
        Comparator { a, b ->
            val ia = a.iterator()
            val ib = b.iterator()
            while (ia.hasNext() && ib.hasNext()) {
                val result = comparator.compare(ia.next(), ib.next())
                if (result != 0) return@Comparator result
            }
            when {
                ia.hasNext() -> 1
                ib.hasNext() -> -1
                else -> 0
            }
        }

    fun <T> isInOrder(iterable: Iterable<T>, comparator: Comparator<in T>): Boolean {
        Preconditions.checkNotNull(comparator)
        val it = iterable.iterator()
        if (!it.hasNext()) return true
        var prev = it.next()
        while (it.hasNext()) {
            val next = it.next()
            if (comparator.compare(prev, next) > 0) return false
            prev = next
        }
        return true
    }

    fun <T> isInStrictOrder(iterable: Iterable<T>, comparator: Comparator<in T>): Boolean {
        Preconditions.checkNotNull(comparator)
        val it = iterable.iterator()
        if (!it.hasNext()) return true
        var prev = it.next()
        while (it.hasNext()) {
            val next = it.next()
            if (comparator.compare(prev, next) >= 0) return false
            prev = next
        }
        return true
    }

    fun <T : Comparable<T>> min(a: T, b: T): T = if (a <= b) a else b
    fun <T : Comparable<T>> max(a: T, b: T): T = if (a >= b) a else b
    fun <T> min(a: T, b: T, comparator: Comparator<in T>): T =
        if (comparator.compare(a, b) <= 0) a else b
    fun <T> max(a: T, b: T, comparator: Comparator<in T>): T =
        if (comparator.compare(a, b) >= 0) a else b
}
