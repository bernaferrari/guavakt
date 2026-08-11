package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

/** Guava Sets factories — **thin aliases** to [HashSet] / [LinkedHashSet] / [ComparatorTreeSet]. Prefer `hashSetOf` / `mutableSetOf`; use GuavaKt for Guava-only set views. */
@GwtCompatible
object Sets {
    fun <E> newHashSet(): MutableSet<E> = HashSet()
    fun <E> newHashSet(vararg elements: E): MutableSet<E> = hashSetOf(*elements)
    fun <E> newHashSet(elements: Iterable<E>): MutableSet<E> =
        if (elements is Collection) HashSet(elements) else HashSet<E>().apply { addAll(elements) }
    fun <E> newHashSet(elements: Iterator<E>): MutableSet<E> =
        HashSet<E>().apply { while (elements.hasNext()) add(elements.next()) }
    fun <E> newHashSetWithExpectedSize(expectedSize: Int): MutableSet<E> {
        Preconditions.checkArgument(expectedSize >= 0)
        return HashSet(MapsCapacity.capacity(expectedSize))
    }

    fun <E> newLinkedHashSet(): MutableSet<E> = LinkedHashSet()
    fun <E> newLinkedHashSet(elements: Iterable<E>): MutableSet<E> =
        LinkedHashSet<E>().apply { addAll(elements) }
    fun <E> newLinkedHashSetWithExpectedSize(expectedSize: Int): MutableSet<E> {
        Preconditions.checkArgument(expectedSize >= 0)
        return LinkedHashSet(MapsCapacity.capacity(expectedSize))
    }

    fun <E : Comparable<E>> newTreeSet(): MutableSet<E> = ComparatorTreeSet(null)
    fun <E> newTreeSet(comparator: Comparator<in E>): MutableSet<E> = ComparatorTreeSet(comparator)
    fun <E : Comparable<E>> newTreeSet(elements: Iterable<E>): MutableSet<E> =
        ComparatorTreeSet<E>(null).apply { addAll(elements) }

    fun <E> newIdentityHashSet(): MutableSet<E> = LinkedHashSet()
    fun <E> newConcurrentHashSet(): MutableSet<E> = LinkedHashSet()
    fun <E> newCopyOnWriteArraySet(): MutableSet<E> = LinkedHashSet()
    fun <E> newCopyOnWriteArraySet(elements: Iterable<E>): MutableSet<E> =
        LinkedHashSet<E>().apply { addAll(elements) }

    fun <E> immutableEnumSet(anElement: E, vararg otherElements: E): Set<E> =
        buildSet { add(anElement); addAll(otherElements) }

    fun <E> intersection(set1: Set<E>, set2: Set<*>): SetView<E> =
        object : SetView<E>() {
            override fun unmodifiableDelegate(): Set<E> =
                set1.filterTo(mutableSetOf()) { it in set2 }
            override val size: Int get() = set1.count { it in set2 }
            override fun contains(element: E): Boolean = element in set1 && element in set2
            override fun iterator(): Iterator<E> = set1.asSequence().filter { it in set2 }.iterator()
        }

    fun <E> union(set1: Set<out E>, set2: Set<out E>): SetView<E> =
        object : SetView<E>() {
            override fun unmodifiableDelegate(): Set<E> = buildSet {
                addAll(set1)
                addAll(set2)
            }
            override val size: Int
                get() {
                    var size = set1.size
                    for (e in set2) if (e !in set1) size++
                    return size
                }
            override fun contains(element: E): Boolean = element in set1 || element in set2
            override fun iterator(): Iterator<E> = iterator {
                yieldAll(set1)
                for (e in set2) if (e !in set1) yield(e)
            }
        }

    fun <E> difference(set1: Set<E>, set2: Set<*>): SetView<E> =
        object : SetView<E>() {
            override fun unmodifiableDelegate(): Set<E> =
                set1.filterTo(mutableSetOf()) { it !in set2 }
            override val size: Int get() = set1.count { it !in set2 }
            override fun contains(element: E): Boolean = element in set1 && element !in set2
            override fun iterator(): Iterator<E> = set1.asSequence().filter { it !in set2 }.iterator()
        }

