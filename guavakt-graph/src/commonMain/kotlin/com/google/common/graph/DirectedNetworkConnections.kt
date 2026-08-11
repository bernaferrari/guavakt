package dev.guavakt.graph

/** Guava DirectedNetworkConnections — directed network adjacency (delegates to [DirectedGraphConnections]). */
open class DirectedNetworkConnections<N, E> : DirectedGraphConnections<N, E>() {
    companion object {
        fun <N, E> of(): DirectedNetworkConnections<N, E> = DirectedNetworkConnections()
    }
}
