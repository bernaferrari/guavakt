package dev.guavakt.graph

/** Guava ForwardingNetwork — forwards all network calls to [delegate]. */
abstract class ForwardingNetwork<N, E> : AbstractNetwork<N, E>() {
    protected abstract fun delegate(): Network<N, E>
    override fun nodes(): Set<N> = delegate().nodes()
    override fun nodeOrder(): ElementOrder<N> = delegate().nodeOrder()
    override fun edges(): Set<E> = delegate().edges()
    override fun edgeOrder(): ElementOrder<E> = delegate().edgeOrder()
    override fun isDirected(): Boolean = delegate().isDirected()
    override fun allowsParallelEdges(): Boolean = delegate().allowsParallelEdges()
    override fun allowsSelfLoops(): Boolean = delegate().allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = delegate().predecessors(node)
    override fun successors(node: N): Set<N> = delegate().successors(node)
    override fun incidentEdges(node: N): Set<E> = delegate().incidentEdges(node)
    override fun inEdges(node: N): Set<E> = delegate().inEdges(node)
    override fun outEdges(node: N): Set<E> = delegate().outEdges(node)
    override fun incidentNodes(edge: E): EndpointPair<N> = delegate().incidentNodes(edge)
    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> = delegate().edgesConnecting(nodeU, nodeV)
}
