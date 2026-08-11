package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/** Guava Collections2 — thin wrappers over Kotlin `filter` / `map` / permutations. Prefer stdlib transforms in new code. */
object Collections2 {
    fun <E> filter(unfiltered: Collection<E>, predicate: (E) -> Boolean): Collection<E> =
        unfiltered.filter(predicate)

    fun <F, T> transform(fromCollection: Collection<F>, function: (F) -> T): Collection<T> =
        fromCollection.map(function)

    fun <E> orderedPermutations(elements: Collection<E>, comparator: Comparator<in E>): Collection<List<E>> {
        val list = elements.sortedWith(comparator)
        return permutations(list).filter { Comparators.isInOrder(it, comparator) }
    }

    fun <E : Comparable<E>> orderedPermutations(elements: Collection<E>): Collection<List<E>> =
        orderedPermutations(elements, naturalOrder())

    fun <E> permutations(elements: Collection<E>): Collection<List<E>> {
        val list = elements.toList()
        if (list.isEmpty()) return listOf(emptyList())
        val result = ArrayList<List<E>>()
        permute(list.toMutableList(), 0, result)
        return result
    }

    private fun <E> permute(list: MutableList<E>, start: Int, out: MutableList<List<E>>) {
        if (start == list.size) {
            out.add(list.toList())
            return
        }
        for (i in start until list.size) {
            list.swap(start, i)
            permute(list, start + 1, out)
            list.swap(start, i)
        }
    }

    private fun <E> MutableList<E>.swap(i: Int, j: Int) {
        val t = this[i]; this[i] = this[j]; this[j] = t
    }

    fun <E> isPermutation(a: Collection<E>, b: Collection<E>): Boolean {
        if (a.size != b.size) return false
        return Multisets.difference(
            HashMultiset.create(a),
            HashMultiset.create(b),
        ).isEmpty() && Multisets.difference(
            HashMultiset.create(b),
            HashMultiset.create(a),
        ).isEmpty()
    }
}
