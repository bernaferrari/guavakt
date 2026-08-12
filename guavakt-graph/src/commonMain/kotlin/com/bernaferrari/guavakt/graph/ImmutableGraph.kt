package com.bernaferrari.guavakt.graph

/** Guava ImmutableGraph — immutable copy of a graph. */
class ImmutableGraph<N> private constructor(
    private val delegate: Graph<N>,
    private val frozenIncidentEdges: Map<N, List<EndpointPair<N>>>,
) : AbstractGraph<N>() {
    override fun nodes(): Set<N> = delegate.nodes()
    override fun incidentEdgeOrder(): ElementOrder<N> = ElementOrder.stable()
    override fun edges(): Set<EndpointPair<N>> = delegate.edges()
    override fun isDirected(): Boolean = delegate.isDirected()
    override fun allowsSelfLoops(): Boolean = delegate.allowsSelfLoops()
    override fun predecessors(node: N): Set<N> = delegate.predecessors(node)
    override fun successors(node: N): Set<N> = delegate.successors(node)
    override fun incidentEdges(node: N): Set<EndpointPair<N>> =
        frozenIncidentEdges[node]?.let { frozen -> LiveSet { LinkedHashSet(frozen) } } ?: delegate.incidentEdges(node)
    companion object {
        fun <N> copyOf(graph: Graph<N>): ImmutableGraph<N> {
            if (graph is ImmutableGraph<*>) {
                @Suppress("UNCHECKED_CAST")
                return graph as ImmutableGraph<N>
            }
            val m = StandardMutableGraph(
                graph.isDirected(),
                graph.allowsSelfLoops(),
                graph.nodeOrder(),
                ElementOrder.stable(),
            )
            for (n in graph.nodes()) m.addNode(n)
            for (e in graph.edges()) m.putEdge(e.nodeU, e.nodeV)
            return ImmutableGraph(m, freezeIncidentEdges(graph))
        }

        private fun <N> freezeIncidentEdges(graph: Graph<N>): Map<N, List<EndpointPair<N>>> =
            buildMap {
                for (node in graph.nodes()) {
                    put(node, graph.incidentEdges(node).toList())
                }
            }
    }
}
