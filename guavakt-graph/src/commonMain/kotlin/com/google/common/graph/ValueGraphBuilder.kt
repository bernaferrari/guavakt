package dev.guavakt.graph

/** Guava ValueGraphBuilder — builds mutable value graphs. */
class ValueGraphBuilder<N, V> private constructor(
    private val directed: Boolean,
    private var allowsSelfLoops: Boolean = false,
    private var nodeOrder: ElementOrder<N> = ElementOrder.insertion(),
    private var incidentEdgeOrder: ElementOrder<N> = ElementOrder.unordered(),
) {
    fun allowsSelfLoops(allows: Boolean): ValueGraphBuilder<N, V> = apply { allowsSelfLoops = allows }
    fun nodeOrder(order: ElementOrder<N>): ValueGraphBuilder<N, V> = apply { nodeOrder = order }
    fun incidentEdgeOrder(order: ElementOrder<N>): ValueGraphBuilder<N, V> = apply {
        require(order.type() == ElementOrder.Type.UNORDERED || order.type() == ElementOrder.Type.STABLE) {
            "incidentEdgeOrder only supports unordered or stable ordering"
        }
        incidentEdgeOrder = order
    }
    fun <N1 : N, V1 : V> build(): MutableValueGraph<N1, V1> =
        StandardMutableValueGraph(directed, allowsSelfLoops, nodeOrder.cast(), incidentEdgeOrder.cast())

    companion object {
        fun <N, V> directed(): ValueGraphBuilder<N, V> = ValueGraphBuilder(true)
        fun <N, V> undirected(): ValueGraphBuilder<N, V> = ValueGraphBuilder(false)
        fun <N, V> from(graph: ValueGraph<N, V>): ValueGraphBuilder<N, V> =
            ValueGraphBuilder(
                graph.isDirected(),
                graph.allowsSelfLoops(),
                graph.nodeOrder(),
                graph.incidentEdgeOrder(),
            )
    }
}
