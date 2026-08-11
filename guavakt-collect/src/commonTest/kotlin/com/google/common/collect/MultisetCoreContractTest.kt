package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultisetCoreContractTest {
    @Test
    fun liveViewsWriteThroughAndRemainLive() {
        val multiset = LinkedHashMultiset.create<String>()
        multiset.add("a", 2)
        val elements = multiset.elementSet() as MutableSet<String>
        val entries = multiset.entrySet() as MutableSet<Multiset.Entry<String>>

        multiset.add("b", 3)
        assertEquals(listOf("a", "b"), elements.toList())
        assertTrue(entries.contains(Multisets.immutableEntry("b", 3)))
        assertFalse(entries.remove(Multisets.immutableEntry("b", 2)))
        assertTrue(entries.remove(Multisets.immutableEntry("b", 3)))
        assertEquals(2, multiset.size)

        assertTrue(elements.remove("a"))
        assertTrue(multiset.isEmpty())
        multiset.add("c", 4)
        assertEquals(setOf("c"), elements)
        assertEquals(setOf(Multisets.immutableEntry("c", 4)), entries)
    }

    @Test
    fun nullableElementsSupportCountAndRemoval() {
        val multiset = LinkedHashMultiset.create<String?>()
        multiset.add(null, 3)
        assertEquals(3, multiset.count(null))
        assertEquals(3, multiset.remove(null, 1))
        assertEquals(2, multiset.count(null))
        assertTrue((multiset.elementSet() as MutableSet<String?>).remove(null))
        assertTrue(multiset.isEmpty())
    }

    @Test
    fun countBasedEqualityHashAndString() {
        val first = LinkedHashMultiset.create(listOf("a", "b", "a"))
        val second = HashMultiset.create(listOf("b", "a", "a"))
        assertEquals<Any>(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals("[a x 2, b]", first.toString())
    }

    @Test
    fun totalSizeSaturatesAndPerElementCountCannotOverflow() {
        val multiset = HashMultiset.create<String>()
        multiset.add("a", Int.MAX_VALUE)
        multiset.add("b", Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, multiset.size)
        kotlin.test.assertFailsWith<IllegalArgumentException> { multiset.add("a", 1) }
    }

    @Test
    fun unmodifiableViewIsLiveAndAllExposedViewsRejectMutation() {
        val source = LinkedHashMultiset.create(listOf("a", "a"))
        val view = Multisets.unmodifiableMultiset(source)
        source.add("b", 3)
        assertEquals(3, view.count("b"))
        kotlin.test.assertFailsWith<UnsupportedOperationException> { view.add("c", 1) }
        kotlin.test.assertFailsWith<UnsupportedOperationException> {
            (view.elementSet() as MutableSet<String>).remove("a")
        }
        kotlin.test.assertFailsWith<UnsupportedOperationException> {
            (view.entrySet() as MutableSet<Multiset.Entry<String>>)
                .remove(Multisets.immutableEntry("a", 2))
        }
    }

    @Test
    fun computedMultisetOperationsAreLiveReadOnlyViews() {
        val first = LinkedHashMultiset.create(listOf("a", "a", "b"))
        val second = LinkedHashMultiset.create(listOf("a", "c", "c", "c"))
        val union = Multisets.union(first, second)
        val intersection = Multisets.intersection(first, second)
        val sum = Multisets.sum(first, second)
        val difference = Multisets.difference(first, second)

        first.add("c", 2)
        second.add("b", 4)
        assertEquals(listOf(2, 4, 3), listOf("a", "b", "c").map(union::count))
        assertEquals(listOf(1, 1, 2), listOf("a", "b", "c").map(intersection::count))
        assertEquals(listOf(3, 5, 5), listOf("a", "b", "c").map(sum::count))
        assertEquals(listOf(1, 0, 0), listOf("a", "b", "c").map(difference::count))
        kotlin.test.assertFailsWith<UnsupportedOperationException> { union.add("d", 1) }
    }

    @Test
    fun filteredMultisetIsLiveSupportsNullAndWritesThrough() {
        val source = LinkedHashMultiset.create<String?>()
        source.add(null, 2)
        source.add("keep", 3)
        source.add("drop", 4)
        val filtered = Multisets.filter(source) { it == null || it.startsWith("k") }
        assertEquals(2, filtered.count(null))
        source.add("known", 2)
        assertEquals(2, filtered.count("known"))
        assertEquals(3, filtered.remove("keep", 1))
        assertEquals(2, source.count("keep"))
        assertTrue((filtered.elementSet() as MutableSet<String?>).remove(null))
        assertEquals(0, source.count(null))
        kotlin.test.assertFailsWith<IllegalArgumentException> { filtered.add("drop", 1) }
    }
}
