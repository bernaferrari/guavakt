package com.bernaferrari.guavakt.graph

/** Guava StandardNetwork — mutable network implementation. */
class StandardNetwork<N, E>(
    directed: Boolean,
    allowsParallelEdges: Boolean,
    allowsSelfLoops: Boolean,
) : StandardMutableNetwork<N, E>(directed, allowsParallelEdges, allowsSelfLoops) {
    // Guava-named surface available via MutableNetwork (nodes/edges/addEdge/...)
    override fun nodes(): Set<N> = super.nodes()
    override fun edges(): Set<E> = super.edges()
}
