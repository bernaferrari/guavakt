package dev.guavakt.graph

/** Guava Network — graph with explicit edge objects. */
interface Network<N, E> : SuccessorsFunction<N>, PredecessorsFunction<N> {
    fun nodes(): Set<N>
    fun nodeOrder(): ElementOrder<N> = ElementOrder.insertion()
    fun edges(): Set<E>
    fun edgeOrder(): ElementOrder<E> = ElementOrder.insertion()
    fun isDirected(): Boolean
    fun allowsParallelEdges(): Boolean
    fun allowsSelfLoops(): Boolean
    fun adjacentNodes(node: N): Set<N>
    override fun predecessors(node: N): Set<N>
    override fun successors(node: N): Set<N>
    fun incidentEdges(node: N): Set<E>
    fun inEdges(node: N): Set<E>
    fun outEdges(node: N): Set<E>
    fun degree(node: N): Int
    fun inDegree(node: N): Int
    fun outDegree(node: N): Int
    fun incidentNodes(edge: E): EndpointPair<N>
    fun adjacentEdges(edge: E): Set<E>
    fun edgesConnecting(nodeU: N, nodeV: N): Set<E>
    fun edgeConnectingOrNull(nodeU: N, nodeV: N): E?
    fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean
    fun asGraph(): Graph<N> = NetworkAsGraphView(this)
}
