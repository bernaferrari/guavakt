package dev.guavakt.graph

interface Graph<N> : SuccessorsFunction<N>, PredecessorsFunction<N> {
    fun nodes(): Set<N>
    fun nodeOrder(): ElementOrder<N> = ElementOrder.insertion()
    fun incidentEdgeOrder(): ElementOrder<N> = ElementOrder.unordered()
    fun edges(): Set<EndpointPair<N>>
    fun isDirected(): Boolean
    fun allowsSelfLoops(): Boolean
    fun adjacentNodes(node: N): Set<N>
    fun incidentEdges(node: N): Set<EndpointPair<N>>
    override fun predecessors(node: N): Set<N>
    override fun successors(node: N): Set<N>
    fun degree(node: N): Int
    fun inDegree(node: N): Int
    fun outDegree(node: N): Int
    fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean
    fun asNetwork(): Network<N, EndpointPair<N>> = GraphNetworkView(this)
}
