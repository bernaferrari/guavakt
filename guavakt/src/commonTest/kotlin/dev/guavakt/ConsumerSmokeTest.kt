package dev.guavakt

import dev.guavakt.base.Joiner
import dev.guavakt.base.Preconditions
import dev.guavakt.cache.CacheBuilder
import dev.guavakt.collect.ArrayListMultimap
import dev.guavakt.collect.ImmutableList
import dev.guavakt.collect.Range
import dev.guavakt.escape.Escapers
import dev.guavakt.graph.GraphBuilder
import dev.guavakt.hash.Hashing
import dev.guavakt.html.HtmlEscapers
import dev.guavakt.io.BaseEncoding
import dev.guavakt.math.IntMath
import dev.guavakt.net.HostAndPort
import dev.guavakt.primitives.Ints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsumerSmokeTest {
    @Test
    fun multiPackage_correctValues() {
        Preconditions.checkArgument(true)
        assertEquals("a-b", Joiner.on("-").join(listOf("a", "b")))
        assertEquals(listOf(1, 2, 3), ImmutableList.copyOf(listOf(1, 2, 3)).toList())
        val mm = ArrayListMultimap.create<String, Int>()
        mm.put("k", 1)
        assertEquals(1, mm.size())
        assertTrue(Range.closed(1, 3).contains(2))
        assertEquals(6, IntMath.gcd(54, 24))
        assertEquals(3, Ints.max(intArrayOf(1, 3, 2)))
        val hash = Hashing.murmur3_32().hashUnencodedChars("guava")
        assertEquals(hash.asInt(), Hashing.murmur3_32().hashUnencodedChars("guava").asInt())
        assertEquals("&lt;x&gt;", HtmlEscapers.htmlEscaper().escape("<x>"))
        assertEquals("AQID", BaseEncoding.base64().encode(byteArrayOf(1, 2, 3)))
        val g: dev.guavakt.graph.MutableGraph<String> = GraphBuilder.directed<String>().build()
        g.putEdge("a", "b")
        assertTrue(g.hasEdgeConnecting("a", "b"))
        val cache = CacheBuilder.newBuilder<String, Int>().build(dev.guavakt.cache.CacheLoader { 99 })
        assertEquals(99, cache.get("z"))
        assertEquals(80, HostAndPort.fromString("h:80").getPort())
        assertEquals(GuavaKt.VERSION, "0.1.0-SNAPSHOT")
        // touch Escapers so escape module is exercised from umbrella
        assertEquals("x", Escapers.nullEscaper().escape("x"))
    }
}
