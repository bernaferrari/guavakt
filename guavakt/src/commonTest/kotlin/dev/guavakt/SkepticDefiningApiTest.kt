package dev.guavakt

import dev.guavakt.cache.Cache
import dev.guavakt.cache.CacheBuilderSpec
import dev.guavakt.cache.ForwardingCache
import dev.guavakt.collect.AbstractBiMap
import dev.guavakt.collect.CompactHashMap
import dev.guavakt.collect.ForwardingMap
import dev.guavakt.collect.HashBasedTable
import dev.guavakt.collect.ImmutableBiMap
import dev.guavakt.collect.LinkedHashMultiset
import dev.guavakt.collect.ObjectArrays
import dev.guavakt.collect.Tables
import dev.guavakt.math.BigIntegerMath
import dev.guavakt.math.LinearTransformation
import dev.guavakt.net.HttpHeaders
import dev.guavakt.primitives.UnsignedLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkepticDefiningApiTest {
    @Test
    fun unsignedLongArithmetic() {
        val a = UnsignedLong.valueOf(5)
        val b = UnsignedLong.valueOf(3)
        assertEquals(UnsignedLong.valueOf(8), a.plus(b))
        assertEquals(UnsignedLong.valueOf(2), a.minus(b))
        assertEquals(UnsignedLong.valueOf(15), a.times(b))
    }

    @Test
    fun tablesTranspose() {
        val t = HashBasedTable.create<String, String, Int>()
        t.put("r", "c", 7)
        assertEquals(7, Tables.transpose(t).get("c", "r"))
        assertEquals("r", Tables.immutableCell("r", "c", 7).getRowKey())
    }

    @Test
    fun httpHeadersConstants() {
        assertEquals("Content-Type", HttpHeaders.CONTENT_TYPE)
        assertEquals("Authorization", HttpHeaders.AUTHORIZATION)
    }

    @Test
    fun linearTransformationAndBigIntegerMath() {
        val lt = LinearTransformation.mapping(0.0, 0.0).withSlope(2.0)
        assertEquals(4.0, lt.transform(2.0), 1e-9)
        assertEquals("6", BigIntegerMath.factorial(3).toString())
    }

    @Test
    fun cacheBuilderSpecAndForwardingCache() {
        val spec = CacheBuilderSpec.parse("maximumSize=10")
        assertEquals(10L, spec.maximumSize)
        val inner = object : Cache<String, Int> {
            private val m = LinkedHashMap<String, Int>()
            override fun getIfPresent(key: String): Int? = m[key]
            override fun get(key: String, loader: () -> Int): Int = m.getOrPut(key, loader)
            override fun getAllPresent(keys: Iterable<String>): Map<String, Int> =
                keys.mapNotNull { k -> m[k]?.let { k to it } }.toMap()
            override fun put(key: String, value: Int) { m[key] = value }
            override fun putAll(map: Map<out String, Int>) { m.putAll(map) }
            override fun invalidate(key: String) { m.remove(key) }
            override fun invalidateAll(keys: Iterable<String>) { keys.forEach { m.remove(it) } }
            override fun invalidateAll() { m.clear() }
            override fun size(): Long = m.size.toLong()
            override fun stats() = dev.guavakt.cache.CacheStats(0, 0, 0, 0, 0, 0)
            override fun asMap(): Map<String, Int> = m.toMap()
            override fun cleanUp() {}
        }
        val fwd = object : ForwardingCache<String, Int>() {
            override fun delegate(): Cache<String, Int> = inner
        }
        fwd.put("a", 1)
        assertEquals(1, fwd.getIfPresent("a"))
    }

    @Test
    fun linkedHashMultisetAndObjectArraysAndBiMap() {
        val ms = LinkedHashMultiset.create<String>()
        ms.add("b"); ms.add("a"); ms.add("b")
        assertEquals(listOf("b", "b", "a"), ms.toList())
        val arr = ObjectArrays.newArray<Any>(2)
        arr[0] = "x"
        arr[1] = "y"
        ObjectArrays.checkElementsNotNull(arr)
        assertEquals("x", arr[0])
        val inv: Map<Int, String> = ImmutableBiMap.of("a", 1).inverse()
        assertEquals("a", inv[1])
    }

    @Test
    fun abstractBiMapInverseAndForcePut() {
        val m = AbstractBiMap.create<String, Int>()
        m.put("a", 1)
        assertEquals("a", m.inverse().get(1))
        m.forcePut("b", 1)
        assertEquals("b", m.inverse().get(1))
        assertEquals(null, m.get("a"))
    }

    @Test
    fun forwardingMapDelegates() {
        val inner = LinkedHashMap<String, Int>()
        val fwd = object : ForwardingMap<String, Int>() {
            override fun delegate(): MutableMap<String, Int> = inner
        }
        fwd.put("k", 2)
        assertEquals(2, inner["k"])
    }

    @Test
    fun compactHashMapFactories() {
        val m = CompactHashMap.createWithExpectedSize<String, Int>(8)
        m.put("x", 1)
        assertEquals(1, m["x"])
        m.trimToSize()
    }

    @Test
    fun forwardingListMultimapGetPutRemoveAll() {
        val inner = dev.guavakt.collect.ArrayListMultimap.create<String, Int>()
        val fwd = object : dev.guavakt.collect.ForwardingListMultimap<String, Int>() {
            override fun delegate() = inner
        }
        assertTrue(fwd.put("a", 1))
        assertEquals(listOf(1), fwd.get("a"))
        assertEquals(listOf(1), fwd.removeAll("a"))
        assertTrue(fwd.get("a").isEmpty())
    }

    @Test
    fun transformedAndSequentialIterators() {
        val backing = listOf(1, 2, 3).iterator()
        val transformed = object : dev.guavakt.collect.TransformedIterator<Int, String>(backing) {
            override fun transform(from: Int): String = "n=$from"
        }
        assertEquals(listOf("n=1", "n=2", "n=3"), transformed.asSequence().toList())
        val seq = object : dev.guavakt.collect.AbstractSequentialIterator<Int>(1) {
            override fun computeNext(previous: Int): Int? = if (previous >= 3) null else previous + 1
        }
        assertEquals(listOf(1, 2, 3), seq.asSequence().toList())
    }

    @Test
    fun networkAddEdgeIncidentNodes() {
        val net = dev.guavakt.graph.NetworkBuilder.directed<String, String>().build<String, String>()
        net.addEdge("a", "b", "e1")
        assertEquals(setOf("a", "b"), net.nodes())
        assertEquals(setOf("e1"), net.edges())
        assertEquals("a", net.incidentNodes("e1").nodeU)
        assertEquals("b", net.incidentNodes("e1").nodeV)
        assertTrue(net.hasEdgeConnecting("a", "b"))
    }
}
