package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Defining-API tests for stems cleared in the collect hollow drain. */
class CollectDefiningApiTest {
    @Test
    fun multisets_union_intersection_filter_unmodifiable() {
        val a = HashMultiset.create(listOf("x", "x", "y"))
        val b = HashMultiset.create(listOf("x", "z"))
        val u = Multisets.union(a, b)
        assertEquals(2, u.count("x"))
        assertEquals(1, u.count("y"))
        assertEquals(1, u.count("z"))
        val inter = Multisets.intersection(a, b)
        assertEquals(1, inter.count("x"))
        assertEquals(0, inter.count("y"))
        val filtered = Multisets.filter(a) { it == "x" }
        assertEquals(2, filtered.count("x"))
        assertEquals(0, filtered.count("y"))
        val unmod = Multisets.unmodifiableMultiset(a)
        assertEquals(2, unmod.count("x"))
        var threw = false
        try {
            unmod.add("w", 1)
        } catch (_: UnsupportedOperationException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun fluentIterable_from_preserves_input() {
        val fi = FluentIterable.from(listOf(1, 2, 3))
        assertFalse(fi.isEmpty())
        assertEquals(3, fi.size())
        assertTrue(fi.contains(2))
        assertEquals(listOf(2, 3), fi.skip(1).toList())
        assertEquals(listOf("1", "2", "3"), fi.transform { it.toString() }.toList())
        val cat = FluentIterable.concat(listOf("a"), listOf("b", "c"))
        assertEquals(listOf("a", "b", "c"), cat.toList())
        assertFalse(FluentIterable.of(1).isEmpty())
    }

    @Test
    fun immutableSortedMultiset_factories() {
        val empty = ImmutableSortedMultiset.of<String>()
        assertTrue(empty.isEmpty())
        val one = ImmutableSortedMultiset.of("b")
        assertEquals(1, one.count("b"))
        val copy = ImmutableSortedMultiset.copyOf(listOf("b", "a", "b", "c"))
        assertNotNull(copy)
        assertEquals(2, copy.count("b"))
        assertEquals(listOf("a", "b", "b", "c"), copy.asList())
        val built = ImmutableSortedMultiset.builder<String>().add("z").add("a").add("z").build()
        assertEquals(2, built.count("z"))
        assertEquals("a", built.elementSet().first())
    }

    @Test
    fun multimaps_newListMultimap_nonNull() {
        val mm = Multimaps.newListMultimap<String, Int>()
        assertNotNull(mm)
        mm.put("k", 1)
        assertEquals(1, mm.size())
    }
}
