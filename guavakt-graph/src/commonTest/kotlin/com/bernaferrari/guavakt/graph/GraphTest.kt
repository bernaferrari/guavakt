package com.bernaferrari.guavakt.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphTest {
    @Test
    fun directedGraph_adjacency() {
        val g: MutableGraph<String> = GraphBuilder.directed<String>().build()
        g.addNode("a")
        g.addNode("b")
        assertTrue(g.putEdge("a", "b"))
        assertTrue(g.hasEdgeConnecting("a", "b"))
        assertFalse(g.hasEdgeConnecting("b", "a"))
        assertEquals(setOf("b"), g.successors("a"))
        assertEquals(setOf("a"), g.predecessors("b"))
        assertEquals(setOf("a", "b"), Graphs.reachableNodes(g, "a"))
    }

    @Test
    fun directedValueGraph_adjacencyIncludesIncomingAndOutgoingNeighbors() {
        val graph = ValueGraphBuilder.directed<String, Int>().build<String, Int>()
        graph.putEdgeValue("a", "b", 1)
        graph.putEdgeValue("b", "c", 2)

        assertEquals(setOf("a", "c"), graph.adjacentNodes("b"))
        assertEquals(setOf("a"), graph.predecessors("b"))
        assertEquals(setOf("c"), graph.successors("b"))
    }

    @Test
    fun undirected_singleEdge_isAcyclic() {
        val g: MutableGraph<Int> = GraphBuilder.undirected<Int>().build()
        g.putEdge(1, 2)
        assertFalse(Graphs.hasCycle(g), "single undirected edge must not be a cycle")
    }

    @Test
    fun undirected_path_isAcyclic() {
        val g: MutableGraph<Int> = GraphBuilder.undirected<Int>().build()
        g.putEdge(1, 2)
        g.putEdge(2, 3)
        assertFalse(Graphs.hasCycle(g), "path/tree must be acyclic")
    }

    @Test
    fun undirected_triangle_hasCycle() {
        val g: MutableGraph<Int> = GraphBuilder.undirected<Int>().build()
        g.putEdge(1, 2)
        g.putEdge(2, 3)
        g.putEdge(3, 1)
        assertTrue(Graphs.hasCycle(g))
    }

    @Test
    fun directed_backEdge_hasCycle() {
        val g: MutableGraph<String> = GraphBuilder.directed<String>().build()
        g.putEdge("a", "b")
        g.putEdge("b", "a")
        assertTrue(Graphs.hasCycle(g))
    }

    @Test
    fun sortedElementOrderControlsPortableGraphAndNetworkIteration() {
        val map = ElementOrder.natural<Int>().createMap<String>(3)
        map[3] = "three"
        map[1] = "one"
        map[2] = "two"
        assertEquals(listOf(1, 2, 3), map.keys.toList())

        val graph = GraphBuilder.directed<Int>().nodeOrder(ElementOrder.natural()).build<Int>()
        graph.addNode(3)
        graph.addNode(1)
        graph.addNode(2)
        assertEquals(listOf(1, 2, 3), graph.nodes().toList())
        assertEquals(ElementOrder.Type.SORTED, graph.nodeOrder().type())

        val network = NetworkBuilder.directed<Int, String>()
            .allowsParallelEdges(true)
            .nodeOrder(ElementOrder.natural())
            .edgeOrder(ElementOrder.natural())
            .build<Int, String>()
        network.addEdge(3, 1, "z")
        network.addEdge(1, 2, "a")
        network.addEdge(2, 3, "m")
        assertEquals(listOf(1, 2, 3), network.nodes().toList())
        assertEquals(listOf("a", "m", "z"), network.edges().toList())
        assertEquals(ElementOrder.Type.SORTED, network.edgeOrder().type())
    }

    @Test
    fun heldBaseAccessorViewsTrackMutationsAndInvalidateRemovedRelations() {
        val graph = GraphBuilder.directed<String>().build<String>()
        graph.putEdge("a", "b")
        val nodes = graph.nodes()
        val edges = graph.edges()
        val successors = graph.successors("a")
        graph.putEdge("a", "c")
        assertEquals(setOf("a", "b", "c"), nodes)
        assertEquals(setOf("b", "c"), successors)
        assertEquals(2, edges.size)
        graph.removeNode("a")
        assertEquals(setOf("b", "c"), nodes)
        assertEquals(0, edges.size)
        assertFailsWith<IllegalStateException> { successors.size }

        val network = NetworkBuilder.directed<String, String>().allowsParallelEdges(true).build<String, String>()
        network.addEdge("a", "b", "ab1")
        val connecting = network.edgesConnecting("a", "b")
        network.addEdge("a", "b", "ab2")
        assertEquals(setOf("ab1", "ab2"), connecting)
        network.removeNode("a")
        assertFailsWith<IllegalStateException> { connecting.size }
    }

    @Test
    fun stableIncidentEdgeOrderRetainsConnectingInsertionOrder() {
        val graph = GraphBuilder.directed<String>()
            .incidentEdgeOrder(ElementOrder.stable())
            .build<String>()
        graph.putEdge("b", "a")
        graph.putEdge("a", "c")
        graph.putEdge("d", "a")
        graph.putEdge("a", "b")
        assertEquals(
            listOf("<b -> a>", "<a -> c>", "<d -> a>", "<a -> b>"),
            graph.incidentEdges("a").map { it.toString() },
        )
        assertEquals(ElementOrder.Type.STABLE, graph.incidentEdgeOrder().type())
        assertFailsWith<IllegalArgumentException> {
            GraphBuilder.directed<String>().incidentEdgeOrder(ElementOrder.insertion())
        }
    }

    @Test
    fun stableIncidentEdgesProjectComparatorEquivalentNodeAliases() {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val graph = GraphBuilder.directed<String>()
            .nodeOrder(ElementOrder.sorted(byLength))
            .incidentEdgeOrder(ElementOrder.stable())
            .build<String>()

        graph.putEdge("a", "cc")
        assertEquals(listOf("<b -> cc>"), graph.incidentEdges("b").map(Any::toString))
        assertEquals(listOf("<a -> dd>"), graph.incidentEdges("dd").map(Any::toString))

        graph.putEdge("b", "dd")
        assertEquals(listOf("<a -> cc>", "<b -> cc>"), graph.incidentEdges("cc").map(Any::toString))
        graph.removeNode("b")
        assertTrue(graph.edges().isEmpty())
        assertEquals(listOf("<a -> cc>"), graph.incidentEdges("cc").map(Any::toString))
        assertEquals(listOf("<a -> dd>"), graph.incidentEdges("dd").map(Any::toString))
    }
}
