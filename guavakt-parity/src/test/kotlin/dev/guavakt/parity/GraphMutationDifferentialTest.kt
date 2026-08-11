package dev.guavakt.parity

import com.google.common.graph.MutableNetwork as JavaMutableNetwork
import com.google.common.graph.MutableValueGraph as JavaMutableValueGraph
import com.google.common.graph.GraphBuilder as JavaGraphBuilder
import com.google.common.graph.ElementOrder as JavaElementOrder
import com.google.common.graph.NetworkBuilder as JavaNetworkBuilder
import com.google.common.graph.ValueGraphBuilder as JavaValueGraphBuilder
import dev.guavakt.graph.MutableNetwork
import dev.guavakt.graph.MutableValueGraph
import dev.guavakt.graph.GraphBuilder
import dev.guavakt.graph.NetworkBuilder
import dev.guavakt.graph.ValueGraphBuilder
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** Longer deterministic state traces beyond the focused graph API examples. */
class GraphMutationDifferentialTest {
    @Test
    fun directedAndUndirectedValueGraphMutationsMatchGuava() {
        val javaDirected = JavaValueGraphBuilder.directed().allowsSelfLoops(true).build<String, Int>()
        val kotlinDirected = ValueGraphBuilder.directed<String, Int>().allowsSelfLoops(true).build<String, Int>()
        fun directedStep(label: String, javaAction: () -> Any?, kotlinAction: () -> Any?) {
            assertEquals(outcome(javaAction), outcome(kotlinAction), "$label result")
            assertEquals(valueSnapshot(javaDirected), valueSnapshot(kotlinDirected), "$label state")
        }

        directedStep("add isolated", { javaDirected.addNode("z") }) { kotlinDirected.addNode("z") }
        directedStep("a to b", { javaDirected.putEdgeValue("a", "b", 1) }) { kotlinDirected.putEdgeValue("a", "b", 1) }
        directedStep("b to c", { javaDirected.putEdgeValue("b", "c", 2) }) { kotlinDirected.putEdgeValue("b", "c", 2) }
        directedStep("c to a", { javaDirected.putEdgeValue("c", "a", 3) }) { kotlinDirected.putEdgeValue("c", "a", 3) }
        directedStep("self loop", { javaDirected.putEdgeValue("a", "a", 4) }) { kotlinDirected.putEdgeValue("a", "a", 4) }
        directedStep("update", { javaDirected.putEdgeValue("a", "b", 5) }) { kotlinDirected.putEdgeValue("a", "b", 5) }
        directedStep("remove existing", { javaDirected.removeEdge("b", "c") }) { kotlinDirected.removeEdge("b", "c") }
        directedStep("restore", { javaDirected.putEdgeValue("b", "c", 6) }) { kotlinDirected.putEdgeValue("b", "c", 6) }
        directedStep("remove node", { javaDirected.removeNode("b") }) { kotlinDirected.removeNode("b") }
        directedStep("remove absent node", { javaDirected.removeNode("missing") }) { kotlinDirected.removeNode("missing") }
        directedStep("remove absent edge", { javaDirected.removeEdge("a", "b") }) { kotlinDirected.removeEdge("a", "b") }
        directedStep("new reciprocal", { javaDirected.putEdgeValue("d", "a", 7) }) { kotlinDirected.putEdgeValue("d", "a", 7) }
        directedStep("new outgoing", { javaDirected.putEdgeValue("a", "d", 8) }) { kotlinDirected.putEdgeValue("a", "d", 8) }

        val javaUndirected = JavaValueGraphBuilder.undirected().build<String, Int>()
        val kotlinUndirected = ValueGraphBuilder.undirected<String, Int>().build<String, Int>()
        fun undirectedStep(label: String, javaAction: () -> Any?, kotlinAction: () -> Any?) {
            assertEquals(outcome(javaAction), outcome(kotlinAction), "$label result")
            assertEquals(valueSnapshot(javaUndirected), valueSnapshot(kotlinUndirected), "$label state")
        }

        undirectedStep("undirected add", { javaUndirected.putEdgeValue("a", "b", 1) }) { kotlinUndirected.putEdgeValue("a", "b", 1) }
        undirectedStep("undirected reverse update", { javaUndirected.putEdgeValue("b", "a", 2) }) { kotlinUndirected.putEdgeValue("b", "a", 2) }
        undirectedStep("undirected extend", { javaUndirected.putEdgeValue("b", "c", 3) }) { kotlinUndirected.putEdgeValue("b", "c", 3) }
        undirectedStep("undirected illegal self loop", { javaUndirected.putEdgeValue("a", "a", 4) }) {
            kotlinUndirected.putEdgeValue("a", "a", 4)
        }
        undirectedStep("undirected reverse remove", { javaUndirected.removeEdge("c", "b") }) { kotlinUndirected.removeEdge("c", "b") }
        undirectedStep("undirected remove node", { javaUndirected.removeNode("a") }) { kotlinUndirected.removeNode("a") }
    }

