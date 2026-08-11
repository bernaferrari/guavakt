package dev.guavakt.parity

import com.google.common.graph.Guava336GraphHarness
import dev.guavakt.graph.EndpointPair
import dev.guavakt.graph.ElementOrder
import dev.guavakt.graph.Graph
import dev.guavakt.graph.GraphBuilder
import dev.guavakt.graph.Graphs
import dev.guavakt.graph.ImmutableGraph
import dev.guavakt.graph.ImmutableValueGraph
import dev.guavakt.graph.NetworkBuilder
import dev.guavakt.graph.ValueGraphBuilder
import dev.guavakt.hash.BloomFilter
import dev.guavakt.hash.Funnels
import dev.guavakt.math.IntMath
import dev.guavakt.math.LongMath
import kotlin.test.Test
import kotlin.test.assertEquals

class Guava336DeltaDifferentialTest {
    @Test
    fun saturatedAbsMatchesGuava() {
        assertEquals(
            Guava336Harness.mathTrace(),
            listOf(
                IntMath.saturatedAbs(Int.MIN_VALUE),
                IntMath.saturatedAbs(Int.MAX_VALUE),
                IntMath.saturatedAbs(-10),
                IntMath.saturatedAbs(0),
                LongMath.saturatedAbs(Long.MIN_VALUE),
                LongMath.saturatedAbs(Long.MAX_VALUE),
                LongMath.saturatedAbs(-10),
                LongMath.saturatedAbs(0),
            ),
        )
    }

    @Test
    fun bloomSerializedSizeMatchesGuava() {
        assertEquals(
            Guava336Harness.bloomSerializedSizes(),
            listOf(
                BloomFilter.create(Funnels.integerFunnel(), 0).serializedSize(),
                BloomFilter.create(Funnels.integerFunnel(), 1).serializedSize(),
                BloomFilter.create(Funnels.integerFunnel(), 100, 0.01).serializedSize(),
                BloomFilter.create(Funnels.integerFunnel(), 1_000, 0.01).serializedSize(),
            ),
        )
    }

    @Test
    fun endpointPairValueSemanticsMatchGuava() {
        val ordered = EndpointPair.ordered("a", "b")
        val unordered = EndpointPair.unordered("a", "b")
        val reversed = EndpointPair.unordered("b", "a")
        val trace = listOf(
            ordered.toString(),
            unordered.toString(),
            ordered == EndpointPair.ordered("a", "b"),
            ordered == EndpointPair.ordered("b", "a"),
            unordered == reversed,
            unordered.hashCode() == reversed.hashCode(),
            ordered.source(),
            ordered.target(),
            unordered.adjacentNode("a"),
            unordered.toList(),
            failureName { unordered.source() },
            failureName { unordered.adjacentNode("missing") },
        )
        assertEquals(Guava336Harness.endpointPairTrace(), trace)
    }

    @Test
    fun transitiveClosureStrategiesMatchGuava() {
        assertEquals(Guava336GraphHarness.closureTraces(), kotlinClosureTraces())
    }

    @Test
    fun graphAsNetworkViewMatchesGuava() {
        assertEquals(Guava336GraphHarness.asNetworkTrace(), kotlinAsNetworkTrace())
    }

    @Test
    fun valueGraphUtilitiesMatchGuava() {
        assertEquals(Guava336GraphHarness.valueGraphUtilityTrace(), kotlinValueGraphUtilityTrace())
    }

    @Test
    fun networkUtilitiesMatchGuava() {
        assertEquals(Guava336GraphHarness.networkUtilityTrace(), kotlinNetworkUtilityTrace())
    }

    @Test
    fun graphOrderingMatchesGuava() {
        assertEquals(Guava336GraphHarness.orderingTrace(), kotlinOrderingTrace())
    }

    @Test
    fun baseGraphAccessorViewsMatchGuava() {
        assertEquals(Guava336GraphHarness.baseAccessorViewTrace(), kotlinBaseAccessorViewTrace())
    }

    @Test
    fun comparatorEquivalentGraphNodesMatchGuava() {
        assertEquals(Guava336GraphHarness.comparatorEquivalentNodeTrace(), kotlinComparatorEquivalentNodeTrace())
    }

