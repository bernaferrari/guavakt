package dev.guavakt.graph

interface PredecessorsFunction<N> {
    fun predecessors(node: N): Iterable<N>
}