    @Test
    fun networkMutationsAndAmbiguousLookupMatchGuava() {
        val javaNetwork = JavaNetworkBuilder.directed()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build<String, String>()
        val kotlinNetwork = NetworkBuilder.directed<String, String>()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build<String, String>()
        fun networkStep(label: String, javaAction: () -> Any?, kotlinAction: () -> Any?) {
            assertEquals(outcome(javaAction), outcome(kotlinAction), "$label result")
            assertEquals(networkSnapshot(javaNetwork), networkSnapshot(kotlinNetwork), "$label state")
        }

        networkStep("add first", { javaNetwork.addEdge("a", "b", "ab1") }) { kotlinNetwork.addEdge("a", "b", "ab1") }
        networkStep("add parallel", { javaNetwork.addEdge("a", "b", "ab2") }) { kotlinNetwork.addEdge("a", "b", "ab2") }
        networkStep("add reverse", { javaNetwork.addEdge("b", "a", "ba") }) { kotlinNetwork.addEdge("b", "a", "ba") }
        networkStep("add self loop", { javaNetwork.addEdge("a", "a", "aa") }) { kotlinNetwork.addEdge("a", "a", "aa") }
        networkStep("reuse same edge", { javaNetwork.addEdge("a", "b", "ab1") }) { kotlinNetwork.addEdge("a", "b", "ab1") }
        networkStep("reuse incompatible edge", { javaNetwork.addEdge("b", "c", "ab1") }) { kotlinNetwork.addEdge("b", "c", "ab1") }
        networkStep("remove parallel", { javaNetwork.removeEdge("ab2") }) { kotlinNetwork.removeEdge("ab2") }
        networkStep("add chain", { javaNetwork.addEdge("b", "c", "bc") }) { kotlinNetwork.addEdge("b", "c", "bc") }
        networkStep("remove a", { javaNetwork.removeNode("a") }) { kotlinNetwork.removeNode("a") }
        networkStep("remove absent edge", { javaNetwork.removeEdge("missing") }) { kotlinNetwork.removeEdge("missing") }
    }

    @Test
    fun seededValueGraphMutationTracesMatchGuava() {
        valueGraphSeeds.forEach { seed ->
            valueGraphMutationTrace(directed = true, seed = seed)
            valueGraphMutationTrace(directed = false, seed = seed xor 0xC0FFEEL)
        }
    }

    @Test
    fun seededNetworkMutationTracesMatchGuava() {
        networkSeeds.forEach { seed ->
            networkMutationTrace(directed = true, seed = seed)
            networkMutationTrace(directed = false, seed = seed xor 0xBADC0DEL)
        }
    }

    @Test
    fun undirectedComparatorEquivalentAliasesMatchGuava() {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        assertEquals(
            javaUndirectedComparatorTrace(byLength),
            kotlinUndirectedComparatorTrace(byLength),
        )
    }

    @Test
    fun stableValueGraphComparatorAliasRemovalPreservesGuavaTwoLevelViews() {
        stableValueGraphComparatorAliasTrace(directed = true)
        stableValueGraphComparatorAliasTrace(directed = false)
    }

    @Test
    fun stableGraphComparatorAliasRemovalPreservesGuavaTwoLevelViews() {
        stableGraphComparatorAliasTrace(directed = true)
        stableGraphComparatorAliasTrace(directed = false)
    }

