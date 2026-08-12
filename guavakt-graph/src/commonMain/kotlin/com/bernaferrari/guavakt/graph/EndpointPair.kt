package com.bernaferrari.guavakt.graph

@ConsistentCopyVisibility
data class EndpointPair<N> internal constructor(
    val nodeU: N,
    val nodeV: N,
    val isOrdered: Boolean,
) : Iterable<N> {
    fun source(): N {
        if (!isOrdered) throw UnsupportedOperationException("Cannot call source() on an unordered EndpointPair")
        return nodeU
    }

    fun target(): N {
        if (!isOrdered) throw UnsupportedOperationException("Cannot call target() on an unordered EndpointPair")
        return nodeV
    }

    fun adjacentNode(node: N): N = when (node) {
        nodeU -> nodeV
        nodeV -> nodeU
        else -> throw IllegalArgumentException("EndpointPair $this does not contain node $node")
    }

    override operator fun iterator(): Iterator<N> = listOf(nodeU, nodeV).iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EndpointPair<*> || isOrdered != other.isOrdered) return false
        return if (isOrdered) {
            nodeU == other.nodeU && nodeV == other.nodeV
        } else {
            (nodeU == other.nodeU && nodeV == other.nodeV) ||
                (nodeU == other.nodeV && nodeV == other.nodeU)
        }
    }

    override fun hashCode(): Int = if (isOrdered) {
        31 * (31 + nodeU.hashCode()) + nodeV.hashCode()
    } else {
        nodeU.hashCode() + nodeV.hashCode()
    }

    override fun toString(): String =
        if (isOrdered) "<$nodeU -> $nodeV>" else "[$nodeU, $nodeV]"

    companion object {
        fun <N> ordered(source: N, target: N): EndpointPair<N> = EndpointPair(source, target, true)
        /** Guava intentionally swaps these to discourage relying on order for an unordered pair. */
        fun <N> unordered(nodeU: N, nodeV: N): EndpointPair<N> = EndpointPair(nodeV, nodeU, false)
    }
}
