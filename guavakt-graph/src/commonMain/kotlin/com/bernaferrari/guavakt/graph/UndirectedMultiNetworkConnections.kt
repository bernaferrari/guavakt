package com.bernaferrari.guavakt.graph

/** Guava UndirectedMultiNetworkConnections — undirected network adjacency (delegates to [UndirectedGraphConnections]). */
open class UndirectedMultiNetworkConnections<N, E> : UndirectedGraphConnections<N, E>() {
    companion object {
        fun <N, E> of(): UndirectedMultiNetworkConnections<N, E> = UndirectedMultiNetworkConnections()
    }
}