    @Test
    fun sortedNetworkEdgeAliasesAreKotlinSafeWhereGuavaThrowsInternalNpe() {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val javaNetwork = JavaNetworkBuilder.directed()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .edgeOrder(JavaElementOrder.sorted(byLength))
            .build<String, String>()
        val kotlinNetwork = NetworkBuilder.directed<String, String>()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .edgeOrder(dev.guavakt.graph.ElementOrder.sorted(byLength))
            .build<String, String>()

        fun javaState(): List<Any?> = networkEdgeAliasState(
            nodes = javaNetwork.nodes().toList(),
            edges = javaNetwork.edges().toList(),
            incident = { node -> javaNetwork.incidentEdges(node).toList() },
            endpoints = { edge -> javaNetwork.incidentNodes(edge).toString() },
        )
        fun kotlinState(): List<Any?> = networkEdgeAliasState(
            nodes = kotlinNetwork.nodes().toList(),
            edges = kotlinNetwork.edges().toList(),
            incident = { node -> kotlinNetwork.incidentEdges(node).toList() },
            endpoints = { edge -> kotlinNetwork.incidentNodes(edge).toString() },
        )
        fun step(label: String, javaAction: () -> Any?, kotlinAction: () -> Any?) {
            assertEquals(outcome(javaAction), outcome(kotlinAction), "$label result")
            assertEquals(javaState(), kotlinState(), "$label state")
        }

        step("add canonical edge", { javaNetwork.addEdge("a", "cc", "x") }) {
            kotlinNetwork.addEdge("a", "cc", "x")
        }
        // Guava reaches an internal NPE after its sorted edge map accepts the alias but its raw
        // edge lookup cannot resolve it. GuavaKt deliberately returns the coherent no-op result.
        assertEquals("NullPointerException", outcome { javaNetwork.addEdge("a", "cc", "y") })
        assertEquals(false, outcome { kotlinNetwork.addEdge("a", "cc", "y") })
        assertEquals(javaState(), kotlinState(), "safe comparator-equivalent re-add state")
        assertEquals("NullPointerException", outcome { javaNetwork.removeEdge("y") })
        assertEquals(true, outcome { kotlinNetwork.removeEdge("y") })
        assertEquals(emptyList(), kotlinNetwork.edges().toList())
        assertEquals(emptyList(), kotlinNetwork.incidentEdges("a").toList())
    }

    private fun stableGraphComparatorAliasTrace(directed: Boolean) {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val javaGraph = (if (directed) JavaGraphBuilder.directed() else JavaGraphBuilder.undirected())
            .allowsSelfLoops(true)
            .nodeOrder(JavaElementOrder.sorted(byLength))
            .incidentEdgeOrder(JavaElementOrder.stable())
            .build<String>()
        val kotlinGraph = (if (directed) GraphBuilder.directed<String>() else GraphBuilder.undirected())
            .allowsSelfLoops(true)
            .nodeOrder(dev.guavakt.graph.ElementOrder.sorted(byLength))
            .incidentEdgeOrder(dev.guavakt.graph.ElementOrder.stable())
            .build<String>()

        fun javaState(): List<Any?> = stableAliasValueState(
            nodes = javaGraph.nodes().toList(),
            edges = javaGraph.edges().map(Any::toString),
            incident = { node -> javaGraph.incidentEdges(node).map(Any::toString) },
        )
        fun kotlinState(): List<Any?> = stableAliasValueState(
            nodes = kotlinGraph.nodes().toList(),
            edges = kotlinGraph.edges().map(Any::toString),
            incident = { node -> kotlinGraph.incidentEdges(node).map(Any::toString) },
        )
        fun step(label: String, javaAction: () -> Any?, kotlinAction: () -> Any?) {
            val context = "${if (directed) "directed" else "undirected"} $label"
            assertEquals(outcome(javaAction), outcome(kotlinAction), "$context result")
            assertEquals(javaState(), kotlinState(), "$context state")
        }

        step("first alias edge", { javaGraph.putEdge("a", "cc") }) { kotlinGraph.putEdge("a", "cc") }
        step("second raw adjacency alias edge", { javaGraph.putEdge("b", "dd") }) { kotlinGraph.putEdge("b", "dd") }
        step("remove comparator-equivalent owner", { javaGraph.removeNode("b") }) { kotlinGraph.removeNode("b") }
        step("re-add comparator-equivalent owner", { javaGraph.addNode("b") }) { kotlinGraph.addNode("b") }
    }

