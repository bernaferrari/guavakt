package com.bernaferrari.guavakt.graph

class GraphBuilder<N> private constructor(
    private val directed: Boolean,
    private var allowsSelfLoops: Boolean = false,
    private var nodeOrder: ElementOrder<N> = ElementOrder.insertion(),
    private var incidentEdgeOrder: ElementOrder<N> = ElementOrder.unordered(),
) {
    fun allowsSelfLoops(allowsSelfLoops: Boolean): GraphBuilder<N> = apply { this.allowsSelfLoops = allowsSelfLoops }
    fun nodeOrder(order: ElementOrder<N>): GraphBuilder<N> = apply { nodeOrder = order }
    fun incidentEdgeOrder(order: ElementOrder<N>): GraphBuilder<N> = apply {
        require(order.type() == ElementOrder.Type.UNORDERED || order.type() == ElementOrder.Type.STABLE) {
            "incidentEdgeOrder only supports unordered or stable ordering"
        }
        incidentEdgeOrder = order
    }
    fun <N1 : N> build(): MutableGraph<N1> =
        StandardMutableGraph(directed, allowsSelfLoops, nodeOrder.cast(), incidentEdgeOrder.cast())

    companion object {
        fun <N> directed(): GraphBuilder<N> = GraphBuilder(true)
        fun <N> undirected(): GraphBuilder<N> = GraphBuilder(false)
        fun <N> from(graph: Graph<N>): GraphBuilder<N> =
            GraphBuilder(graph.isDirected(), graph.allowsSelfLoops(), graph.nodeOrder(), graph.incidentEdgeOrder())
    }
}
