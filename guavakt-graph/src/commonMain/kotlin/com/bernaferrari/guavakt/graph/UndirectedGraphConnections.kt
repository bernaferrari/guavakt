package com.bernaferrari.guavakt.graph

/**
 * Guava UndirectedGraphConnections — predecessor and successor share one adjacency map.
 */
open class UndirectedGraphConnections<N, V> : GraphConnections<N, V> {
    private val adj = LinkedHashMap<N, V>()

    override fun adjacentNodes(): Set<N> = adj.keys.toSet()
    override fun predecessors(): Set<N> = adjacentNodes()
    override fun successors(): Set<N> = adjacentNodes()
    override fun incidentEdges(): Set<Any?> = emptySet()
    override fun adjacentNode(edge: Any?): N = error("undirected node connections have no edge ids")
    override fun value(node: N): V? = adj[node]
    override fun removePredecessor(node: N) { adj.remove(node) }
    override fun removeSuccessor(node: N): V? = adj.remove(node)
    override fun addPredecessor(node: N, value: V) { adj[node] = value }
    override fun addSuccessor(node: N, value: V): V? = adj.put(node, value)

    companion object {
        fun <N, V> of(): UndirectedGraphConnections<N, V> = UndirectedGraphConnections()
    }
}