    private fun stableValueGraphComparatorAliasTrace(directed: Boolean) {
        val byLength = Comparator<String> { first, second -> first.length.compareTo(second.length) }
        val javaGraph = (if (directed) JavaValueGraphBuilder.directed() else JavaValueGraphBuilder.undirected())
            .allowsSelfLoops(true)
            .nodeOrder(JavaElementOrder.sorted(byLength))
            .incidentEdgeOrder(JavaElementOrder.stable())
            .build<String, Int>()
        val kotlinGraph = (if (directed) ValueGraphBuilder.directed<String, Int>() else ValueGraphBuilder.undirected())
            .allowsSelfLoops(true)
            .nodeOrder(dev.guavakt.graph.ElementOrder.sorted(byLength))
            .incidentEdgeOrder(dev.guavakt.graph.ElementOrder.stable())
            .build<String, Int>()

        fun javaState(): List<Any?> = stableAliasValueState(
            nodes = javaGraph.nodes().toList(),
            edges = javaGraph.edges().map(Any::toString),
            incident = { node -> javaGraph.incidentEdges(node).map(Any::toString) },
        )
        fun kotlinState(): List<Any?> = stableAliasValueState(
            nodes = kotlinGraph.nodes().toList(),
            edges = kotlinGraph.edges().map(Any::toString),
            incident = { node -> kotlinGraph.incidentEdges(node).map(Any::toString) },
        )
        fun step(label: String, javaAction: () -> Any?, kotlinAction: () -> Any?) {
            val context = "${if (directed) "directed" else "undirected"} $label"
            assertEquals(outcome(javaAction), outcome(kotlinAction), "$context result")
            assertEquals(javaState(), kotlinState(), "$context state")
        }

        step("first alias edge", { javaGraph.putEdgeValue("a", "cc", 1) }) {
            kotlinGraph.putEdgeValue("a", "cc", 1)
        }
        step("second raw adjacency alias edge", { javaGraph.putEdgeValue("b", "dd", 2) }) {
            kotlinGraph.putEdgeValue("b", "dd", 2)
        }
        step("remove comparator-equivalent owner", { javaGraph.removeNode("b") }) {
            kotlinGraph.removeNode("b")
        }
        step("re-add comparator-equivalent owner", { javaGraph.addNode("b") }) {
            kotlinGraph.addNode("b")
        }
    }

    private fun javaUndirectedComparatorTrace(comparator: Comparator<String>): List<Any?> {
        val graph = JavaGraphBuilder.undirected()
            .allowsSelfLoops(true)
            .nodeOrder(JavaElementOrder.sorted(comparator))
            .build<String>()
        val values = JavaValueGraphBuilder.undirected()
            .allowsSelfLoops(true)
            .nodeOrder(JavaElementOrder.sorted(comparator))
            .build<String, Int>()
        val network = JavaNetworkBuilder.undirected()
            .allowsParallelEdges(false)
            .allowsSelfLoops(true)
            .nodeOrder(JavaElementOrder.sorted(comparator))
            .build<String, String>()
        return comparatorAliasTrace(
            graphAddNode = graph::addNode,
            graphPutEdge = graph::putEdge,
            graphHasEdge = graph::hasEdgeConnecting,
            graphEdges = { graph.edges().map { it.toString() }.sorted() },
            graphNodes = { graph.nodes().toList() },
            graphAdjacent = { graph.adjacentNodes(it).toList() },
            graphRemove = graph::removeEdge,
            valuePutEdge = values::putEdgeValue,
            valueGet = values::edgeValueOrDefault,
            valueEdges = { values.edges().map { it.toString() }.sorted() },
            valueNodes = { values.nodes().toList() },
            valueRemove = values::removeEdge,
            networkAdd = network::addEdge,
            networkHasEdge = network::hasEdgeConnecting,
            networkConnecting = { first, second -> network.edgesConnecting(first, second).toList() },
            networkAdjacent = { network.adjacentNodes(it).toList() },
            networkNodes = { network.nodes().toList() },
        )
    }

