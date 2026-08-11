package dev.guavakt.graph

/** Guava DirectedGraphConnections — separate predecessor/successor adjacency. */
open class DirectedGraphConnections<N, V> : StandardGraphConnections<N, V>() {
    companion object {
        fun <N, V> of(): DirectedGraphConnections<N, V> = DirectedGraphConnections()
    }
}
