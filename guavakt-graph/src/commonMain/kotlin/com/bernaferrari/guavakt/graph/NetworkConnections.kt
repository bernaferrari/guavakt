package com.bernaferrari.guavakt.graph

/**
 * Guava NetworkConnections — per-node edge connections in a network.
 */
interface NetworkConnections<N, E> {
    fun adjacentNodes(): Set<N>
    fun predecessors(): Set<N>
    fun successors(): Set<N>
    fun incidentEdges(): Set<E>
    fun inEdges(): Set<E>
    fun outEdges(): Set<E>
    fun edgesConnecting(node: N): Set<E>
    fun adjacentNode(edge: E): N
    fun removeInEdge(edge: E, isSelfLoop: Boolean): N
    fun removeOutEdge(edge: E): N
    fun addInEdge(edge: E, node: N, isSelfLoop: Boolean)
    fun addOutEdge(edge: E, node: N): N?
}

open class StandardNetworkConnections<N, E> : NetworkConnections<N, E> {
    private val inMap = LinkedHashMap<E, N>()
    private val outMap = LinkedHashMap<E, N>()

    override fun adjacentNodes(): Set<N> = buildSet { addAll(inMap.values); addAll(outMap.values) }
    override fun predecessors(): Set<N> = inMap.values.toSet()
    override fun successors(): Set<N> = outMap.values.toSet()
    override fun incidentEdges(): Set<E> = buildSet { addAll(inMap.keys); addAll(outMap.keys) }
    override fun inEdges(): Set<E> = inMap.keys.toSet()
    override fun outEdges(): Set<E> = outMap.keys.toSet()
    override fun edgesConnecting(node: N): Set<E> =
        outMap.filterValues { it == node }.keys
    override fun adjacentNode(edge: E): N =
        outMap[edge] ?: inMap[edge] ?: error("unknown edge")
    override fun removeInEdge(edge: E, isSelfLoop: Boolean): N = inMap.remove(edge)!!
    override fun removeOutEdge(edge: E): N = outMap.remove(edge)!!
    override fun addInEdge(edge: E, node: N, isSelfLoop: Boolean) { inMap[edge] = node }
    override fun addOutEdge(edge: E, node: N): N? = outMap.put(edge, node)
}