    private fun kotlinUndirectedComparatorTrace(comparator: Comparator<String>): List<Any?> {
        val graph = GraphBuilder.undirected<String>()
            .allowsSelfLoops(true)
            .nodeOrder(dev.guavakt.graph.ElementOrder.sorted(comparator))
            .build<String>()
        val values = ValueGraphBuilder.undirected<String, Int>()
            .allowsSelfLoops(true)
            .nodeOrder(dev.guavakt.graph.ElementOrder.sorted(comparator))
            .build<String, Int>()
        val network = NetworkBuilder.undirected<String, String>()
            .allowsParallelEdges(false)
            .allowsSelfLoops(true)
            .nodeOrder(dev.guavakt.graph.ElementOrder.sorted(comparator))
            .build<String, String>()
        return comparatorAliasTrace(
            graphAddNode = graph::addNode,
            graphPutEdge = graph::putEdge,
            graphHasEdge = graph::hasEdgeConnecting,
            graphEdges = { graph.edges().map { it.toString() }.sorted() },
            graphNodes = { graph.nodes().toList() },
            graphAdjacent = { graph.adjacentNodes(it).toList() },
            graphRemove = graph::removeEdge,
            valuePutEdge = values::putEdgeValue,
            valueGet = values::edgeValueOrDefault,
            valueEdges = { values.edges().map { it.toString() }.sorted() },
            valueNodes = { values.nodes().toList() },
            valueRemove = values::removeEdge,
            networkAdd = network::addEdge,
            networkHasEdge = network::hasEdgeConnecting,
            networkConnecting = { first, second -> network.edgesConnecting(first, second).toList() },
            networkAdjacent = { network.adjacentNodes(it).toList() },
            networkNodes = { network.nodes().toList() },
        )
    }

    private fun comparatorAliasTrace(
        graphAddNode: (String) -> Boolean,
        graphPutEdge: (String, String) -> Boolean,
        graphHasEdge: (String, String) -> Boolean,
        graphEdges: () -> List<String>,
        graphNodes: () -> List<String>,
        graphAdjacent: (String) -> List<String>,
        graphRemove: (String, String) -> Boolean,
        valuePutEdge: (String, String, Int) -> Int?,
        valueGet: (String, String, Int) -> Int?,
        valueEdges: () -> List<String>,
        valueNodes: () -> List<String>,
        valueRemove: (String, String) -> Int?,
        networkAdd: (String, String, String) -> Boolean,
        networkHasEdge: (String, String) -> Boolean,
        networkConnecting: (String, String) -> List<String>,
        networkAdjacent: (String) -> List<String>,
        networkNodes: () -> List<String>,
    ): List<Any?> = listOf(
        outcome { graphAddNode("a") },
        outcome { graphAddNode("b") },
        outcome { graphPutEdge("b", "cc") },
        graphNodes(), graphAdjacent("a"), graphHasEdge("a", "dd"), graphEdges(),
        outcome { graphPutEdge("a", "dd") }, graphEdges(),
        outcome { graphRemove("b", "dd") }, graphNodes(), graphEdges(),

        outcome { valuePutEdge("a", "cc", 1) },
        outcome { valuePutEdge("b", "dd", 2) },
        valueNodes(), valueGet("a", "cc", -1), valueGet("b", "dd", -1), valueEdges(),
        outcome { valueRemove("a", "dd") }, valueEdges(),

        outcome { networkAdd("a", "cc", "edge-1") },
        networkNodes(), networkAdjacent("b"), networkHasEdge("a", "dd"), networkConnecting("b", "dd"),
        outcome { networkAdd("b", "dd", "edge-2") }, networkConnecting("a", "cc"),
    )

    private fun stableAliasValueState(
        nodes: List<String>,
        edges: List<String>,
        incident: (String) -> List<String>,
    ): List<Any?> = listOf(
        nodes,
        edges,
        outcome { incident("a") },
        outcome { incident("b") },
        outcome { incident("cc") },
        outcome { incident("dd") },
    )

    private fun networkEdgeAliasState(
        nodes: List<String>,
        edges: List<String>,
        incident: (String) -> List<String>,
        endpoints: (String) -> String,
    ): List<Any?> = listOf(
        nodes,
        edges,
        listOf("a", "b", "cc", "dd").map { node -> outcome { incident(node) } },
        // Guava 33.6 throws an internal NullPointerException for a non-identical
        // comparator-equivalent edge argument to incidentNodes. GuavaKt intentionally resolves
        // that alias safely, so only query the canonical edge here.
        listOf("x").map { edge -> outcome { endpoints(edge) } },
    )