    @Test
    fun incidentEdgeOrderMatchesGuava() {
        assertEquals(Guava336GraphHarness.incidentEdgeOrderTrace(), kotlinIncidentEdgeOrderTrace())
    }

    private fun kotlinClosureTraces(): List<List<String>> {
        val dag = GraphBuilder.directed<String>().allowsSelfLoops(true).build<String>()
        dag.putEdge("a", "b")
        dag.putEdge("b", "c")
        dag.addNode("i")
        val cycle = GraphBuilder.directed<String>().allowsSelfLoops(true).build<String>()
        cycle.putEdge("a", "b")
        cycle.putEdge("b", "a")
        cycle.putEdge("d", "d")
        cycle.addNode("i")
        val undirected = GraphBuilder.undirected<String>().allowsSelfLoops(true).build<String>()
        undirected.putEdge("a", "b")
        undirected.addNode("i")
        return listOf(
            closure(dag, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS),
            closure(dag, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES),
            closure(cycle, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS),
            closure(cycle, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES),
            closure(undirected, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS),
            closure(undirected, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES),
        )
    }

    private fun closure(
        graph: Graph<String>,
        strategy: Graphs.TransitiveClosureSelfLoopStrategy,
    ): List<String> = edges(Graphs.transitiveClosure(graph, strategy))

    private fun kotlinAsNetworkTrace(): List<Any?> {
        val graph = GraphBuilder.directed<String>().allowsSelfLoops(true).build<String>()
        graph.putEdge("a", "b")
        graph.putEdge("a", "a")
        val network = graph.asNetwork()
        val trace = mutableListOf<Any?>(
            network.nodes().sorted(),
            edges(network.asGraph()),
            network.asGraph() == graph,
            network.allowsParallelEdges(),
            network.degree("a"),
            network.inDegree("a"),
            network.outDegree("a"),
            network.inEdges("b").map { it.toString() }.sorted(),
            network.outEdges("a").map { it.toString() }.sorted(),
            network.edgesConnecting("a", "b").map { it.toString() }.sorted(),
            network.incidentNodes(EndpointPair.ordered("a", "b")).toString(),
        )
        val heldIncoming = network.inEdges("b")
        graph.putEdge("c", "b")
        trace.add(network.inEdges("b").map { it.toString() }.sorted())
        trace.add(heldIncoming.map { it.toString() }.sorted())
        trace.add(network.nodes().sorted())
        trace.add(edges(network.asGraph()))
        trace.add(failureName { network.incidentNodes(EndpointPair.ordered("missing", "b")) })
        graph.removeNode("b")
        trace.add(failureName { heldIncoming.size })
        return trace
    }

    private fun kotlinValueGraphUtilityTrace(): List<Any?> {
        val trace = mutableListOf<Any?>()
        val undirected = ValueGraphBuilder.undirected<String, Int>().build<String, Int>()
        trace.add(undirected.putEdgeValue("a", "b", 1))
        trace.add(undirected.putEdgeValue("b", "a", 2))
        trace.add(undirected.edgeValueOrDefault("a", "b", -1))
        trace.add(undirected.edgeValueOrDefault("b", "a", -1))
        trace.add(undirected.edges().map { it.toString() }.sorted())
        trace.add(undirected.removeEdge("b", "a"))
        trace.add(undirected.hasEdgeConnecting("a", "b"))

        val graph = ValueGraphBuilder.directed<String, Int>().allowsSelfLoops(true).build<String, Int>()
        graph.putEdgeValue("a", "b", 10)
        graph.putEdgeValue("b", "c", 20)
        graph.addNode("i")
        val transpose = Graphs.transpose(graph)
        trace.add(transpose.hasEdgeConnecting("b", "a"))
        trace.add(transpose.edgeValueOrDefault("b", "a", -1))
        trace.add(transpose.inDegree("a"))
        trace.add(transpose.outDegree("a"))
        trace.add(Graphs.transpose(transpose) === graph)
        graph.putEdgeValue("c", "a", 30)
        trace.add(transpose.edgeValueOrDefault("a", "c", -1))
        trace.add(transpose.edges().map { it.toString() }.sorted())

        val induced = Graphs.inducedSubgraph(graph, listOf("a", "b", "i"))
        trace.add(induced.nodes().sorted())
        trace.add(induced.edges().map { it.toString() }.sorted())
        trace.add(induced.edgeValueOrDefault("a", "b", -1))
        val copy = Graphs.copyOf(graph)
        graph.removeEdge("a", "b")
        trace.add(copy.edges().map { it.toString() }.sorted())
        trace.add(copy.edgeValueOrDefault("a", "b", -1))
        trace.add(failureName { Graphs.inducedSubgraph(graph, listOf("missing")) })
        return trace
    }

