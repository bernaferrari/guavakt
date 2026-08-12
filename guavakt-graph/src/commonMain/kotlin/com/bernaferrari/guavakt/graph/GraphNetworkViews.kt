package com.bernaferrari.guavakt.graph

/** Live edge-oriented view introduced by Guava 33.6.0. */
internal class GraphNetworkView<N>(
    private val graph: Graph<N>,
) : AbstractNetwork<N, EndpointPair<N>>() {
    override fun nodes(): Set<N> = LiveSet(values = graph::nodes)
    override fun nodeOrder(): ElementOrder<N> = graph.nodeOrder()
    override fun edges(): Set<EndpointPair<N>> = LiveSet(values = graph::edges)
    override fun asGraph(): Graph<N> = graph
    override fun isDirected(): Boolean = graph.isDirected()
    override fun allowsParallelEdges(): Boolean = false
    override fun allowsSelfLoops(): Boolean = graph.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = graph.predecessors(node)
    override fun successors(node: N): Set<N> = graph.successors(node)

    override fun incidentEdges(node: N): Set<EndpointPair<N>> = nodeSet(node) {
        buildSet {
            addAll(inEdgesNow(node))
            addAll(outEdgesNow(node))
        }
    }

    override fun inEdges(node: N): Set<EndpointPair<N>> =
        nodeSet(node) { inEdgesNow(node) }

    override fun outEdges(node: N): Set<EndpointPair<N>> =
        nodeSet(node) { outEdgesNow(node) }

    private fun inEdgesNow(node: N): Set<EndpointPair<N>> =
        if (!graph.isDirected()) incidentEdgesUndirected(node) else buildSet {
            for (predecessor in graph.predecessors(node)) {
                add(EndpointPair.ordered(predecessor, node))
            }
        }

    private fun outEdgesNow(node: N): Set<EndpointPair<N>> =
        if (!graph.isDirected()) incidentEdgesUndirected(node) else buildSet {
            for (successor in graph.successors(node)) {
                add(EndpointPair.ordered(node, successor))
            }
        }

    override fun incidentNodes(edge: EndpointPair<N>): EndpointPair<N> {
        if (!containsEdge(edge)) throw IllegalArgumentException("Edge $edge is not in this network")
        return edge
    }

    override fun edgesConnecting(nodeU: N, nodeV: N): Set<EndpointPair<N>> {
        validateNode(nodeU)
        validateNode(nodeV)
        return LiveSet(
            isValid = { nodeU in graph.nodes() && nodeV in graph.nodes() },
            invalid = { "Node pair ($nodeU, $nodeV) is no longer in this graph" },
        ) {
            if (!graph.hasEdgeConnecting(nodeU, nodeV)) emptySet()
            else setOf(
                if (graph.isDirected()) EndpointPair.ordered(nodeU, nodeV)
                else EndpointPair.unordered(nodeU, nodeV),
            )
        }
    }

    private fun incidentEdgesUndirected(node: N): Set<EndpointPair<N>> = buildSet {
        for (adjacent in graph.adjacentNodes(node)) add(EndpointPair.unordered(node, adjacent))
    }

    private fun containsEdge(edge: EndpointPair<N>): Boolean {
        if (edge.isOrdered != graph.isDirected()) return false
        return if (graph.isDirected()) {
            graph.hasEdgeConnecting(edge.nodeU, edge.nodeV)
        } else {
            graph.hasEdgeConnecting(edge.nodeU, edge.nodeV) ||
                graph.hasEdgeConnecting(edge.nodeV, edge.nodeU)
        }
    }

    private fun nodeSet(
        node: N,
        values: () -> Set<EndpointPair<N>>,
    ): Set<EndpointPair<N>> {
        validateNode(node)
        return LiveSet(
            isValid = { node in graph.nodes() },
            invalid = { "Node $node is no longer in this graph" },
            values = values,
        )
    }

    private fun validateNode(node: N) {
        if (node !in graph.nodes()) throw IllegalArgumentException("Node $node is not an element of this graph")
    }
}

internal class NetworkAsGraphView<N, E>(
    private val network: Network<N, E>,
) : AbstractGraph<N>() {
    override fun nodes(): Set<N> = LiveSet(values = network::nodes)
    override fun nodeOrder(): ElementOrder<N> = network.nodeOrder()
    override fun edges(): Set<EndpointPair<N>> = LiveSet {
        network.edges().mapTo(LinkedHashSet(), network::incidentNodes)
    }
    override fun isDirected(): Boolean = network.isDirected()
    override fun allowsSelfLoops(): Boolean = network.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = network.predecessors(node)
    override fun successors(node: N): Set<N> = network.successors(node)
}

internal class ValueGraphAsGraphView<N, V>(
    private val valueGraph: ValueGraph<N, V>,
) : AbstractGraph<N>() {
    override fun nodes(): Set<N> = LiveSet(values = valueGraph::nodes)
    override fun nodeOrder(): ElementOrder<N> = valueGraph.nodeOrder()
    override fun incidentEdgeOrder(): ElementOrder<N> = valueGraph.incidentEdgeOrder()
    override fun edges(): Set<EndpointPair<N>> = LiveSet(values = valueGraph::edges)
    override fun isDirected(): Boolean = valueGraph.isDirected()
    override fun allowsSelfLoops(): Boolean = valueGraph.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = valueGraph.predecessors(node)
    override fun successors(node: N): Set<N> = valueGraph.successors(node)
}
