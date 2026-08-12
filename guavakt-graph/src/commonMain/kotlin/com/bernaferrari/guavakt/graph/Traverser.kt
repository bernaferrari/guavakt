package com.bernaferrari.guavakt.graph

/**
 * Guava Traverser — breadth/depth-first traversal over a [SuccessorsFunction].
 * Kotlin-idiomatic: returns [Sequence]/[Iterable] of nodes without Java queues exposed.
 */
class Traverser<N> private constructor(
    private val successors: SuccessorsFunction<N>,
) {
    fun breadthFirst(startNode: N): Iterable<N> = breadthFirst(listOf(startNode))

    fun breadthFirst(startNodes: Iterable<N>): Iterable<N> = Iterable {
        object : Iterator<N> {
            private val queue = ArrayDeque<N>()
            private val seen = LinkedHashSet<N>()
            init {
                for (n in startNodes) if (seen.add(n)) queue.addLast(n)
            }
            override fun hasNext(): Boolean = queue.isNotEmpty()
            override fun next(): N {
                if (queue.isEmpty()) throw NoSuchElementException()
                val n = queue.removeFirst()
                for (s in successors.successors(n)) if (seen.add(s)) queue.addLast(s)
                return n
            }
        }
    }

    fun depthFirstPreOrder(startNode: N): Iterable<N> = depthFirstPreOrder(listOf(startNode))

    fun depthFirstPreOrder(startNodes: Iterable<N>): Iterable<N> = Iterable {
        object : Iterator<N> {
            private val stack = ArrayDeque<N>()
            private val seen = LinkedHashSet<N>()
            init {
                for (n in startNodes.toList().asReversed()) if (seen.add(n)) stack.addLast(n)
            }
            override fun hasNext(): Boolean = stack.isNotEmpty()
            override fun next(): N {
                if (stack.isEmpty()) throw NoSuchElementException()
                val n = stack.removeLast()
                for (s in successors.successors(n).toList().asReversed()) {
                    if (seen.add(s)) stack.addLast(s)
                }
                return n
            }
        }
    }

    fun depthFirstPostOrder(startNode: N): Iterable<N> = depthFirstPostOrder(listOf(startNode))

    fun depthFirstPostOrder(startNodes: Iterable<N>): Iterable<N> {
        // Materialize post-order via recursive stack simulation (stdlib-friendly list)
        val result = ArrayList<N>()
        val seen = LinkedHashSet<N>()
        fun visit(n: N) {
            if (!seen.add(n)) return
            for (s in successors.successors(n)) visit(s)
            result.add(n)
        }
        for (n in startNodes) visit(n)
        return result
    }

    companion object {
        fun <N> forGraph(graph: SuccessorsFunction<N>): Traverser<N> = Traverser(graph)
        fun <N> forTree(tree: SuccessorsFunction<N>): Traverser<N> = Traverser(tree)
    }
}
