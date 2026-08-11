package dev.guavakt.graph

interface MutableNetwork<N, E> : Network<N, E> {
    fun addNode(node: N): Boolean
    fun addEdge(nodeU: N, nodeV: N, edge: E): Boolean
    fun removeNode(node: N): Boolean
    fun removeEdge(edge: E): Boolean
}
