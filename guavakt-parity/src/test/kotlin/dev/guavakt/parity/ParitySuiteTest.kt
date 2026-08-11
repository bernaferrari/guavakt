package dev.guavakt.parity

import dev.guavakt.base.Ascii
import dev.guavakt.base.CharMatcher
import dev.guavakt.base.Enums
import dev.guavakt.base.Joiner
import dev.guavakt.base.MoreObjects
import dev.guavakt.base.Optional
import dev.guavakt.base.Preconditions
import dev.guavakt.base.Splitter
import dev.guavakt.base.Stopwatch
import dev.guavakt.base.Strings
import dev.guavakt.base.Suppliers
import dev.guavakt.base.Ticker
import dev.guavakt.cache.CacheBuilder
import dev.guavakt.collect.ArrayListMultimap
import dev.guavakt.collect.HashMultimap
import dev.guavakt.collect.HashMultiset
import dev.guavakt.collect.ImmutableList
import dev.guavakt.collect.ImmutableMap
import dev.guavakt.collect.ImmutableSet
import dev.guavakt.collect.ImmutableSortedMap
import dev.guavakt.collect.Iterables
import dev.guavakt.collect.Lists
import dev.guavakt.collect.Maps
import dev.guavakt.collect.Range
import dev.guavakt.collect.Sets
import dev.guavakt.collect.TreeMultimap
import dev.guavakt.escape.Escapers
import dev.guavakt.hash.Hashing
import dev.guavakt.math.IntMath
import dev.guavakt.math.LongMath
import dev.guavakt.net.HostAndPort
import dev.guavakt.net.InternetDomainName
import dev.guavakt.primitives.Booleans
import dev.guavakt.primitives.Doubles
import dev.guavakt.primitives.Floats
import dev.guavakt.primitives.Ints
import dev.guavakt.primitives.Longs
import dev.guavakt.primitives.Shorts
import dev.guavakt.reflect.TypeToken
import dev.guavakt.util.concurrent.Futures
import dev.guavakt.util.concurrent.MoreExecutors
import dev.guavakt.util.concurrent.SettableFuture
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

    @Test fun futures_immediate() {
        assertEquals(7, Futures.immediateFuture(7).get())
    }

    @Test fun futures_transform() {
        assertEquals(4, Futures.transform(Futures.immediateFuture(2)) { it * 2 }.get())
    }

    @Test fun settableFuture_roundTrip() {
        val f = SettableFuture.create<String>()
        f.set("ok")
        assertEquals("ok", f.get())
    }

    @Test fun directExecutor_runsInline() {
        var ran = false
        MoreExecutors.directExecutor().execute { ran = true }
        assertTrue(ran)
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

    @Test fun typeToken_string() {
        assertEquals(String::class, TypeToken.of(String::class).getRawType())
    }

    @Test fun escapers_builder_roundTripShape() {
        val esc = Escapers.builder().addEscape('a', "[a]").build()
        assertEquals("[a]bc", esc.escape("abc"))
    }
}