    private fun valueGraphMutationTrace(directed: Boolean, seed: Long) {
        val javaGraph: JavaMutableValueGraph<String, Int> = if (directed) {
            JavaValueGraphBuilder.directed().allowsSelfLoops(false).build<String, Int>()
        } else {
            JavaValueGraphBuilder.undirected().allowsSelfLoops(false).build<String, Int>()
        }
        val kotlinGraph: MutableValueGraph<String, Int> = if (directed) {
            ValueGraphBuilder.directed<String, Int>().allowsSelfLoops(false).build<String, Int>()
        } else {
            ValueGraphBuilder.undirected<String, Int>().allowsSelfLoops(false).build<String, Int>()
        }
        val random = Random(seed)
        val universe = listOf("a", "b", "c", "d", "e")

        repeat(320) { index ->
            val first = universe[random.nextInt(universe.size)]
            val second = universe[random.nextInt(universe.size)]
            val value = random.nextInt(9)
            val operation = random.nextInt(7)
            val javaResult = outcome {
                when (operation) {
                    0 -> javaGraph.addNode(first)
                    1 -> javaGraph.putEdgeValue(first, second, value)
                    2 -> javaGraph.removeEdge(first, second)
                    3 -> javaGraph.removeNode(first)
                    4 -> javaGraph.adjacentNodes(first).sorted()
                    5 -> javaGraph.edgeValueOrDefault(first, second, -1)
                    else -> javaGraph.degree(first)
                }
            }
            val kotlinResult = outcome {
                when (operation) {
                    0 -> kotlinGraph.addNode(first)
                    1 -> kotlinGraph.putEdgeValue(first, second, value)
                    2 -> kotlinGraph.removeEdge(first, second)
                    3 -> kotlinGraph.removeNode(first)
                    4 -> kotlinGraph.adjacentNodes(first).sorted()
                    5 -> kotlinGraph.edgeValueOrDefault(first, second, -1)
                    else -> kotlinGraph.degree(first)
                }
            }
            val label = "${if (directed) "directed" else "undirected"} trace step $index, operation $operation"
            assertEquals(javaResult, kotlinResult, "$label result")
            assertEquals(valueSnapshot(javaGraph), valueSnapshot(kotlinGraph), "$label state")
        }
    }

    private fun networkMutationTrace(directed: Boolean, seed: Long) {
        val javaNetwork: JavaMutableNetwork<String, String> = if (directed) {
            JavaNetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build<String, String>()
        } else {
            JavaNetworkBuilder.undirected().allowsParallelEdges(false).allowsSelfLoops(false).build<String, String>()
        }
        val kotlinNetwork: MutableNetwork<String, String> = if (directed) {
            NetworkBuilder.directed<String, String>().allowsParallelEdges(false).allowsSelfLoops(false).build<String, String>()
        } else {
            NetworkBuilder.undirected<String, String>().allowsParallelEdges(false).allowsSelfLoops(false).build<String, String>()
        }
        val random = Random(seed)
        val universe = listOf("a", "b", "c", "d")
        val edgeUniverse = listOf("e0", "e1", "e2", "e3", "e4", "e5")

        repeat(320) { index ->
            val first = universe[random.nextInt(universe.size)]
            val second = universe[random.nextInt(universe.size)]
            val edge = edgeUniverse[random.nextInt(edgeUniverse.size)]
            val operation = random.nextInt(8)
            val javaResult = outcome {
                when (operation) {
                    0 -> javaNetwork.addNode(first)
                    1 -> javaNetwork.addEdge(first, second, edge)
                    2 -> javaNetwork.removeEdge(edge)
                    3 -> javaNetwork.removeNode(first)
                    4 -> javaNetwork.edgesConnecting(first, second).sorted()
                    5 -> javaNetwork.edgeConnectingOrNull(first, second)
                    6 -> javaNetwork.adjacentEdges(edge).sorted()
                    else -> javaNetwork.degree(first)
                }
            }
            val kotlinResult = outcome {
                when (operation) {
                    0 -> kotlinNetwork.addNode(first)
                    1 -> kotlinNetwork.addEdge(first, second, edge)
                    2 -> kotlinNetwork.removeEdge(edge)
                    3 -> kotlinNetwork.removeNode(first)
                    4 -> kotlinNetwork.edgesConnecting(first, second).sorted()
                    5 -> kotlinNetwork.edgeConnectingOrNull(first, second)
                    6 -> kotlinNetwork.adjacentEdges(edge).sorted()
                    else -> kotlinNetwork.degree(first)
                }
            }
            val label = "${if (directed) "directed" else "undirected"} trace step $index, operation $operation"
            assertEquals(javaResult, kotlinResult, "$label result")
            assertEquals(networkSnapshot(javaNetwork), networkSnapshot(kotlinNetwork), "$label state")
        }
    }

    private fun outcome(block: () -> Any?): Any? = try {
        block()
    } catch (failure: Throwable) {
        failure::class.simpleName
    }

