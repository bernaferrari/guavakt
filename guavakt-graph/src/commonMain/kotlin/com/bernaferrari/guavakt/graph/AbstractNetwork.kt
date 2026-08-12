package com.bernaferrari.guavakt.graph

/**
 * Guava AbstractNetwork — skeletal [Network]; subclasses implement storage.
 */
abstract class AbstractNetwork<N, E> : Network<N, E> {
    abstract override fun nodes(): Set<N>
    abstract override fun edges(): Set<E>
    abstract override fun isDirected(): Boolean
    abstract override fun allowsParallelEdges(): Boolean
    abstract override fun allowsSelfLoops(): Boolean
    abstract override fun predecessors(node: N): Set<N>
    abstract override fun successors(node: N): Set<N>
    abstract override fun incidentEdges(node: N): Set<E>
    abstract override fun inEdges(node: N): Set<E>
    abstract override fun outEdges(node: N): Set<E>
    abstract override fun incidentNodes(edge: E): EndpointPair<N>
    abstract override fun edgesConnecting(nodeU: N, nodeV: N): Set<E>

    override fun degree(node: N): Int = if (isDirected()) {
        inEdges(node).size + outEdges(node).size
    } else {
        incidentEdges(node).size + edgesConnecting(node, node).size
    }
    override fun inDegree(node: N): Int = if (isDirected()) inEdges(node).size else degree(node)
    override fun outDegree(node: N): Int = if (isDirected()) outEdges(node).size else degree(node)
    override fun adjacentNodes(node: N): Set<N> = LiveSet {
        buildSet {
            for (e in incidentEdges(node)) {
                val p = incidentNodes(e)
                add(if (p.nodeU == node) p.nodeV else p.nodeU)
            }
        }
    }
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean =
        nodeU in nodes() && nodeV in successors(nodeU)
    override fun edgeConnectingOrNull(nodeU: N, nodeV: N): E? {
        val connecting = edgesConnecting(nodeU, nodeV)
        return when (connecting.size) {
            0 -> null
            1 -> connecting.first()
            else -> throw IllegalArgumentException("Multiple edges connect $nodeU to $nodeV")
        }
    }
    override fun adjacentEdges(edge: E): Set<E> {
        val nodes = incidentNodes(edge)
        return LiveSet(
            isValid = { edges().contains(edge) },
            invalid = { "Edge $edge is no longer in this network." },
        ) {
            buildSet {
                addAll(incidentEdges(nodes.nodeU))
                addAll(incidentEdges(nodes.nodeV))
                remove(edge)
            }
        }
    }
}
