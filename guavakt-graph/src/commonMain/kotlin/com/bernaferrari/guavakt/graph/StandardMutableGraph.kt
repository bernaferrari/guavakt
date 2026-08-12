package com.bernaferrari.guavakt.graph

class StandardMutableGraph<N>(
    private val directed: Boolean,
    private val selfLoops: Boolean,
    private val nodeIterationOrder: ElementOrder<N> = ElementOrder.insertion(),
    private val incidentIterationOrder: ElementOrder<N> = ElementOrder.unordered(),
) : MutableGraph<N> {
    private val nodeConnections: MutableMap<N, NodeConnections<N>> = nodeIterationOrder.createMap(10)
    private val stableIncidentEdges: LinkedHashSet<EndpointPair<N>>? =
        if (incidentIterationOrder.type() == ElementOrder.Type.STABLE) LinkedHashSet() else null
    override fun nodes(): Set<N> = LiveSet { nodeConnections.keys }
    override fun nodeOrder(): ElementOrder<N> = nodeIterationOrder
    override fun incidentEdgeOrder(): ElementOrder<N> = incidentIterationOrder
    override fun isDirected(): Boolean = directed
    override fun allowsSelfLoops(): Boolean = selfLoops
    override fun edges(): Set<EndpointPair<N>> = LiveSet(values = ::currentEdges)

    private fun currentEdges(): Set<EndpointPair<N>> {
        val result = LinkedHashSet<EndpointPair<N>>()
        if (directed) {
            for ((node, conn) in nodeConnections) for (successor in conn.successors)
                result.add(EndpointPair.ordered(node, successor))
        } else {
            val seen = HashSet<Pair<N, N>>()
            for ((node, conn) in nodeConnections) for (adj in conn.successors) {
                val a = node to adj; val b = adj to node
                if (a !in seen && b !in seen) { seen.add(a); result.add(EndpointPair.unordered(node, adj)) }
            }
        }
        return result
    }
    override fun adjacentNodes(node: N): Set<N> {
        val connections = checked(node)
        return nodeView(node) { connections.adjacentNodes() }
    }
    override fun incidentEdges(node: N): Set<EndpointPair<N>> {
        val connections = checked(node)
        stableIncidentEdges?.let { ordered ->
            return nodeView(node) {
                ordered.mapNotNullTo(LinkedHashSet()) { edge ->
                    stableIncidentEdge(node, connections, edge)
                }
            }
        }
        return LiveSet(
            isValid = { nodeConnections.containsKey(node) },
            invalid = { "Node $node is no longer an element of this graph." },
        ) {
            buildSet {
                if (directed) {
                    for (predecessor in connections.predecessors) add(EndpointPair.ordered(predecessor, node))
                    for (successor in connections.successors) add(EndpointPair.ordered(node, successor))
                } else {
                    for (adjacent in connections.successors) add(EndpointPair.unordered(node, adjacent))
                }
            }
        }
    }
    override fun predecessors(node: N): Set<N> {
        val connections = checked(node)
        return nodeView(node) { connections.predecessors }
    }
    override fun successors(node: N): Set<N> {
        val connections = checked(node)
        return nodeView(node) { connections.successors }
    }
    /** Guava: undirected self-loop contributes 2 to degree. */
    override fun degree(node: N): Int {
        if (directed) return predecessors(node).size + successors(node).size
        val adjacent = adjacentNodes(node)
        return adjacent.size + if (adjacent.contains(node)) 1 else 0
    }
    override fun inDegree(node: N): Int = if (directed) predecessors(node).size else degree(node)
    override fun outDegree(node: N): Int = if (directed) successors(node).size else degree(node)
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean =
        nodeConnections[nodeU]?.successors?.contains(nodeV) == true
    override fun addNode(node: N): Boolean {
        if (nodeConnections.containsKey(node)) return false
        nodeConnections[node] = NodeConnections(directed); return true
    }
    override fun putEdge(nodeU: N, nodeV: N): Boolean {
        if (!selfLoops) require(nodeU != nodeV)
        addNode(nodeU); addNode(nodeV)
        val added = nodeConnections[nodeU]!!.addSuccessor(nodeV)
        if (directed) nodeConnections[nodeV]!!.addPredecessor(nodeU)
        else nodeConnections[nodeV]!!.addSuccessor(nodeU)
        if (added) stableIncidentEdges?.add(edge(nodeU, nodeV))
        return added
    }
    override fun removeNode(node: N): Boolean {
        val connections = nodeConnections.remove(node) ?: return false
        // Guava's stable connection bookkeeping keeps raw endpoint keys. A sorted node map may
        // remove a comparator-equivalent alias while stable records under another raw alias stay
        // observable through the surviving reverse connection.
        stableIncidentEdges?.removeAll { edge -> edge.nodeU == node || edge.nodeV == node }
        for (successor in connections.successors.toList()) {
            if (directed) nodeConnections[successor]?.removePredecessor(node)
            else nodeConnections[successor]?.removeSuccessor(node)
        }
        if (directed) for (p in connections.predecessors.toList()) nodeConnections[p]?.removeSuccessor(node)
        return true
    }
    override fun removeEdge(nodeU: N, nodeV: N): Boolean {
        val connectionsU = nodeConnections[nodeU] ?: return false
        val removed = connectionsU.removeSuccessor(nodeV)
        if (removed) {
            if (directed) nodeConnections[nodeV]?.removePredecessor(nodeU)
            else nodeConnections[nodeV]?.removeSuccessor(nodeU)
            stableIncidentEdges?.removeAll { edge -> matchesEdge(edge, nodeU, nodeV) }
        }
        return removed
    }
    private fun checked(node: N): NodeConnections<N> =
        nodeConnections[node] ?: throw IllegalArgumentException("Node $node is not an element of this graph.")

    private fun sameNode(first: N, second: N): Boolean = nodeIterationOrder.equivalent(first, second)

    private fun edge(nodeU: N, nodeV: N): EndpointPair<N> =
        if (directed) EndpointPair.ordered(nodeU, nodeV) else EndpointPair.unordered(nodeU, nodeV)

    private fun matchesEdge(edge: EndpointPair<N>, nodeU: N, nodeV: N): Boolean =
        if (directed) sameNode(edge.nodeU, nodeU) && edge.nodeV == nodeV
        else (sameNode(edge.nodeU, nodeU) && sameNode(edge.nodeV, nodeV)) ||
            (sameNode(edge.nodeU, nodeV) && sameNode(edge.nodeV, nodeU))

    /** Projects stable incident records through the caller's comparator-equivalent node alias. */
    private fun stableIncidentEdge(
        node: N,
        connections: NodeConnections<N>,
        edge: EndpointPair<N>,
    ): EndpointPair<N>? {
        if (directed) {
            if (sameNode(edge.nodeU, node) && edge.nodeV in connections.successors) {
                return EndpointPair.ordered(node, edge.nodeV)
            }
            if (sameNode(edge.nodeV, node)) {
                val predecessor = connections.predecessors.firstOrNull { it == edge.nodeU }
                    ?: connections.predecessors.firstOrNull { sameNode(it, edge.nodeU) }
                if (predecessor != null) return EndpointPair.ordered(predecessor, node)
            }
            return null
        }
        if (sameNode(edge.nodeU, node) && edge.nodeV in connections.successors) {
            return EndpointPair.unordered(node, edge.nodeV)
        }
        if (sameNode(edge.nodeV, node) && edge.nodeU in connections.successors) {
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
internal class NodeConnections<N>(private val directed: Boolean) {
    val successors = LinkedHashSet<N>()
    val predecessors = LinkedHashSet<N>()
    fun adjacentNodes(): Set<N> = if (directed) successors + predecessors else successors
    fun addSuccessor(node: N): Boolean = successors.add(node)
    fun addPredecessor(node: N): Boolean = predecessors.add(node)
    fun removeSuccessor(node: N): Boolean = successors.remove(node)
    fun removePredecessor(node: N): Boolean = predecessors.remove(node)
}
