package com.bernaferrari.guavakt.graph

/**
 * Guava-shaped mutable network: explicit edge objects connecting node pairs.
 */
open class StandardMutableNetwork<N, E>(
    private val directed: Boolean,
    private val allowsParallelEdges: Boolean,
    private val allowsSelfLoops: Boolean,
    private val nodeIterationOrder: ElementOrder<N> = ElementOrder.insertion(),
    private val edgeIterationOrder: ElementOrder<E> = ElementOrder.insertion(),
) : MutableNetwork<N, E> {
    private val nodesByOrder: MutableMap<N, Unit> = nodeIterationOrder.createMap(10)
    private val edgeToNodes: MutableMap<E, EndpointPair<N>> = edgeIterationOrder.createMap(10)
    private val outEdges: MutableMap<N, LinkedHashSet<E>> = nodeIterationOrder.createMap(10)
    private val inEdges: MutableMap<N, LinkedHashSet<E>> = nodeIterationOrder.createMap(10)

    override fun nodes(): Set<N> = LiveSet { nodesByOrder.keys }
    override fun nodeOrder(): ElementOrder<N> = nodeIterationOrder
    override fun edges(): Set<E> = LiveSet { edgeToNodes.keys }
    override fun edgeOrder(): ElementOrder<E> = edgeIterationOrder
    override fun isDirected(): Boolean = directed
    override fun allowsParallelEdges(): Boolean = allowsParallelEdges
    override fun allowsSelfLoops(): Boolean = allowsSelfLoops

    private fun sameNode(first: N, second: N): Boolean = nodeIterationOrder.equivalent(first, second)

    override fun adjacentNodes(node: N): Set<N> {
        checkNode(node)
        return nodeView(node) {
            buildSet {
                for (e in incidentEdges(node)) {
                    val pair = edgeToNodes[e]!!
                    if (sameNode(pair.nodeU, node)) add(pair.nodeV) else add(pair.nodeU)
                }
            }
        }
    }

    override fun predecessors(node: N): Set<N> {
        checkNode(node)
        if (!directed) return adjacentNodes(node)
        return nodeView(node) {
            buildSet {
                for (e in inEdges[node].orEmpty()) {
                    val pair = edgeToNodes[e]!!
                    add(if (sameNode(pair.nodeV, node)) pair.nodeU else pair.nodeV)
                }
            }
        }
    }

    override fun successors(node: N): Set<N> {
        checkNode(node)
        if (!directed) return adjacentNodes(node)
        return nodeView(node) {
            buildSet {
                for (e in outEdges[node].orEmpty()) {
                    val pair = edgeToNodes[e]!!
                    add(if (sameNode(pair.nodeU, node)) pair.nodeV else pair.nodeU)
                }
            }
        }
    }

    override fun incidentEdges(node: N): Set<E> {
        checkNode(node)
        return nodeView(node) {
            buildSet {
                addAll(outEdges[node].orEmpty())
                addAll(inEdges[node].orEmpty())
            }
        }
    }

    override fun inEdges(node: N): Set<E> {
        checkNode(node)
        if (!directed) return incidentEdges(node)
        return nodeView(node) { inEdges[node].orEmpty() }
    }

    override fun outEdges(node: N): Set<E> {
        checkNode(node)
        if (!directed) return incidentEdges(node)
        return nodeView(node) { outEdges[node].orEmpty() }
    }

    override fun degree(node: N): Int = if (directed) {
        inEdges(node).size + outEdges(node).size
    } else {
        incidentEdges(node).size + edgesConnecting(node, node).size
    }
    override fun inDegree(node: N): Int = if (directed) inEdges(node).size else degree(node)
    override fun outDegree(node: N): Int = if (directed) outEdges(node).size else degree(node)

    override fun incidentNodes(edge: E): EndpointPair<N> =
        edgeToNodes[edge] ?: throw IllegalArgumentException("Edge $edge is not an element of this network.")

    override fun adjacentEdges(edge: E): Set<E> {
        val nodes = incidentNodes(edge)
        return edgeView(edge) {
            buildSet {
                addAll(incidentEdges(nodes.nodeU))
                addAll(incidentEdges(nodes.nodeV))
                remove(edge)
            }
        }
    }

    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> {
        checkNode(nodeU)
        checkNode(nodeV)
        return nodePairView(nodeU, nodeV) {
            buildSet {
                for (e in outEdges[nodeU].orEmpty()) {
                    val p = edgeToNodes[e]!!
                    if ((p.nodeU == nodeU && p.nodeV == nodeV) ||
                        (!directed && p.nodeU == nodeV && p.nodeV == nodeU)
                    ) add(e)
                }
                if (!directed) {
                    for (e in outEdges[nodeV].orEmpty()) {
                        val p = edgeToNodes[e]!!
                        if (p.nodeU == nodeV && p.nodeV == nodeU) add(e)
                    }
                }
            }
        }
    }

    override fun edgeConnectingOrNull(nodeU: N, nodeV: N): E? {
        val connecting = edgesConnecting(nodeU, nodeV)
        return when (connecting.size) {
            0 -> null
            1 -> connecting.first()
            else -> throw IllegalArgumentException("Multiple edges connect $nodeU to $nodeV")
        }
    }

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean =
        nodeU in nodesByOrder && nodeV in nodesByOrder && successors(nodeU).contains(nodeV)

    override fun addNode(node: N): Boolean {
        if (nodesByOrder.put(node, Unit) == null) {
            outEdges.getOrPut(node) { LinkedHashSet() }
            inEdges.getOrPut(node) { LinkedHashSet() }
            return true
        }
        return false
    }

    override fun addEdge(nodeU: N, nodeV: N, edge: E): Boolean {
        if (edgeToNodes.containsKey(edge)) {
            val existing = incidentNodes(edge)
            val requested = if (directed) EndpointPair.ordered(nodeU, nodeV) else EndpointPair.unordered(nodeU, nodeV)
            require(existing == requested) {
                "Edge $edge already connects $existing and cannot be reused for $requested."
            }
            return false
        }
        if (nodeU == nodeV && !allowsSelfLoops) {
            throw IllegalArgumentException("self-loops not allowed")
        }
        if (!allowsParallelEdges && nodeU in nodesByOrder && nodeV in nodesByOrder && hasEdgeConnecting(nodeU, nodeV)) {
            throw IllegalArgumentException("parallel edges not allowed")
        }
        addNode(nodeU)
        addNode(nodeV)
        val pair = if (directed) EndpointPair.ordered(nodeU, nodeV) else EndpointPair.unordered(nodeU, nodeV)
        edgeToNodes[edge] = pair
        outEdges.getOrPut(nodeU) { LinkedHashSet() }.add(edge)
        inEdges.getOrPut(nodeV) { LinkedHashSet() }.add(edge)
        if (!directed && nodeU != nodeV) {
            outEdges.getOrPut(nodeV) { LinkedHashSet() }.add(edge)
            inEdges.getOrPut(nodeU) { LinkedHashSet() }.add(edge)
        }
        return true
    }

    override fun removeNode(node: N): Boolean {
        if (!nodesByOrder.containsKey(node)) return false
        for (e in incidentEdges(node).toList()) removeEdge(e)
        nodesByOrder.remove(node)
        outEdges.remove(node)
        inEdges.remove(node)
        return true
    }

    override fun removeEdge(edge: E): Boolean {
        val pair = edgeToNodes[edge] ?: return false
        // The sorted edge map can resolve a comparator-equivalent alias, whereas per-node edge
        // sets retain the original raw edge object. Locate that stored key before removing the
        // map entry so a successful alias removal cannot leave ghost incident edges behind.
        val storedEdge = edgeToNodes.keys.firstOrNull { it == edge }
            ?: edgeToNodes.keys.firstOrNull { edgeIterationOrder.equivalent(it, edge) }
            ?: throw IllegalStateException("Missing stored edge for $edge")
        edgeToNodes.remove(edge)
        outEdges[pair.nodeU]?.remove(storedEdge)
        inEdges[pair.nodeV]?.remove(storedEdge)
        if (!directed) {
            outEdges[pair.nodeV]?.remove(storedEdge)
            inEdges[pair.nodeU]?.remove(storedEdge)
        }
        return true
    }

    private fun checkNode(node: N) {
        require(node in nodesByOrder) { "Node $node is not an element of this network." }
    }

    private fun <T> nodeView(node: N, values: () -> Set<T>): Set<T> = LiveSet(
        values = values,
        isValid = { nodesByOrder.containsKey(node) },
        invalid = { "Node $node is no longer an element of this network." },
    )

    private fun <T> nodePairView(nodeU: N, nodeV: N, values: () -> Set<T>): Set<T> = LiveSet(
        values = values,
        isValid = { nodesByOrder.containsKey(nodeU) && nodesByOrder.containsKey(nodeV) },
        invalid = { "Node pair ($nodeU, $nodeV) is no longer in this network." },
    )

    private fun <T> edgeView(edge: E, values: () -> Set<T>): Set<T> = LiveSet(
        values = values,
        isValid = { edgeToNodes.containsKey(edge) },
        invalid = { "Edge $edge is no longer in this network." },
    )
}
