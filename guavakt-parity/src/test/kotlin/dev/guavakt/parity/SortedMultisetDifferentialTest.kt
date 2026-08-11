package dev.guavakt.parity

import com.google.common.collect.BoundType as GuavaBoundType
import com.google.common.collect.ImmutableSortedMultiset as GuavaImmutableSortedMultiset
import com.google.common.collect.Multisets as GuavaMultisets
import com.google.common.collect.SortedMultiset as GuavaSortedMultiset
import com.google.common.collect.TreeMultiset as GuavaTreeMultiset
import dev.guavakt.collect.BoundType as GuavaKtBoundType
import dev.guavakt.collect.ImmutableSortedMultiset as GuavaKtImmutableSortedMultiset
import dev.guavakt.collect.Multisets as GuavaKtMultisets
import dev.guavakt.collect.SortedMultiset as GuavaKtSortedMultiset
import dev.guavakt.collect.TreeMultiset as GuavaKtTreeMultiset
import kotlin.test.Test
import kotlin.test.assertEquals

class SortedMultisetDifferentialTest {
    @Test
    fun naturalOrderingBoundariesRangesAndPollingMatchGuava() {
        assertEquals(guavaNaturalTrace(), guavaKtNaturalTrace())
    }

    @Test
    fun comparatorDefinesElementIdentityLikeTreeMap() {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val guava = GuavaTreeMultiset.create(byLength)
        val guavaKt = GuavaKtTreeMultiset.create(byLength)

        val guavaTrace = listOf(
            guava.add("aa", 2),
            guava.add("bb", 3),
            guava.count("aa"),
            guava.count("bb"),
            guava.elementSet().toList(),
            guava.entrySet().map { it.element to it.count },
        )
        val guavaKtTrace = listOf(
            guavaKt.add("aa", 2),
            guavaKt.add("bb", 3),
            guavaKt.count("aa"),
            guavaKt.count("bb"),
            guavaKt.elementSet().toList(),
            guavaKt.entrySet().map { it.getElement() to it.getCount() },
        )
        assertEquals(guavaTrace, guavaKtTrace)

        val guavaRange = guava.headMultiset("xxx", GuavaBoundType.OPEN)
        val guavaKtRange = guavaKt.headMultiset("xxx", GuavaKtBoundType.OPEN)
        assertEquals(guavaRange.count("zz"), guavaKtRange.count("zz"))
        assertEquals(guavaRange.remove("zz", 1), guavaKtRange.remove("zz", 1))
        assertEquals(guava.count("aa"), guavaKt.count("aa"))

        val guavaImmutable = GuavaImmutableSortedMultiset.orderedBy(byLength)
            .addCopies("aa", 2).addCopies("bb", 3).build()
        val guavaKtImmutable = GuavaKtImmutableSortedMultiset.orderedBy(byLength)
            .addCopies("aa", 2).addCopies("bb", 3).build()
        assertEquals(
            listOf(guavaImmutable.count("zz"), guavaImmutable.elementSet().toList()),
            listOf(guavaKtImmutable.count("zz"), guavaKtImmutable.elementSet().toList()),
        )
    }

    @Test
    fun naturalImmutableMultisetUsesComparisonRatherThanEqualsForIdentity() {
        val first = ComparableToken(1, "first")
        val equivalent = ComparableToken(1, "equivalent")
        val later = ComparableToken(2, "later")
        val guava = GuavaImmutableSortedMultiset.copyOf(listOf(first, equivalent, later))
        val guavaKt = GuavaKtImmutableSortedMultiset.copyOf(listOf(first, equivalent, later))

        assertEquals(
            listOf(guava.count(equivalent), guava.elementSet().map { it.group }, guava.toList().map { it.group }),
            listOf(guavaKt.count(equivalent), guavaKt.elementSet().map { it.group }, guavaKt.toList().map { it.group }),
        )
    }

    @Test
    fun descendingAndUnmodifiableViewsMatchGuava() {
        assertEquals(guavaDescendingTrace(), guavaKtDescendingTrace())
    }

    @Test
    fun immutableSortedRangesDescendingAndPollingMatchGuava() {
        val guava = GuavaImmutableSortedMultiset.copyOf(listOf(3, 1, 2, 2, 4))
        val guavaKt = GuavaKtImmutableSortedMultiset.copyOf(listOf(3, 1, 2, 2, 4))
        assertEquals(
            listOf(
                guava.elementSet().toList(),
                entryPair(guava.firstEntry()),
                entryPair(guava.lastEntry()),
                guava.headMultiset(3, GuavaBoundType.CLOSED).toList(),
                guava.tailMultiset(2, GuavaBoundType.OPEN).toList(),
                guava.descendingMultiset().toList(),
                guava.descendingMultiset().descendingMultiset() === guava,
                failureName { guava.pollFirstEntry() },
            ),
            listOf(
                guavaKt.elementSet().toList(),
                entryPairKt(guavaKt.firstEntry()),
                entryPairKt(guavaKt.lastEntry()),
                guavaKt.headMultiset(3, GuavaKtBoundType.CLOSED).toList(),
                guavaKt.tailMultiset(2, GuavaKtBoundType.OPEN).toList(),
                guavaKt.descendingMultiset().toList(),
                guavaKt.descendingMultiset().descendingMultiset() === guavaKt,
                failureName { guavaKt.pollFirstEntry() },
            ),
        )
    }

