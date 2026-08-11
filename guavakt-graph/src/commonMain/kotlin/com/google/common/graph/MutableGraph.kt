package dev.guavakt.graph

interface MutableGraph<N> : Graph<N> {
    fun addNode(node: N): Boolean
    fun putEdge(nodeU: N, nodeV: N): Boolean
    fun removeNode(node: N): Boolean
    fun removeEdge(nodeU: N, nodeV: N): Boolean
}
