package dev.guavakt.graph

/** Guava ImmutableValueGraph — immutable copy. */
class ImmutableValueGraph<N, V> private constructor(
    private val delegate: ValueGraph<N, V>,
    private val frozenIncidentEdges: Map<N, List<EndpointPair<N>>>,
) : AbstractValueGraph<N, V>() {
    override fun nodes(): Set<N> = delegate.nodes()
    override fun incidentEdgeOrder(): ElementOrder<N> = ElementOrder.stable()
    override fun isDirected(): Boolean = delegate.isDirected()
    override fun allowsSelfLoops(): Boolean = delegate.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = delegate.predecessors(node)
    override fun successors(node: N): Set<N> = delegate.successors(node)
    override fun incidentEdges(node: N): Set<EndpointPair<N>> =
        frozenIncidentEdges[node]?.let { frozen -> LiveSet { LinkedHashSet(frozen) } } ?: delegate.incidentEdges(node)
    override fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V?): V? =
        delegate.edgeValueOrDefault(nodeU, nodeV, defaultValue)
    companion object {
        fun <N, V> copyOf(graph: ValueGraph<N, V>): ImmutableValueGraph<N, V> {
            if (graph is ImmutableValueGraph<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return graph as ImmutableValueGraph<N, V>
            }
            val m = StandardMutableValueGraph<N, V>(
                graph.isDirected(),
                graph.allowsSelfLoops(),
                graph.nodeOrder(),
                ElementOrder.stable(),
            )
            for (n in graph.nodes()) m.addNode(n)
            for (e in graph.edges()) {
                val v = graph.edgeValueOrDefault(e.nodeU, e.nodeV, null)
                if (v != null) m.putEdgeValue(e.nodeU, e.nodeV, v)
            }
            return ImmutableValueGraph(m, freezeIncidentEdges(graph))
        }

        private fun <N, V> freezeIncidentEdges(graph: ValueGraph<N, V>): Map<N, List<EndpointPair<N>>> =
            buildMap {
                for (node in graph.nodes()) {
                    put(node, graph.incidentEdges(node).toList())
                }
            }
    }
}
