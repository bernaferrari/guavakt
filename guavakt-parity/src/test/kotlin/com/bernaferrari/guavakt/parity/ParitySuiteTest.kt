package com.bernaferrari.guavakt.parity

import com.bernaferrari.guavakt.base.Ascii
import com.bernaferrari.guavakt.base.CharMatcher
import com.bernaferrari.guavakt.base.Joiner
import com.bernaferrari.guavakt.base.MoreObjects
import com.bernaferrari.guavakt.base.Optional
import com.bernaferrari.guavakt.base.Preconditions
import com.bernaferrari.guavakt.base.Splitter
import com.bernaferrari.guavakt.base.Stopwatch
import com.bernaferrari.guavakt.base.Strings
import com.bernaferrari.guavakt.base.Suppliers
import com.bernaferrari.guavakt.base.Ticker
import com.bernaferrari.guavakt.cache.CacheBuilder
import com.bernaferrari.guavakt.collect.ArrayListMultimap
import com.bernaferrari.guavakt.collect.HashMultimap
import com.bernaferrari.guavakt.collect.HashMultiset
import com.bernaferrari.guavakt.collect.ImmutableList
import com.bernaferrari.guavakt.collect.ImmutableMap
import com.bernaferrari.guavakt.collect.ImmutableSet
import com.bernaferrari.guavakt.collect.ImmutableSortedMap
import com.bernaferrari.guavakt.collect.Iterables
import com.bernaferrari.guavakt.collect.Lists
import com.bernaferrari.guavakt.collect.Maps
import com.bernaferrari.guavakt.collect.Range
import com.bernaferrari.guavakt.collect.Sets
import com.bernaferrari.guavakt.collect.TreeMultimap
import com.bernaferrari.guavakt.escape.Escapers
import com.bernaferrari.guavakt.hash.Hashing
import com.bernaferrari.guavakt.math.IntMath
import com.bernaferrari.guavakt.math.LongMath
import com.bernaferrari.guavakt.net.HostAndPort
import com.bernaferrari.guavakt.net.InternetDomainName
import com.bernaferrari.guavakt.primitives.Booleans
import com.bernaferrari.guavakt.primitives.Doubles
import com.bernaferrari.guavakt.primitives.Floats
import com.bernaferrari.guavakt.primitives.Ints
import com.bernaferrari.guavakt.primitives.Longs
import com.bernaferrari.guavakt.primitives.Shorts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParitySuiteTest {
    @Test fun preconditions_checkArgument() {
        assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false) }
        Preconditions.checkArgument(true)
    }

    @Test fun preconditions_elementIndex_message() {
        val e = assertFailsWith<IndexOutOfBoundsException> { Preconditions.checkElementIndex(3, 2) }
        assertTrue(e.message!!.contains("3") || e.message!!.contains("index"))
    }

    @Test fun optional_of_absent() {
        assertEquals(1, Optional.of(1).get())
        assertFalse(Optional.absent<Int>().isPresent())
    }

    @Test fun joiner_join() {
        assertEquals("a,b", Joiner.on(",").join(listOf("a", "b")))
    }

    @Test fun splitter_split() {
        assertEquals(listOf("a", "b"), Splitter.on('-').splitToList("a-b"))
    }

    @Test fun strings_nullToEmpty() {
        assertEquals("", Strings.nullToEmpty(null))
        assertEquals("x", Strings.nullToEmpty("x"))
    }

    @Test fun ascii_toLowerCase() {
        assertEquals("abc", Ascii.toLowerCase("ABC"))
    }

    @Test fun charMatcher_digit() {
        assertTrue(CharMatcher.digit().matches('5'))
        assertFalse(CharMatcher.digit().matches('a'))
    }

    @Test fun moreObjects_firstNonNull() {
        assertEquals("a", MoreObjects.firstNonNull(null, "a"))
    }

    @Test fun suppliers_memoize() {
        var n = 0
        val s = Suppliers.memoize { ++n; "v" }
        assertEquals("v", s.get())
        assertEquals("v", s.get())
        assertEquals(1, n)
    }

    @Test fun stopwatch_ticker() {
        val t = object : Ticker() {
            var nanos = 0L
            override fun read(): Long = nanos
        }
        val sw = Stopwatch.createUnstarted(t)
        sw.start()
        t.nanos = 1_000_000L
        sw.stop()
        assertTrue(sw.elapsed(kotlin.time.DurationUnit.MILLISECONDS) >= 0)
    }

    @Test fun immutableList_of() {
        assertEquals(listOf(1, 2, 3), ImmutableList.of(1, 2, 3))
    }

    @Test fun immutableMap_of() {
        assertEquals(1, ImmutableMap.of("a", 1)["a"])
    }

    @Test fun immutableSet_of() {
        assertEquals(setOf(1, 2), ImmutableSet.of(1, 2).toSet())
    }

    @Test fun immutableSortedMap_order() {
        val m = ImmutableSortedMap.copyOf(mapOf("b" to 2, "a" to 1))
        assertEquals(listOf("a", "b"), m.keys.toList())
    }

    @Test fun multimap_liveView() {
        val mm = ArrayListMultimap.create<String, Int>()
        assertTrue(mm.get("k").add(1))
        assertTrue(mm.containsEntry("k", 1))
        assertEquals(1, mm.size())
    }

    @Test fun hashMultimap_setSemantics() {
        val mm = HashMultimap.create<String, Int>()
        assertTrue(mm.put("k", 1))
        assertFalse(mm.put("k", 1))
        assertEquals(1, mm.get("k").size)
    }

    @Test fun multiset_count() {
        val ms = HashMultiset.create<String>()
        ms.add("a"); ms.add("a"); ms.add("b")
        assertEquals(2, ms.count("a"))
    }

    @Test fun treeMap_sortedKeys() {
        val m = Maps.newTreeMap<String, Int>()
        m["c"] = 3; m["a"] = 1; m["b"] = 2
        assertEquals(listOf("a", "b", "c"), m.keys.toList())
    }

    @Test fun treeSet_sorted() {
        val s = Sets.newTreeSet<Int>()
        s.addAll(listOf(3, 1, 2))
        assertEquals(listOf(1, 2, 3), s.toList())
    }

    @Test fun treeMultimap_keyOrder() {
        val mm = TreeMultimap.create<String, Int>()
        mm.put("b", 2); mm.put("a", 1); mm.put("a", 0)
        assertEquals(listOf("a", "b"), mm.keySet().toList())
        assertEquals(listOf(0, 1), mm.get("a").toList())
    }

    @Test fun range_contains() {
        val r = Range.closed(1, 5)
        assertTrue(r.contains(3))
        assertFalse(r.contains(6))
    }

    @Test fun iterables_getFirst() {
        assertEquals(1, Iterables.getFirst(listOf(1, 2), -1))
        assertEquals(-1, Iterables.getFirst(emptyList(), -1))
    }

    @Test fun lists_partition() {
        assertEquals(listOf(listOf(1, 2), listOf(3)), Lists.partition(listOf(1, 2, 3), 2))
    }

    @Test fun ints_compare() {
        assertTrue(Ints.compare(1, 2) < 0)
    }

    @Test fun longs_shorts_floats_doubles_booleans() {
        assertTrue(Longs.compare(1L, 2L) < 0)
        assertTrue(Shorts.compare(1, 2) < 0)
        assertTrue(Floats.compare(1f, 2f) < 0)
        assertTrue(Doubles.compare(1.0, 2.0) < 0)
        assertTrue(Booleans.compare(false, true) < 0)
    }

    @Test fun intMath_checkedAdd() {
        assertEquals(3, IntMath.checkedAdd(1, 2))
    }

    @Test fun longMath_checkedMultiply() {
        assertEquals(6L, LongMath.checkedMultiply(2L, 3L))
    }

    @Test fun murmur_empty_stable() {
        val h = Hashing.murmur3_32().hashBytes(ByteArray(0)).asInt()
        assertEquals(h, Hashing.murmur3_32().hashBytes(ByteArray(0)).asInt())
    }

    @Test fun cache_maxSize() {
        val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(1).build<Int, Int>()
        c.put(1, 10); c.put(2, 20)
        assertEquals(1, c.size())
    }

    @Test fun hostAndPort_parse() {
        val hp = HostAndPort.fromString("example.com:8080")
        assertEquals("example.com", hp.getHost())
        assertEquals(8080, hp.getPort())
    }

    @Test fun internetDomainName_basic() {
        val d = InternetDomainName.from("foo.bar.com")
        assertTrue(d.parts().isNotEmpty())
    }

    @Test fun escapers_builder_roundTripShape() {
        val esc = Escapers.builder().addEscape('a', "[a]").build()
        assertEquals("[a]bc", esc.escape("abc"))
    }
}
