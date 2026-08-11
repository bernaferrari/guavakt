package dev.guavakt.graph

/**
 * Guava MultiEdgesConnecting — view of multiple edges connecting nodes in a multigraph.
 */
internal class MultiEdgesConnecting<E>(
    private val outEdges: Map<*, Collection<E>>,
    private val targetNode: Any?,
) : AbstractSet<E>() {
    private fun edges(): Collection<E> = outEdges[targetNode] ?: emptyList()

    override val size: Int get() = edges().size

    override fun iterator(): Iterator<E> = edges().iterator()

    override fun contains(element: E): Boolean = element in edges()
}