    private fun valueSnapshot(graph: JavaMutableValueGraph<String, Int>): List<Any?> =
        valueSnapshot(
            nodes = graph.nodes(),
            edges = graph.edges().map { it.toString() },
            nodeTrace = { node ->
                listOf(
                    graph.adjacentNodes(node).sorted(), graph.predecessors(node).sorted(), graph.successors(node).sorted(),
                    graph.degree(node), graph.inDegree(node), graph.outDegree(node),
                )
            },
            edgeValue = { first, second -> graph.edgeValueOrDefault(first, second, -1) },
        )

    private fun valueSnapshot(graph: MutableValueGraph<String, Int>): List<Any?> =
        valueSnapshot(
            nodes = graph.nodes(),
            edges = graph.edges().map { it.toString() },
            nodeTrace = { node ->
                listOf(
                    graph.adjacentNodes(node).sorted(), graph.predecessors(node).sorted(), graph.successors(node).sorted(),
                    graph.degree(node), graph.inDegree(node), graph.outDegree(node),
                )
            },
            edgeValue = { first, second -> graph.edgeValueOrDefault(first, second, -1) },
        )

    private fun valueSnapshot(
        nodes: Set<String>,
        edges: Iterable<String>,
        nodeTrace: (String) -> List<Any>,
        edgeValue: (String, String) -> Int?,
    ): List<Any?> {
        val universe = listOf("a", "b", "c", "d", "z")
        return listOf(
            nodes.sorted(),
            edges.sorted(),
            nodes.sorted().map(nodeTrace),
            universe.flatMap { first -> universe.map { second -> "$first>$second=${edgeValue(first, second)}" } },
        )
    }

    private fun networkSnapshot(network: JavaMutableNetwork<String, String>): List<Any?> =
        networkSnapshot(
            nodes = network.nodes(),
            edges = network.edges(),
            incident = { edge -> network.incidentNodes(edge).toString() },
            nodeTrace = { node ->
                listOf(
                    network.adjacentNodes(node).sorted(), network.predecessors(node).sorted(), network.successors(node).sorted(),
                    network.incidentEdges(node).sorted(), network.inEdges(node).sorted(), network.outEdges(node).sorted(),
                    network.degree(node), network.inDegree(node), network.outDegree(node),
                )
            },
            connecting = { first, second -> outcome { network.edgesConnecting(first, second).sorted() } },
            single = { first, second -> outcome { network.edgeConnectingOrNull(first, second) } },
        )

    private fun networkSnapshot(network: MutableNetwork<String, String>): List<Any?> =
        networkSnapshot(
            nodes = network.nodes(),
            edges = network.edges(),
            incident = { edge -> network.incidentNodes(edge).toString() },
            nodeTrace = { node ->
                listOf(
                    network.adjacentNodes(node).sorted(), network.predecessors(node).sorted(), network.successors(node).sorted(),
                    network.incidentEdges(node).sorted(), network.inEdges(node).sorted(), network.outEdges(node).sorted(),
                    network.degree(node), network.inDegree(node), network.outDegree(node),
                )
            },
            connecting = { first, second -> outcome { network.edgesConnecting(first, second).sorted() } },
            single = { first, second -> outcome { network.edgeConnectingOrNull(first, second) } },
        )

    private fun networkSnapshot(
        nodes: Set<String>,
        edges: Set<String>,
        incident: (String) -> String,
        nodeTrace: (String) -> List<Any>,
        connecting: (String, String) -> Any?,
        single: (String, String) -> Any?,
    ): List<Any?> {
        val universe = listOf("a", "b", "c")
        return listOf(
            nodes.sorted(),
            edges.sorted(),
            edges.sorted().map { "$it=${incident(it)}" },
            nodes.sorted().map(nodeTrace),
            universe.flatMap { first -> universe.map { second -> "$first>$second=${connecting(first, second)}:${single(first, second)}" } },
        )
    }

    private companion object {
        val valueGraphSeeds = listOf(0L, 1L, 0x51A7EL, 0x1CEDL, 0x5EEDL, 0xBAD5EEDL, 0x1234_5678L, Long.MAX_VALUE)
        val networkSeeds = listOf(0L, 1L, 0x6A7EL, 0xC0DEL, 0xF00DL, 0xBEEFL, 0x1357_9BDFL, Long.MAX_VALUE)
    }
}
