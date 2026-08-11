package dev.guavakt.graph

/** Guava DirectedMultiNetworkConnections — directed network adjacency (delegates to [DirectedGraphConnections]). */
open class DirectedMultiNetworkConnections<N, E> : DirectedGraphConnections<N, E>() {
    companion object {
        fun <N, E> of(): DirectedMultiNetworkConnections<N, E> = DirectedMultiNetworkConnections()
    }
}