    fun <E> symmetricDifference(set1: Set<out E>, set2: Set<out E>): SetView<E> =
        object : SetView<E>() {
            override fun unmodifiableDelegate(): Set<E> = buildSet {
                for (e in set1) if (e !in set2) add(e)
                for (e in set2) if (e !in set1) add(e)
            }
            override fun contains(element: E): Boolean {
                val in1 = element in set1
                val in2 = element in set2
                return in1 != in2
            }
            override fun iterator(): Iterator<E> = iterator {
                for (e in set1) if (e !in set2) yield(e)
                for (e in set2) if (e !in set1) yield(e)
            }
            override val size: Int
                get() {
                    var n = 0
                    for (e in set1) if (e !in set2) n++
                    for (e in set2) if (e !in set1) n++
                    return n
                }
        }

    fun <E> cartesianProduct(sets: List<Set<E>>): Set<List<E>> {
        if (sets.isEmpty()) return setOf(emptyList())
        var acc: List<List<E>> = listOf(emptyList())
        for (set in sets) {
            acc = acc.flatMap { prefix -> set.map { prefix + it } }
        }
        return acc.toSet()
    }

    fun <E> cartesianProduct(vararg sets: Set<E>): Set<List<E>> = cartesianProduct(sets.toList())

    fun <E> powerSet(set: Set<E>): Set<Set<E>> {
        val list = set.toList()
        val n = list.size
        Preconditions.checkArgument(n <= 30, "Too many elements to create power set: %s > 30", n)
        val result = LinkedHashSet<Set<E>>()
        for (mask in 0 until (1 shl n)) {
            val subset = LinkedHashSet<E>()
            for (i in 0 until n) if ((mask and (1 shl i)) != 0) subset.add(list[i])
            result.add(subset)
        }
        return result
    }

    fun <E> combinations(set: Set<E>, size: Int): Set<Set<E>> {
        Preconditions.checkArgument(size >= 0)
        val list = set.toList()
        if (size > list.size) return emptySet()
        if (size == 0) return setOf(emptySet())
        val result = LinkedHashSet<Set<E>>()
        fun rec(start: Int, remaining: Int, acc: MutableList<E>) {
            if (remaining == 0) {
                result.add(acc.toSet())
                return
            }
            for (i in start..list.size - remaining) {
                acc.add(list[i])
                rec(i + 1, remaining - 1, acc)
                acc.removeAt(acc.lastIndex)
            }
        }
        rec(0, size, ArrayList())
        return result
    }

    fun <E> filter(unfiltered: Set<E>, predicate: (E) -> Boolean): Set<E> =
        unfiltered.filterTo(LinkedHashSet()) { predicate(it) }

    fun <E> synchronizedSet(set: MutableSet<E>): MutableSet<E> = set
    fun <E> unmodifiableSet(set: Set<out E>): Set<E> = set.toSet()

    abstract class SetView<E> : AbstractSet<E>() {
        abstract fun unmodifiableDelegate(): Set<E>
        abstract override fun iterator(): Iterator<E>
        fun copyInto(set: MutableSet<E>): MutableSet<E> {
            set.addAll(this)
            return set
        }
        /** Snapshot as a Kotlin [Set] (Guava returned ImmutableSet). */
        fun immutableCopy(): Set<E> = toSet()
    }
}

/** Shared map capacity math (Guava Maps.capacity). */
internal object MapsCapacity {
    fun capacity(expectedSize: Int): Int {
        if (expectedSize < 3) {
            CollectPreconditions.checkNonnegative(expectedSize, "expectedSize")
            return expectedSize + 1
        }
        if (expectedSize < Int.MAX_VALUE / 2) return expectedSize + expectedSize / 3
        return Int.MAX_VALUE
    }
}
