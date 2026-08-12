package com.bernaferrari.guavakt.graph

interface MutableValueGraph<N, V> : ValueGraph<N, V> {
    fun addNode(node: N): Boolean
    fun putEdgeValue(nodeU: N, nodeV: N, value: V): V?
    fun removeNode(node: N): Boolean
    fun removeEdge(nodeU: N, nodeV: N): V?
}
