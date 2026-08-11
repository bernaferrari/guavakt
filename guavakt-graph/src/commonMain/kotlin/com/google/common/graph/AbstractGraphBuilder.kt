package dev.guavakt.graph

/**
 * Guava AbstractGraphBuilder — shared builder options for graphs/networks/value graphs.
 */
abstract class AbstractGraphBuilder<N>(
    val directed: Boolean,
) {
    var allowsSelfLoops: Boolean = false
        protected set
    var nodeOrder: ElementOrder<N> = ElementOrder.insertion()
        protected set

    fun allowsSelfLoops(allowsSelfLoops: Boolean): AbstractGraphBuilder<N> = apply {
        this.allowsSelfLoops = allowsSelfLoops
    }
}