    private fun guavaNaturalTrace(): List<Any?> {
        val multiset = naturalGuava()
        val head = multiset.headMultiset(3, GuavaBoundType.CLOSED)
        val trace = mutableListOf<Any?>(
            multiset.elementSet().toList(),
            multiset.toList(),
            entryPair(multiset.firstEntry()),
            entryPair(multiset.lastEntry()),
            entries(multiset.headMultiset(3, GuavaBoundType.OPEN)),
            entries(head),
            entries(multiset.tailMultiset(2, GuavaBoundType.OPEN)),
            entries(multiset.subMultiset(2, GuavaBoundType.CLOSED, 4, GuavaBoundType.OPEN)),
        )
        multiset.add(0, 2)
        multiset.add(2, 1)
        trace.add(entries(head))
        trace.addAll(listOf(
            head.remove(2, 2),
            multiset.count(2),
            head.add(1, 2),
            failureName { head.add(4, 1) },
            failureName { head.add(4, 0) },
            failureName { head.setCount(4, 0) },
            entryPair(multiset.pollFirstEntry()),
            entryPair(multiset.pollLastEntry()),
            entries(multiset),
        ))
        return trace
    }

    private fun guavaKtNaturalTrace(): List<Any?> {
        val multiset = naturalGuavaKt()
        val head = multiset.headMultiset(3, GuavaKtBoundType.CLOSED)
        val trace = mutableListOf<Any?>(
            multiset.elementSet().toList(),
            multiset.toList(),
            entryPairKt(multiset.firstEntry()),
            entryPairKt(multiset.lastEntry()),
            entriesKt(multiset.headMultiset(3, GuavaKtBoundType.OPEN)),
            entriesKt(head),
            entriesKt(multiset.tailMultiset(2, GuavaKtBoundType.OPEN)),
            entriesKt(multiset.subMultiset(2, GuavaKtBoundType.CLOSED, 4, GuavaKtBoundType.OPEN)),
        )
        multiset.add(0, 2)
        multiset.add(2, 1)
        trace.add(entriesKt(head))
        trace.addAll(listOf(
            head.remove(2, 2),
            multiset.count(2),
            head.add(1, 2),
            failureName { head.add(4, 1) },
            failureName { head.add(4, 0) },
            failureName { head.setCount(4, 0) },
            entryPairKt(multiset.pollFirstEntry()),
            entryPairKt(multiset.pollLastEntry()),
            entriesKt(multiset),
        ))
        return trace
    }

    private fun guavaDescendingTrace(): List<Any?> {
        val source = naturalGuava()
        val descending = source.descendingMultiset()
        val unmodifiable = GuavaMultisets.unmodifiableSortedMultiset(source)
        source.add(5, 2)
        return listOf(
            descending.elementSet().toList(),
            descending.toList(),
            entryPair(descending.firstEntry()),
            entryPair(descending.lastEntry()),
            descending.descendingMultiset() === source,
            entries(descending.headMultiset(3, GuavaBoundType.CLOSED)),
            entries(descending.tailMultiset(3, GuavaBoundType.OPEN)),
            unmodifiable.count(5),
            unmodifiable.descendingMultiset().descendingMultiset() === unmodifiable,
            failureName { unmodifiable.add(6, 1) },
            failureName { unmodifiable.pollLastEntry() },
            failureName { unmodifiable.elementSet().remove(1) },
        )
    }

    private fun guavaKtDescendingTrace(): List<Any?> {
        val source = naturalGuavaKt()
        val descending = source.descendingMultiset()
        val unmodifiable = GuavaKtMultisets.unmodifiableSortedMultiset(source)
        source.add(5, 2)
        return listOf(
            descending.elementSet().toList(),
            descending.toList(),
            entryPairKt(descending.firstEntry()),
            entryPairKt(descending.lastEntry()),
            descending.descendingMultiset() === source,
            entriesKt(descending.headMultiset(3, GuavaKtBoundType.CLOSED)),
            entriesKt(descending.tailMultiset(3, GuavaKtBoundType.OPEN)),
            unmodifiable.count(5),
            unmodifiable.descendingMultiset().descendingMultiset() === unmodifiable,
            failureName { unmodifiable.add(6, 1) },
            failureName { unmodifiable.pollLastEntry() },
            failureName { (unmodifiable.elementSet() as MutableSet).remove(1) },
        )
    }

    private fun naturalGuava(): GuavaSortedMultiset<Int> = GuavaTreeMultiset.create<Int>().apply {
        add(3, 2)
        add(1, 1)
        add(2, 3)
        add(4, 1)
    }

    private fun naturalGuavaKt(): GuavaKtSortedMultiset<Int> = GuavaKtTreeMultiset.create<Int>().apply {
        add(3, 2)
        add(1, 1)
        add(2, 3)
        add(4, 1)
    }

    private fun entries(multiset: GuavaSortedMultiset<Int>) =
        multiset.entrySet().map { it.element to it.count }

    private fun entriesKt(multiset: GuavaKtSortedMultiset<Int>) =
        multiset.entrySet().map { it.getElement() to it.getCount() }

    private fun <E> entryPair(entry: com.google.common.collect.Multiset.Entry<E>?): Pair<E, Int>? =
        entry?.let { it.element to it.count }

    private fun <E> entryPairKt(entry: dev.guavakt.collect.Multiset.Entry<E>?): Pair<E, Int>? =
        entry?.let { it.getElement() to it.getCount() }

    private data class ComparableToken(val group: Int, val label: String) : Comparable<ComparableToken> {
        override fun compareTo(other: ComparableToken): Int = group.compareTo(other.group)
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