    private fun kotlinNetworkUtilityTrace(): List<Any?> {
        val trace = mutableListOf<Any?>()
        val network = NetworkBuilder.directed<String, String>()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build<String, String>()
        network.addEdge("a", "b", "ab1")
        network.addEdge("a", "b", "ab2")
        network.addEdge("b", "c", "bc")
        network.addNode("i")
        trace.add(Graphs.hasCycle(network))
        val transpose = Graphs.transpose(network)
        trace.add(transpose.successors("b").sorted())
        trace.add(transpose.predecessors("a").sorted())
        trace.add(transpose.inEdges("a").sorted())
        trace.add(transpose.outEdges("b").sorted())
        trace.add(transpose.incidentNodes("ab1").toString())
        trace.add(transpose.edgesConnecting("b", "a").sorted())
        trace.add(Graphs.transpose(transpose) === network)
        network.addEdge("c", "a", "ca")
        trace.add(transpose.hasEdgeConnecting("a", "c"))
        trace.add(Graphs.hasCycle(network))

        val induced = Graphs.inducedSubgraph(network, listOf("a", "b", "i"))
        trace.add(induced.nodes().sorted())
        trace.add(induced.edges().sorted())
        trace.add(induced.allowsParallelEdges())
        val copy = Graphs.copyOf(network)
        network.removeEdge("ab1")
        trace.add(copy.edges().sorted())
        trace.add(copy.allowsSelfLoops())
        trace.add(failureName { Graphs.inducedSubgraph(network, listOf("missing")) })

        val parallelUndirected = NetworkBuilder.undirected<String, String>()
            .allowsParallelEdges(true)
            .build<String, String>()
        parallelUndirected.addEdge("x", "y", "xy1")
        parallelUndirected.addEdge("x", "y", "xy2")
        trace.add(Graphs.hasCycle(parallelUndirected))
        trace.add(Graphs.hasCycle(parallelUndirected.asGraph()))
        return trace
    }

    private fun kotlinOrderingTrace(): List<Any?> {
        val trace = mutableListOf<Any?>()
        val sortedMap = ElementOrder.natural<Int>().createMap<String>(3)
        sortedMap[3] = "three"
        sortedMap[1] = "one"
        sortedMap[2] = "two"
        trace.add(sortedMap.keys.toList())
        trace.add(failureName { ElementOrder.insertion<Int>().comparator() })

        val graph = GraphBuilder.directed<Int>().nodeOrder(ElementOrder.natural()).build<Int>()
        graph.addNode(3)
        graph.addNode(1)
        graph.addNode(2)
        trace.add(graph.nodes().toList())
        trace.add(graph.nodeOrder().type().name)
        val graphCopy = Graphs.copyOf(graph)
        trace.add(graphCopy.nodes().toList())
        trace.add(graphCopy.nodeOrder().type().name)

        val valueGraph = ValueGraphBuilder.directed<Int, String>()
            .nodeOrder(ElementOrder.natural())
            .build<Int, String>()
        valueGraph.addNode(3)
        valueGraph.addNode(1)
        valueGraph.addNode(2)
        trace.add(valueGraph.nodes().toList())
        trace.add(Graphs.copyOf(valueGraph).nodeOrder().type().name)

        val network = NetworkBuilder.directed<Int, String>()
            .allowsParallelEdges(true)
            .nodeOrder(ElementOrder.natural())
            .edgeOrder(ElementOrder.natural())
            .build<Int, String>()
        network.addEdge(3, 1, "z")
        network.addEdge(1, 2, "a")
        network.addEdge(2, 3, "m")
        trace.add(network.nodes().toList())
        trace.add(network.edges().toList())
        trace.add(network.nodeOrder().type().name)
        trace.add(network.edgeOrder().type().name)
        val networkCopy = Graphs.copyOf(network)
        trace.add(networkCopy.nodes().toList())
        trace.add(networkCopy.edges().toList())
        trace.add(networkCopy.nodeOrder().type().name)
        trace.add(networkCopy.edgeOrder().type().name)
        return trace
    }

