package com.bernaferrari.guavakt.graph

/** Guava AbstractGraph — skeletal [Graph]. */
abstract class AbstractGraph<N> : Graph<N> {
    abstract override fun nodes(): Set<N>
    abstract override fun isDirected(): Boolean
    abstract override fun allowsSelfLoops(): Boolean
    abstract override fun predecessors(node: N): Set<N>
    abstract override fun successors(node: N): Set<N>
    override fun edges(): Set<EndpointPair<N>> = LiveSet {
        buildSet {
            for (n in nodes()) for (s in successors(n)) {
                add(if (isDirected()) EndpointPair.ordered(n, s) else EndpointPair.unordered(n, s))
            }
        }
    }
    override fun adjacentNodes(node: N): Set<N> = LiveSet {
        buildSet {
            addAll(predecessors(node)); addAll(successors(node))
        }
    }
    override fun incidentEdges(node: N): Set<EndpointPair<N>> = LiveSet {
        buildSet {
            if (isDirected()) {
                for (predecessor in predecessors(node)) add(EndpointPair.ordered(predecessor, node))
                for (successor in successors(node)) add(EndpointPair.ordered(node, successor))
            } else {
                for (adjacent in adjacentNodes(node)) add(EndpointPair.unordered(node, adjacent))
            }
        }
    }
    override fun degree(node: N): Int = if (isDirected()) {
        inDegree(node) + outDegree(node)
    } else {
        adjacentNodes(node).size + if (hasEdgeConnecting(node, node)) 1 else 0
    }
    override fun inDegree(node: N): Int = predecessors(node).size
    override fun outDegree(node: N): Int = successors(node).size
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean =
        nodeU in nodes() && nodeV in successors(nodeU)
}
