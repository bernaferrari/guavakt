package com.bernaferrari.guavakt.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkTest {
    @Test
    fun mutableNetwork_addEdge() {
        val n: MutableNetwork<String, String> = NetworkBuilder.directed<String, String>().build()
        assertTrue(n.addNode("a"))
        assertTrue(n.addEdge("a", "b", "e1"))
        assertTrue(n.hasEdgeConnecting("a", "b"))
        assertEquals(setOf("b"), n.successors("a"))
        assertEquals("e1", n.edgeConnectingOrNull("a", "b"))
    }

    @Test
    fun comparatorEquivalentEdgeAliasRemovalUsesTheStoredRawEdge() {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val network = NetworkBuilder.directed<String, String>()
            .allowsParallelEdges(true)
            .edgeOrder(ElementOrder.sorted(byLength))
            .build<String, String>()

        assertTrue(network.addEdge("a", "cc", "x"))
        assertFalse(network.addEdge("a", "cc", "y"))
        assertEquals("<a -> cc>", network.incidentNodes("y").toString())

        assertTrue(network.removeEdge("y"))
        assertTrue(network.edges().isEmpty())
        assertTrue(network.incidentEdges("a").isEmpty())
    }
}
