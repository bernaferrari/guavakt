package com.bernaferrari.guavakt.graph

/** Guava ImmutableNetwork — immutable view copied from a network. */
class ImmutableNetwork<N, E> private constructor(
    private val delegate: Network<N, E>,
) : AbstractNetwork<N, E>() {
    override fun nodes(): Set<N> = delegate.nodes()
    override fun edges(): Set<E> = delegate.edges()
    override fun isDirected(): Boolean = delegate.isDirected()
    override fun allowsParallelEdges(): Boolean = delegate.allowsParallelEdges()
    override fun allowsSelfLoops(): Boolean = delegate.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = delegate.predecessors(node)
    override fun successors(node: N): Set<N> = delegate.successors(node)
    override fun incidentEdges(node: N): Set<E> = delegate.incidentEdges(node)
    override fun inEdges(node: N): Set<E> = delegate.inEdges(node)
    override fun outEdges(node: N): Set<E> = delegate.outEdges(node)
    override fun incidentNodes(edge: E): EndpointPair<N> = delegate.incidentNodes(edge)
    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> = delegate.edgesConnecting(nodeU, nodeV)

    companion object {
        fun <N, E> copyOf(network: Network<N, E>): ImmutableNetwork<N, E> {
            val m = StandardMutableNetwork<N, E>(
                network.isDirected(),
                network.allowsParallelEdges(),
                network.allowsSelfLoops(),
                network.nodeOrder(),
                network.edgeOrder(),
            )
            for (n in network.nodes()) m.addNode(n)
            for (e in network.edges()) {
                val p = network.incidentNodes(e)
                m.addEdge(p.nodeU, p.nodeV, e)
            }
            return ImmutableNetwork(m)
        }
    }
}
