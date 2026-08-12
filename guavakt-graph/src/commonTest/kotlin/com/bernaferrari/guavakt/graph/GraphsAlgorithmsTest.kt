package com.bernaferrari.guavakt.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphsAlgorithmsTest {
    @Test
    fun reachableAndTranspose() {
        val g: MutableGraph<String> = GraphBuilder.directed<String>().build()
        g.putEdge("a", "b")
        g.putEdge("b", "c")
        assertEquals(setOf("a", "b", "c"), Graphs.reachableNodes(g, "a"))
        val t = Graphs.transpose(g)
        assertTrue(t.hasEdgeConnecting("b", "a"))
        assertFalse(Graphs.hasCycle(g))
        g.putEdge("c", "a")
        assertTrue(Graphs.hasCycle(g))
    }

    @Test
    fun inducedSubgraph() {
        val g: MutableGraph<Int> = GraphBuilder.undirected<Int>().build()
        g.putEdge(1, 2)
        g.putEdge(2, 3)
        val sub = Graphs.inducedSubgraph(g, listOf(1, 2))
        assertTrue(sub.hasEdgeConnecting(1, 2))
        assertFalse(sub.nodes().contains(3))
    }
}
