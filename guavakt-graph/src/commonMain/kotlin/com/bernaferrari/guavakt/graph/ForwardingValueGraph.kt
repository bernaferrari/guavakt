package com.bernaferrari.guavakt.graph

/** Guava ForwardingValueGraph — forwards to [delegate]. */
abstract class ForwardingValueGraph<N, V> : AbstractValueGraph<N, V>() {
    protected abstract fun delegate(): ValueGraph<N, V>
    override fun nodes(): Set<N> = delegate().nodes()
    override fun nodeOrder(): ElementOrder<N> = delegate().nodeOrder()
    override fun incidentEdgeOrder(): ElementOrder<N> = delegate().incidentEdgeOrder()
    override fun isDirected(): Boolean = delegate().isDirected()
    override fun allowsSelfLoops(): Boolean = delegate().allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = delegate().predecessors(node)
    override fun successors(node: N): Set<N> = delegate().successors(node)
    override fun incidentEdges(node: N): Set<EndpointPair<N>> = delegate().incidentEdges(node)
    override fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V?): V? =
        delegate().edgeValueOrDefault(nodeU, nodeV, defaultValue)
}
