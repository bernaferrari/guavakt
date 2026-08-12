package com.bernaferrari.guavakt.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransposeLiveViewTest {
    @Test fun transposeIsLiveAndDoubleTransposeReturnsOriginal() {
        val graph = GraphBuilder.directed<String>().build<String>()
        val transpose = Graphs.transpose(graph)

        graph.putEdge("a", "b")
        assertTrue(transpose.hasEdgeConnecting("b", "a"))
        assertFalse(transpose.hasEdgeConnecting("a", "b"))

        graph.putEdge("c", "a")
        assertEquals(setOf("b", "c"), transpose.predecessors("a") + transpose.successors("a"))
        assertSame(graph, Graphs.transpose(transpose))
    }

    @Test fun directedDegreeCountsBothDirectionsAndSelfLoopTwice() {
        val graph = GraphBuilder.directed<String>().allowsSelfLoops(true).build<String>()
        graph.putEdge("a", "a")
        graph.putEdge("b", "a")
        assertEquals(3, graph.degree("a"))
    }
}
