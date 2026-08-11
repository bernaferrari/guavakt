package dev.guavakt.graph

/** Guava UndirectedNetworkConnections — undirected network adjacency (delegates to [UndirectedGraphConnections]). */
open class UndirectedNetworkConnections<N, E> : UndirectedGraphConnections<N, E>() {
    companion object {
        fun <N, E> of(): UndirectedNetworkConnections<N, E> = UndirectedNetworkConnections()
    }
}
