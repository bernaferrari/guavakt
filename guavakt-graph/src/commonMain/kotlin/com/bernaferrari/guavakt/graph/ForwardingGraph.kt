package com.bernaferrari.guavakt.graph

/** Guava ForwardingGraph — forwards to [delegate]. */
abstract class ForwardingGraph<N> : AbstractGraph<N>() {
    protected abstract fun delegate(): Graph<N>
    override fun nodes(): Set<N> = delegate().nodes()
    override fun nodeOrder(): ElementOrder<N> = delegate().nodeOrder()
    override fun incidentEdgeOrder(): ElementOrder<N> = delegate().incidentEdgeOrder()
    override fun edges(): Set<EndpointPair<N>> = delegate().edges()
    override fun isDirected(): Boolean = delegate().isDirected()
    override fun allowsSelfLoops(): Boolean = delegate().allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = delegate().predecessors(node)
    override fun successors(node: N): Set<N> = delegate().successors(node)
    override fun adjacentNodes(node: N): Set<N> = delegate().adjacentNodes(node)
    override fun incidentEdges(node: N): Set<EndpointPair<N>> = delegate().incidentEdges(node)
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean = delegate().hasEdgeConnecting(nodeU, nodeV)
}
