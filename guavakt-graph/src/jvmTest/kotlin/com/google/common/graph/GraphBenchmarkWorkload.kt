package dev.guavakt.graph

/** JVM-only deterministic workload batches called by the JMH graph harness. */
class GraphBenchmarkWorkload {
    private var mutableGraph: MutableGraph<Int> = newSparseGraph()
    private val denseDag: Graph<Int> = newDenseDag()

    fun prepareMutation() {
        mutableGraph = newSparseGraph()
    }

    /** Covers node/edge construction without traversal or algorithm work. */
    fun buildSparseGraphBatch(): Int = newSparseGraph().edges().size

    /** Covers successor-set iteration and queue bookkeeping in the public reachability algorithm. */
    fun reachableDenseDagBatch(): Int = Graphs.reachableNodes(denseDag, 0).size

    /** Covers an alternating write/delete trace from a stable, non-empty directed graph. */
    fun mutateEdgeBatch(): Int {
        repeat(MUTATIONS_PER_BATCH) { step ->
            val source = (step * 37) % NODE_COUNT
            val target = (source + 1 + (step % 13)) % NODE_COUNT
            if ((step and 1) == 0) mutableGraph.putEdge(source, target)
            else mutableGraph.removeEdge(source, target)
        }
        return mutableGraph.edges().size
    }

    private fun newSparseGraph(): MutableGraph<Int> = GraphBuilder.directed<Int>().build<Int>().also { graph ->
        repeat(NODE_COUNT) { graph.addNode(it) }
        for (node in 0 until NODE_COUNT - 4) {
            graph.putEdge(node, node + 1)
            graph.putEdge(node, node + 4)
        }
    }

    private fun newDenseDag(): Graph<Int> = GraphBuilder.directed<Int>().build<Int>().also { graph ->
        repeat(NODE_COUNT) { graph.addNode(it) }
        for (source in 0 until NODE_COUNT) {
            for (offset in 1..DAG_FAN_OUT) {
                val target = source + offset
                if (target < NODE_COUNT) graph.putEdge(source, target)
            }
        }
    }

    private companion object {
        const val NODE_COUNT = 1_024
        const val DAG_FAN_OUT = 8
        const val MUTATIONS_PER_BATCH = 1_024
    }
}
