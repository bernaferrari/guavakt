package com.bernaferrari.guavakt

import com.bernaferrari.guavakt.base.Joiner
import com.bernaferrari.guavakt.base.Preconditions
import com.bernaferrari.guavakt.cache.CacheBuilder
import com.bernaferrari.guavakt.collect.ArrayListMultimap
import com.bernaferrari.guavakt.collect.ImmutableList
import com.bernaferrari.guavakt.collect.Range
import com.bernaferrari.guavakt.escape.Escapers
import com.bernaferrari.guavakt.graph.GraphBuilder
import com.bernaferrari.guavakt.hash.Hashing
import com.bernaferrari.guavakt.html.HtmlEscapers
import com.bernaferrari.guavakt.io.BaseEncoding
import com.bernaferrari.guavakt.math.IntMath
import com.bernaferrari.guavakt.net.HostAndPort
import com.bernaferrari.guavakt.primitives.Ints
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
        val g: com.bernaferrari.guavakt.graph.MutableGraph<String> = GraphBuilder.directed<String>().build()
        g.putEdge("a", "b")
        assertTrue(g.hasEdgeConnecting("a", "b"))
        val cache = CacheBuilder.newBuilder<String, Int>().build(com.bernaferrari.guavakt.cache.CacheLoader { 99 })
        assertEquals(99, cache.get("z"))
        assertEquals(80, HostAndPort.fromString("h:80").getPort())
        assertEquals(GuavaKt.VERSION, "0.1.0")
        // touch Escapers so escape module is exercised from umbrella
        assertEquals("x", Escapers.nullEscaper().escape("x"))
    }
}
