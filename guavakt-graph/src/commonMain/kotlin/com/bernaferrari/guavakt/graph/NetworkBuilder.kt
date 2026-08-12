package com.bernaferrari.guavakt.graph

class NetworkBuilder<N, E> private constructor(
    private val directed: Boolean,
    private var allowsParallelEdges: Boolean = false,
    private var allowsSelfLoops: Boolean = false,
    private var nodeOrder: ElementOrder<N> = ElementOrder.insertion(),
    private var edgeOrder: ElementOrder<E> = ElementOrder.insertion(),
) {
    fun allowsParallelEdges(allows: Boolean): NetworkBuilder<N, E> = apply { allowsParallelEdges = allows }
    fun allowsSelfLoops(allows: Boolean): NetworkBuilder<N, E> = apply { allowsSelfLoops = allows }
    fun nodeOrder(order: ElementOrder<N>): NetworkBuilder<N, E> = apply { nodeOrder = order }
    fun edgeOrder(order: ElementOrder<E>): NetworkBuilder<N, E> = apply { edgeOrder = order }
    fun <N1 : N, E1 : E> build(): MutableNetwork<N1, E1> =
        StandardMutableNetwork(directed, allowsParallelEdges, allowsSelfLoops, nodeOrder.cast(), edgeOrder.cast())
    companion object {
        fun <N, E> directed(): NetworkBuilder<N, E> = NetworkBuilder(true)
        fun <N, E> undirected(): NetworkBuilder<N, E> = NetworkBuilder(false)
        fun <N, E> from(network: Network<N, E>): NetworkBuilder<N, E> =
            NetworkBuilder(
                network.isDirected(),
                network.allowsParallelEdges(),
                network.allowsSelfLoops(),
                network.nodeOrder(),
                network.edgeOrder(),
            )
    }
}
