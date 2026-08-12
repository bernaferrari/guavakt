package com.bernaferrari.guavakt.graph

/**
 * Guava Graphs — algorithms on graph views.
 */
object Graphs {
    enum class TransitiveClosureSelfLoopStrategy {
        ADD_SELF_LOOPS_ALWAYS,
        ADD_SELF_LOOPS_FOR_CYCLES,
    }

    fun <N> reachableNodes(graph: SuccessorsFunction<N>, node: N): Set<N> {
        val visited = LinkedHashSet<N>()
        val queue = ArrayDeque<N>()
        queue.add(node)
        visited.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (next in graph.successors(current)) if (visited.add(next)) queue.add(next)
        }
        return visited
    }

    fun <N> hasCycle(graph: Graph<N>): Boolean {
        if (graph.isDirected()) {
            val WHITE = 0
            val GRAY = 1
            val BLACK = 2
            val color = HashMap<N, Int>()
            for (n in graph.nodes()) color[n] = WHITE
            fun visit(u: N): Boolean {
                color[u] = GRAY
                for (v in graph.successors(u)) when (color[v]) {
                    GRAY -> return true
                    WHITE -> if (visit(v)) return true
                }
                color[u] = BLACK
                return false
            }
            for (n in graph.nodes()) if (color[n] == WHITE && visit(n)) return true
            return false
        }
        val visited = HashSet<N>()
        fun visit(u: N, parent: N?): Boolean {
            visited.add(u)
            for (v in graph.adjacentNodes(u)) {
                if (v == parent) continue
                if (v in visited) return true
                if (visit(v, u)) return true
            }
            return false
        }
        for (n in graph.nodes()) if (n !in visited && visit(n, null)) return true
        return false
    }

    /**
     * Returns whether [network] contains a cycle.
     *
     * Parallel edges do not change reachability in a directed network. In an undirected network,
     * however, two parallel edges form a length-two cycle even though its [Network.asGraph] view
     * contains only one endpoint pair.
     */
    fun hasCycle(network: Network<*, *>): Boolean {
        if (!network.isDirected() && network.allowsParallelEdges() && network.edges().size > network.asGraph().edges().size) {
            return true
        }
        return hasCycle(network.asGraph())
    }

    fun <N> transpose(graph: Graph<N>): Graph<N> {
        if (!graph.isDirected()) return graph
        if (graph is TransposedGraph<N>) return graph.original
        return TransposedGraph(graph)
    }

    /** Returns a live view with every directed value-graph edge reversed. */
    fun <N, V> transpose(graph: ValueGraph<N, V>): ValueGraph<N, V> {
        if (!graph.isDirected()) return graph
        if (graph is TransposedValueGraph<N, V>) return graph.original
        return TransposedValueGraph(graph)
    }

    /** Returns a live view with every directed network edge reversed. */
    fun <N, E> transpose(network: Network<N, E>): Network<N, E> {
        if (!network.isDirected()) return network
        if (network is TransposedNetwork<N, E>) return network.original
        return TransposedNetwork(network)
    }

    @Deprecated(
        "Choose an explicit self-loop strategy; most callers want ADD_SELF_LOOPS_FOR_CYCLES",
        ReplaceWith("transitiveClosure(graph, TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS)"),
    )
    fun <N> transitiveClosure(graph: Graph<N>): ImmutableGraph<N> =
        transitiveClosure(graph, TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS)

    fun <N> transitiveClosure(
        graph: Graph<N>,
        strategy: TransitiveClosureSelfLoopStrategy,
    ): ImmutableGraph<N> {
        val result: MutableGraph<N> =
            GraphBuilder.from(graph).allowsSelfLoops(true).build()
        for (n in graph.nodes()) {
            result.addNode(n)
            val reachable = when (strategy) {
                TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS -> reachableNodes(graph, n)
                TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES ->
                    reachableNodesFrom(graph, graph.successors(n))
            }
            for (r in reachable) {
                result.putEdge(n, r)
            }
        }
        return ImmutableGraph.copyOf(result)
    }

    fun <N> inducedSubgraph(graph: Graph<N>, nodes: Iterable<N>): MutableGraph<N> {
        val result: MutableGraph<N> =
            GraphBuilder.from(graph).build()
        for (n in nodes) result.addNode(n)
        for (n in result.nodes()) {
            for (m in graph.successors(n)) {
                if (m in result.nodes()) result.putEdge(n, m)
            }
        }
        return result
    }

    /** Creates the value-preserving subgraph induced by [nodes]. */
    fun <N, V> inducedSubgraph(graph: ValueGraph<N, V>, nodes: Iterable<N>): MutableValueGraph<N, V> {
        val result: MutableValueGraph<N, V> =
            ValueGraphBuilder.from(graph).build()
        for (n in nodes) result.addNode(n)
        for (n in result.nodes()) {
            for (successor in graph.successors(n)) {
                if (successor in result.nodes()) {
                    val value = requireNotNull(graph.edgeValueOrDefault(n, successor, null))
                    result.putEdgeValue(n, successor, value)
                }
            }
        }
        return result
    }

    /** Creates the edge-object-preserving subnetwork induced by [nodes]. */
    fun <N, E> inducedSubgraph(network: Network<N, E>, nodes: Iterable<N>): MutableNetwork<N, E> {
        val result: MutableNetwork<N, E> =
            NetworkBuilder.from(network).build()
        for (n in nodes) result.addNode(n)
        for (n in result.nodes()) {
            for (edge in network.outEdges(n)) {
                val endpoints = network.incidentNodes(edge)
                val successor = endpoints.adjacentNode(n)
                if (successor in result.nodes()) result.addEdge(n, successor, edge)
            }
        }
        return result
    }

    fun <N> copyOf(graph: Graph<N>): MutableGraph<N> {
        val result: MutableGraph<N> =
            GraphBuilder.from(graph).build()
        for (n in graph.nodes()) result.addNode(n)
        for (e in graph.edges()) result.putEdge(e.source(), e.target())
        return result
    }

    /** Creates a mutable copy with the same value-graph topology and edge values. */
    fun <N, V> copyOf(graph: ValueGraph<N, V>): MutableValueGraph<N, V> {
        val result: MutableValueGraph<N, V> =
            ValueGraphBuilder.from(graph).build()
        for (n in graph.nodes()) result.addNode(n)
        for (edge in graph.edges()) {
            val value = requireNotNull(graph.edgeValueOrDefault(edge.nodeU, edge.nodeV, null))
            result.putEdgeValue(edge.nodeU, edge.nodeV, value)
        }
        return result
    }

    /** Creates a mutable copy with the same network topology and edge objects. */
    fun <N, E> copyOf(network: Network<N, E>): MutableNetwork<N, E> {
        val result: MutableNetwork<N, E> =
            NetworkBuilder.from(network).build()
        for (n in network.nodes()) result.addNode(n)
        for (edge in network.edges()) {
            val endpoints = network.incidentNodes(edge)
            result.addEdge(endpoints.nodeU, endpoints.nodeV, edge)
        }
        return result
    }

    private fun <N> reachableNodesFrom(graph: SuccessorsFunction<N>, startingNodes: Iterable<N>): Set<N> {
        val visited = LinkedHashSet<N>()
        val queue = ArrayDeque<N>()
        for (node in startingNodes) if (visited.add(node)) queue.addLast(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (next in graph.successors(current)) if (visited.add(next)) queue.addLast(next)
        }
        return visited
    }
}

