package dev.guavakt.graph

/**
 * Guava EdgesConnecting — view of edges connecting a node pair (or incident to a node).
 */
internal class EdgesConnecting<E>(
    private val nodeToOutEdge: Map<*, E>,
    private val targetNode: Any?,
) : AbstractSet<E>() {
    override val size: Int get() = if (targetNode in nodeToOutEdge) 1 else 0

    override fun iterator(): Iterator<E> {
        val edge = nodeToOutEdge[targetNode] ?: return emptyList<E>().iterator()
        return listOf(edge).iterator()
    }

    override fun contains(element: E): Boolean {
        val edge = nodeToOutEdge[targetNode] ?: return false
        return edge == element
    }

    fun edgeConnectingOrNull(): E? = nodeToOutEdge[targetNode]
}
