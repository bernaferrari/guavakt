package dev.guavakt.graph

/**
 * Mutable value graph with Guava's two-level identity model.
 *
 * The outer node map follows [nodeIterationOrder], while each node's successor/predecessor maps
 * use ordinary equality. This distinction matters for a sorted node order whose comparator treats
 * different values as equivalent: Guava resolves the owning connection through the outer map, but
 * retains raw aliases in that connection's adjacency map.
 */
open class StandardMutableValueGraph<N, V>(
    private val directed: Boolean,
    private val allowsSelfLoops: Boolean,
    private val nodeIterationOrder: ElementOrder<N> = ElementOrder.insertion(),
    private val incidentIterationOrder: ElementOrder<N> = ElementOrder.unordered(),
) : MutableValueGraph<N, V> {
    private val nodeConnections: MutableMap<N, ValueConnections<N, V>> = nodeIterationOrder.createMap(10)
    private val stableIncidentEdges: LinkedHashSet<EndpointPair<N>>? =
        if (incidentIterationOrder.type() == ElementOrder.Type.STABLE) LinkedHashSet() else null

    private fun endpoints(nodeU: N, nodeV: N): EndpointPair<N> =
        if (directed) EndpointPair.ordered(nodeU, nodeV) else EndpointPair.unordered(nodeU, nodeV)

    private fun connections(node: N): ValueConnections<N, V>? = nodeConnections[node]

    private fun checkNode(node: N): ValueConnections<N, V> =
        connections(node) ?: throw IllegalArgumentException("Node $node is not an element of this graph.")

    override fun nodes(): Set<N> = LiveSet { nodeConnections.keys }
    override fun nodeOrder(): ElementOrder<N> = nodeIterationOrder
    override fun incidentEdgeOrder(): ElementOrder<N> = incidentIterationOrder
    override fun edges(): Set<EndpointPair<N>> = LiveSet(values = ::currentEdges)
    override fun isDirected(): Boolean = directed
    override fun allowsSelfLoops(): Boolean = allowsSelfLoops

    override fun adjacentNodes(node: N): Set<N> {
        val connections = checkNode(node)
        return nodeView(node) {
            if (directed) LinkedHashSet<N>().apply {
                addAll(connections.successors.keys)
                addAll(connections.predecessors.keys)
            } else {
                connections.successors.keys
            }
        }
    }

    override fun incidentEdges(node: N): Set<EndpointPair<N>> {
        val connections = checkNode(node)
        stableIncidentEdges?.let { ordered ->
            return nodeView(node) {
                ordered.mapNotNullTo(LinkedHashSet()) { edge ->
                    stableIncidentEdge(node, connections, edge)
                }
            }
        }
        return nodeView(node) {
            buildSet {
                if (directed) {
                    for (predecessor in connections.predecessors.keys) add(EndpointPair.ordered(predecessor, node))
                    for (successor in connections.successors.keys) add(EndpointPair.ordered(node, successor))
                } else {
                    for (adjacent in connections.successors.keys) add(EndpointPair.unordered(node, adjacent))
                }
            }
        }
    }

    override fun predecessors(node: N): Set<N> {
        val connections = checkNode(node)
        return nodeView(node) { if (directed) connections.predecessors.keys else connections.successors.keys }
    }

    override fun successors(node: N): Set<N> {
        val connections = checkNode(node)
        return nodeView(node) { connections.successors.keys }
    }

    override fun degree(node: N): Int = if (directed) {
        predecessors(node).size + successors(node).size
    } else {
        adjacentNodes(node).size + if (hasEdgeConnecting(node, node)) 1 else 0
    }

    override fun inDegree(node: N): Int = if (directed) predecessors(node).size else degree(node)
    override fun outDegree(node: N): Int = if (directed) successors(node).size else degree(node)

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean =
        connections(nodeU)?.successors?.containsKey(nodeV) == true

    override fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V?): V? =
        connections(nodeU)?.successors?.get(nodeV) ?: defaultValue

    override fun addNode(node: N): Boolean {
        if (nodeConnections.containsKey(node)) return false
        nodeConnections[node] = ValueConnections()
        return true
    }

    override fun putEdgeValue(nodeU: N, nodeV: N, value: V): V? {
        if (nodeU == nodeV && !allowsSelfLoops) throw IllegalArgumentException("self-loop")
        val connectionsU = connections(nodeU) ?: ValueConnections<N, V>().also { nodeConnections[nodeU] = it }
        val previous = connectionsU.successors.put(nodeV, value)
        val connectionsV = connections(nodeV) ?: ValueConnections<N, V>().also { nodeConnections[nodeV] = it }
        if (directed) connectionsV.predecessors[nodeU] = value else connectionsV.successors[nodeU] = value
        if (previous == null) stableIncidentEdges?.add(endpoints(nodeU, nodeV))
        return previous
    }

    override fun removeNode(node: N): Boolean {
        val connections = connections(node) ?: return false
        // Stable incident edges retain Guava's raw endpoint bookkeeping. With a sorted node
        // order that considers aliases equivalent, removing one alias deliberately does not
        // erase a stable record stored under a different raw alias; affected reverse adjacency
        // maps exhibit the same two-level behavior.
        stableIncidentEdges?.removeAll { edge -> edge.nodeU == node || edge.nodeV == node }
        if (allowsSelfLoops && connections.successors.remove(node) != null && directed) {
            connections.predecessors.remove(node)
        }
        for (successor in connections.successors.keys.toList()) {
            val successorConnections = connections(successor)
                ?: throw IllegalStateException("Successor $successor is not an element of this graph.")
            if (directed) successorConnections.predecessors.remove(node) else successorConnections.successors.remove(node)
            connections.successors.remove(successor)
        }
        if (directed) {
            for (predecessor in connections.predecessors.keys.toList()) {
                val predecessorConnections = connections(predecessor)
                    ?: throw IllegalStateException("Predecessor $predecessor is not an element of this graph.")
                predecessorConnections.successors.remove(node)
                connections.predecessors.remove(predecessor)
            }
        }
        nodeConnections.remove(node)
        return true
    }

    override fun removeEdge(nodeU: N, nodeV: N): V? {
        val connectionsU = connections(nodeU) ?: return null
        val connectionsV = connections(nodeV) ?: return null
        val previous = connectionsU.successors.remove(nodeV) ?: return null
        if (directed) connectionsV.predecessors.remove(nodeU) else connectionsV.successors.remove(nodeU)
        removeStableEdge(nodeU, nodeV)
        return previous
    }

    private fun currentEdges(): Set<EndpointPair<N>> = buildSet {
        for ((node, connections) in nodeConnections) {
            for (successor in connections.successors.keys) add(endpoints(node, successor))
        }
    }

    private fun removeStableEdge(nodeU: N, nodeV: N) {
        val ordered = stableIncidentEdges ?: return
        val iterator = ordered.iterator()
        while (iterator.hasNext()) {
            val edge = iterator.next()
            val matches = if (directed) {
                nodeIterationOrder.equivalent(edge.nodeU, nodeU) && edge.nodeV == nodeV
            } else {
                (nodeIterationOrder.equivalent(edge.nodeV, nodeU) && edge.nodeU == nodeV) ||
                    (nodeIterationOrder.equivalent(edge.nodeV, nodeV) && edge.nodeU == nodeU)
            }
            if (matches) {
                iterator.remove()
                return
            }
        }
    }

    /**
     * Projects a stable edge through the node alias used for this lookup. Guava's sorted node
     * map resolves comparator-equivalent aliases to one connection object, but that connection's
     * adjacency keys remain ordinary-equality values. Consequently `incidentEdges(alias)` uses
     * [alias] in the returned endpoint pair while preserving the raw other endpoint.
     */
    private fun stableIncidentEdge(
        node: N,
        connections: ValueConnections<N, V>,
        edge: EndpointPair<N>,
    ): EndpointPair<N>? {
        if (directed) {
            if (
                nodeIterationOrder.equivalent(edge.nodeU, node) &&
                connections.successors.containsKey(edge.nodeV)
            ) {
                return EndpointPair.ordered(node, edge.nodeV)
            }
            if (nodeIterationOrder.equivalent(edge.nodeV, node)) {
                // Prefer the exact raw predecessor key. If removal of a comparator-equivalent
                // alias has already removed that key, Guava's sorted connection lookup falls
                // back to the remaining comparator-equivalent raw predecessor.
                val predecessor = connections.predecessors.keys.firstOrNull { it == edge.nodeU }
                    ?: connections.predecessors.keys.firstOrNull {
                        nodeIterationOrder.equivalent(it, edge.nodeU)
                    }
                if (predecessor != null) return EndpointPair.ordered(predecessor, node)
            }
            return null
        }
        if (
            nodeIterationOrder.equivalent(edge.nodeU, node) &&
            connections.successors.containsKey(edge.nodeV)
        ) {
            return EndpointPair.unordered(node, edge.nodeV)
        }
        if (
            nodeIterationOrder.equivalent(edge.nodeV, node) &&
            connections.successors.containsKey(edge.nodeU)
        ) {
            return EndpointPair.unordered(node, edge.nodeU)
        }
        return null
    }

    private fun <T> nodeView(node: N, values: () -> Set<T>): Set<T> = LiveSet(
        values = values,
        isValid = { nodeConnections.containsKey(node) },
        invalid = { "Node $node is no longer an element of this graph." },
    )
}

private class ValueConnections<N, V> {
    val successors = LinkedHashMap<N, V>()
    val predecessors = LinkedHashMap<N, V>()
}
