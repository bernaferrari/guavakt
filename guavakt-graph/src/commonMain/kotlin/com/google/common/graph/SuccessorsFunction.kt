package dev.guavakt.graph

interface SuccessorsFunction<N> {
    fun successors(node: N): Iterable<N>
}