    private fun kotlinBaseAccessorViewTrace(): List<Any?> {
        val trace = mutableListOf<Any?>()

        val graph = GraphBuilder.directed<String>().build<String>()
        graph.putEdge("a", "b")
        trace.add(Graphs.transpose(graph).hasEdgeConnecting("missing", "a"))
        val graphNodes = graph.nodes()
        val graphEdges = graph.edges()
        val graphSuccessors = graph.successors("a")
        graph.putEdge("b", "c")
        graph.putEdge("a", "c")
        trace.add(graphNodes.sorted())
        trace.add(graphEdges.map { it.toString() }.sorted())
        trace.add(graphSuccessors.sorted())
        graph.removeNode("a")
        trace.add(graphNodes.sorted())
        trace.add(graphEdges.map { it.toString() }.sorted())
        trace.add(failureName { graphSuccessors.size })

        val valueGraph = ValueGraphBuilder.directed<String, Int>().build<String, Int>()
        valueGraph.putEdgeValue("a", "b", 1)
        val valueNodes = valueGraph.nodes()
        val valueEdges = valueGraph.edges()
        val valueSuccessors = valueGraph.successors("a")
        valueGraph.putEdgeValue("b", "c", 2)
        valueGraph.putEdgeValue("a", "c", 3)
        trace.add(valueNodes.sorted())
        trace.add(valueEdges.map { it.toString() }.sorted())
        trace.add(valueSuccessors.sorted())
        valueGraph.removeNode("a")
        trace.add(valueNodes.sorted())
        trace.add(valueEdges.map { it.toString() }.sorted())
        trace.add(failureName { valueSuccessors.size })

        val network = NetworkBuilder.directed<String, String>()
            .allowsParallelEdges(true)
            .build<String, String>()
        network.addEdge("a", "b", "ab1")
        val networkNodes = network.nodes()
        val networkEdges = network.edges()
        val networkOutEdges = network.outEdges("a")
        val connecting = network.edgesConnecting("a", "b")
        val adjacent = network.adjacentEdges("ab1")
        network.addEdge("a", "b", "ab2")
        network.addEdge("b", "c", "bc")
        trace.add(networkNodes.sorted())
        trace.add(networkEdges.sorted())
        trace.add(networkOutEdges.sorted())
        trace.add(connecting.sorted())
        trace.add(adjacent.sorted())
        network.removeNode("a")
        trace.add(networkNodes.sorted())
        trace.add(networkEdges.sorted())
        trace.add(failureName { networkOutEdges.size })
        trace.add(failureName { connecting.size })
        trace.add(failureName { adjacent.size })
        return trace
    }

