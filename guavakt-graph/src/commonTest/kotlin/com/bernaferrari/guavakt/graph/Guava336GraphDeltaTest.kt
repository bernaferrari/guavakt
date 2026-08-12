package com.bernaferrari.guavakt.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Guava336GraphDeltaTest {
    @Test
    fun unorderedEndpointPairHasOrderIndependentValueSemanticsButSwappedPresentation() {
        val pair = EndpointPair.unordered("a", "b")
        val reverse = EndpointPair.unordered("b", "a")

        assertEquals("[b, a]", pair.toString())
        assertEquals(listOf("b", "a"), pair.toList())
        assertEquals(reverse, pair)
        assertEquals(reverse.hashCode(), pair.hashCode())
        assertEquals("b", pair.adjacentNode("a"))
        assertFailsWith<UnsupportedOperationException> { pair.source() }
        assertFailsWith<IllegalArgumentException> { pair.adjacentNode("missing") }
    }

    @Test
    fun transitiveClosureCanLimitSelfLoopsToIncidentCycles() {
        val graph = GraphBuilder.directed<String>().allowsSelfLoops(true).build<String>()
        graph.putEdge("a", "b")
        graph.putEdge("b", "a")
        graph.putEdge("b", "c")
        graph.addNode("i")

        val always = Graphs.transitiveClosure(
            graph,
            Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS,
        )
        val cycles = Graphs.transitiveClosure(
            graph,
            Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES,
        )

        assertTrue(always.nodes().all { always.hasEdgeConnecting(it, it) })
        assertTrue(cycles.hasEdgeConnecting("a", "a"))
        assertTrue(cycles.hasEdgeConnecting("b", "b"))
        assertFalse(cycles.hasEdgeConnecting("c", "c"))
        assertFalse(cycles.hasEdgeConnecting("i", "i"))
        assertTrue(cycles.hasEdgeConnecting("a", "c"))
    }

    @Test
    fun graphAsNetworkIsLiveAndInvalidatesHeldNodeViews() {
        val graph = GraphBuilder.directed<String>().allowsSelfLoops(true).build<String>()
        graph.putEdge("a", "b")
        graph.putEdge("a", "a")
        val network = graph.asNetwork()
        val incoming = network.inEdges("b")

        assertSame(graph, network.asGraph())
        assertFalse(network.allowsParallelEdges())
        assertEquals(3, network.degree("a"))
        assertEquals(setOf(EndpointPair.ordered("a", "b")), incoming)

        graph.putEdge("c", "b")
        assertEquals(
            setOf(EndpointPair.ordered("a", "b"), EndpointPair.ordered("c", "b")),
            incoming,
        )
        assertEquals(setOf("a", "b", "c"), network.nodes())

        graph.removeNode("b")
        assertFailsWith<IllegalStateException> { incoming.size }
    }

    @Test
    fun valueGraphAsNetworkAndNetworkAsGraphRemainLive() {
        val valueGraph = ValueGraphBuilder.directed<String, Int>().build<String, Int>()
        valueGraph.putEdgeValue("a", "b", 1)
        val valueNetwork = valueGraph.asNetwork()
        assertEquals(setOf(EndpointPair.ordered("a", "b")), valueNetwork.edges())
        valueGraph.putEdgeValue("b", "c", 2)
        assertTrue(valueNetwork.hasEdgeConnecting("b", "c"))

        val network = NetworkBuilder.directed<String, String>().build<String, String>()
        network.addEdge("x", "y", "xy")
        val graphView = network.asGraph()
        network.addEdge("y", "z", "yz")
        assertTrue(graphView.hasEdgeConnecting("y", "z"))
        assertEquals(setOf("x", "y", "z"), graphView.nodes())
    }

    @Test
    fun edgeConnectingOrNullRejectsParallelEdges() {
        val network = NetworkBuilder.directed<String, String>()
            .allowsParallelEdges(true)
            .build<String, String>()
        network.addEdge("a", "b", "first")
        network.addEdge("a", "b", "second")

        assertFailsWith<IllegalArgumentException> { network.edgeConnectingOrNull("a", "b") }
    }
}
