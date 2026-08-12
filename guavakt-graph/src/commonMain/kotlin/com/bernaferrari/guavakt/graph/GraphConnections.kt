package com.bernaferrari.guavakt.graph

/**
 * Guava GraphConnections — adjacency for one node (incident edges + adjacent nodes).
 */
interface GraphConnections<N, V> {
    fun adjacentNodes(): Set<N>
    fun predecessors(): Set<N>
    fun successors(): Set<N>
    fun incidentEdges(): Set<Any?>
    fun adjacentNode(edge: Any?): N
    fun value(node: N): V?
    fun removePredecessor(node: N)
    fun removeSuccessor(node: N): V?
    fun addPredecessor(node: N, value: V)
    fun addSuccessor(node: N, value: V): V?
}

/** Default in-memory connections for a single node. */
open class StandardGraphConnections<N, V> : GraphConnections<N, V> {
    private val pred = LinkedHashMap<N, V>()
    private val succ = LinkedHashMap<N, V>()
    private val edgeToNode = LinkedHashMap<Any?, N>()

    override fun adjacentNodes(): Set<N> = buildSet { addAll(pred.keys); addAll(succ.keys) }
    override fun predecessors(): Set<N> = pred.keys.toSet()
    override fun successors(): Set<N> = succ.keys.toSet()
    override fun incidentEdges(): Set<Any?> = edgeToNode.keys.toSet()
    override fun adjacentNode(edge: Any?): N = edgeToNode[edge] ?: error("unknown edge")
    override fun value(node: N): V? = succ[node] ?: pred[node]
    override fun removePredecessor(node: N) { pred.remove(node) }
    override fun removeSuccessor(node: N): V? = succ.remove(node)
    override fun addPredecessor(node: N, value: V) { pred[node] = value }
    override fun addSuccessor(node: N, value: V): V? = succ.put(node, value)
}