    private fun kotlinComparatorEquivalentNodeTrace(): List<Any?> {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val trace = mutableListOf<Any?>()

        val graph = GraphBuilder.directed<String>().nodeOrder(ElementOrder.sorted(byLength)).build<String>()
        trace.add(graph.addNode("a"))
        trace.add(graph.addNode("b"))
        trace.add(graph.putEdge("b", "cc"))
        trace.add(graph.nodes().toList())
        trace.add(graph.successors("a").sorted())
        trace.add(graph.edges().map { it.toString() }.sorted())
        trace.add(graph.hasEdgeConnecting("a", "cc"))
        trace.add(graph.removeNode("b"))
        trace.add(graph.nodes().toList())

        val valueGraph = ValueGraphBuilder.directed<String, Int>()
            .nodeOrder(ElementOrder.sorted(byLength))
            .build<String, Int>()
        trace.add(valueGraph.addNode("a"))
        trace.add(valueGraph.addNode("b"))
        trace.add(valueGraph.putEdgeValue("b", "cc", 1))
        trace.add(valueGraph.nodes().toList())
        trace.add(valueGraph.successors("a").sorted())
        trace.add(valueGraph.edges().map { it.toString() }.sorted())
        trace.add(valueGraph.hasEdgeConnecting("a", "cc"))
        trace.add(valueGraph.edgeValueOrDefault("a", "cc", -1))
        trace.add(valueGraph.removeNode("b"))
        trace.add(valueGraph.nodes().toList())

        val network = NetworkBuilder.directed<String, String>()
            .nodeOrder(ElementOrder.sorted(byLength))
            .build<String, String>()
        trace.add(network.addNode("a"))
        trace.add(network.addNode("b"))
        trace.add(network.addEdge("b", "cc", "edge"))
        trace.add(network.nodes().toList())
        trace.add(network.successors("a").sorted())
        trace.add(network.incidentNodes("edge").toString())
        trace.add(network.hasEdgeConnecting("a", "cc"))
        trace.add(network.addEdge("b", "cc", "edge"))
        trace.add(failureName { network.addEdge("a", "cc", "edge") })
        trace.add(network.removeNode("b"))
        trace.add(network.nodes().toList())
        return trace
    }

    private fun kotlinIncidentEdgeOrderTrace(): List<Any?> {
        val trace = mutableListOf<Any?>()
        val graph = GraphBuilder.directed<String>()
            .incidentEdgeOrder(ElementOrder.stable())
            .build<String>()
        graph.putEdge("b", "a")
        graph.putEdge("a", "c")
        graph.putEdge("d", "a")
        graph.putEdge("a", "b")
        trace.add(graph.incidentEdgeOrder().type().name)
        trace.add(graph.incidentEdges("a").map { it.toString() })
        val graphCopy = Graphs.copyOf(graph)
        trace.add(graphCopy.incidentEdgeOrder().type().name)
        trace.add(graphCopy.incidentEdges("a").map { it.toString() })
        trace.add(Graphs.transpose(graph).incidentEdgeOrder().type().name)
        val immutableGraph = ImmutableGraph.copyOf(graph)
        trace.add(immutableGraph.incidentEdgeOrder().type().name)
        trace.add(immutableGraph.incidentEdges("a").map { it.toString() })
        trace.add(ImmutableGraph.copyOf(immutableGraph) === immutableGraph)

        val valueGraph = ValueGraphBuilder.directed<String, Int>()
            .incidentEdgeOrder(ElementOrder.stable())
            .build<String, Int>()
        valueGraph.putEdgeValue("b", "a", 1)
        valueGraph.putEdgeValue("a", "c", 2)
        valueGraph.putEdgeValue("d", "a", 3)
        valueGraph.putEdgeValue("a", "b", 4)
        trace.add(valueGraph.incidentEdgeOrder().type().name)
        trace.add(valueGraph.incidentEdges("a").map { it.toString() })
        val valueCopy = Graphs.copyOf(valueGraph)
        trace.add(valueCopy.incidentEdgeOrder().type().name)
        trace.add(valueCopy.incidentEdges("a").map { it.toString() })
        trace.add(Graphs.transpose(valueGraph).incidentEdgeOrder().type().name)
        val immutableValueGraph = ImmutableValueGraph.copyOf(valueGraph)
        trace.add(immutableValueGraph.incidentEdgeOrder().type().name)
        trace.add(immutableValueGraph.incidentEdges("a").map { it.toString() })
        trace.add(ImmutableValueGraph.copyOf(immutableValueGraph) === immutableValueGraph)
        trace.add(failureName { GraphBuilder.directed<String>().incidentEdgeOrder(ElementOrder.insertion()) })
        trace.add(
            failureName {
                ValueGraphBuilder.directed<String, Int>().incidentEdgeOrder(
                    ElementOrder.sorted(Comparator.naturalOrder<String>()),
                )
            },
        )
        return trace
    }

    private fun edges(graph: Graph<String>): List<String> = graph.edges().map { it.toString() }.sorted()

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }

}