/** A live read-only view: later mutations to [original] are immediately visible. */
private class TransposedGraph<N>(val original: Graph<N>) : AbstractGraph<N>() {
    override fun nodes(): Set<N> = original.nodes()
    override fun nodeOrder(): ElementOrder<N> = original.nodeOrder()
    override fun incidentEdgeOrder(): ElementOrder<N> = original.incidentEdgeOrder()
    override fun isDirected(): Boolean = true
    override fun allowsSelfLoops(): Boolean = original.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = original.successors(node)
    override fun successors(node: N): Set<N> = original.predecessors(node)
}

/** Live transpose of a directed value graph. */
private class TransposedValueGraph<N, V>(val original: ValueGraph<N, V>) : ForwardingValueGraph<N, V>() {
    override fun delegate(): ValueGraph<N, V> = original
    override fun predecessors(node: N): Set<N> = original.successors(node)
    override fun successors(node: N): Set<N> = original.predecessors(node)
    override fun inDegree(node: N): Int = original.outDegree(node)
    override fun outDegree(node: N): Int = original.inDegree(node)
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean = original.hasEdgeConnecting(nodeV, nodeU)
    override fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V?): V? =
        original.edgeValueOrDefault(nodeV, nodeU, defaultValue)
}

/** Live transpose of a directed network. */
private class TransposedNetwork<N, E>(val original: Network<N, E>) : ForwardingNetwork<N, E>() {
    override fun delegate(): Network<N, E> = original
    override fun predecessors(node: N): Set<N> = original.successors(node)
    override fun successors(node: N): Set<N> = original.predecessors(node)
    override fun inDegree(node: N): Int = original.outDegree(node)
    override fun outDegree(node: N): Int = original.inDegree(node)
    override fun inEdges(node: N): Set<E> = original.outEdges(node)
    override fun outEdges(node: N): Set<E> = original.inEdges(node)
    override fun incidentNodes(edge: E): EndpointPair<N> {
        val endpoints = original.incidentNodes(edge)
        return EndpointPair.ordered(endpoints.nodeV, endpoints.nodeU)
    }
    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> = original.edgesConnecting(nodeV, nodeU)
    override fun edgeConnectingOrNull(nodeU: N, nodeV: N): E? = original.edgeConnectingOrNull(nodeV, nodeU)
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean = original.hasEdgeConnecting(nodeV, nodeU)
}
