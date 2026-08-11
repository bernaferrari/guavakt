package dev.guavakt.collect

/**
 * Views values as nodes of a tree and traverses each node's ordered [children].
 *
 * This compatibility type is iterative, lazy, and available in common code. New code should use
 * `dev.guavakt.graph.Traverser.forTree`, which supports the same traversal orders. Nodes are
 * non-null by type, child order is preserved, and cycles are not detected because the input is
 * assumed to be a tree.
 */
@Deprecated(
    message = "Use dev.guavakt.graph.Traverser.forTree instead",
    level = DeprecationLevel.WARNING,
)
abstract class TreeTraverser<T : Any> {
    /** Returns the children of [root] in traversal order. */
    abstract fun children(root: T): Iterable<T>

    /** Returns a reusable lazy preorder view: node, then each child subtree. */
    fun preOrderTraversal(root: T): FluentIterable<T> =
        FluentIterable.from(Iterable { preOrderIterator(root) })

    /** Returns a reusable lazy postorder view: each child subtree, then node. */
    fun postOrderTraversal(root: T): FluentIterable<T> =
        FluentIterable.from(Iterable { postOrderIterator(root) })

    /** Returns a reusable lazy breadth-first view, level by level. */
    fun breadthFirstTraversal(root: T): FluentIterable<T> =
        FluentIterable.from(Iterable { breadthFirstIterator(root) })

    internal fun preOrderIterator(root: T): UnmodifiableIterator<T> = PreOrderIterator(root)

    internal fun postOrderIterator(root: T): UnmodifiableIterator<T> = PostOrderIterator(root)

    internal fun breadthFirstIterator(root: T): UnmodifiablePeekingIterator<T> =
        BreadthFirstIterator(root)

    private inner class PreOrderIterator(root: T) : UnmodifiableIterator<T>() {
        private val stack = ArrayDeque<Iterator<T>>().apply {
            addLast(listOf(root).iterator())
        }

        override fun hasNext(): Boolean = stack.isNotEmpty()

        override fun next(): T {
            if (stack.isEmpty()) throw NoSuchElementException()
            val iterator = stack.last()
            val result = requireNotNull(iterator.next()) { "Tree nodes must not be null" }
            if (!iterator.hasNext()) stack.removeLast()
            val childIterator = children(result).iterator()
            if (childIterator.hasNext()) stack.addLast(childIterator)
            return result
        }
    }

    private inner class PostOrderIterator(root: T) : UnmodifiableIterator<T>() {
        private val stack = ArrayDeque<PostOrderNode<T>>().apply { addLast(expand(root)) }
        private var nextValue: T? = null
        private var nextReady = false

        override fun hasNext(): Boolean {
            if (nextReady) return true
            while (stack.isNotEmpty()) {
                val top = stack.last()
                if (top.children.hasNext()) {
                    val child = requireNotNull(top.children.next()) { "Tree nodes must not be null" }
                    stack.addLast(expand(child))
                } else {
                    stack.removeLast()
                    nextValue = top.root
                    nextReady = true
                    return true
                }
            }
            return false
        }

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            @Suppress("UNCHECKED_CAST")
            val result = nextValue as T
            nextValue = null
            nextReady = false
            return result
        }

        private fun expand(root: T): PostOrderNode<T> =
            PostOrderNode(root, children(root).iterator())
    }

    private inner class BreadthFirstIterator(root: T) : UnmodifiablePeekingIterator<T>() {
        private val queue = ArrayDeque<T>().apply { addLast(root) }

        override fun hasNext(): Boolean = queue.isNotEmpty()

        override fun peek(): T = queue.firstOrNull() ?: throw NoSuchElementException()

        override fun next(): T {
            if (queue.isEmpty()) throw NoSuchElementException()
            val result = queue.removeFirst()
            children(result).forEach { child ->
                queue.addLast(requireNotNull(child) { "Tree nodes must not be null" })
            }
            return result
        }
    }

    private data class PostOrderNode<T : Any>(val root: T, val children: Iterator<T>)

    companion object {
        /** Creates a traverser backed by [nodeToChildrenFunction]. */
        @Suppress("DEPRECATION")
        fun <T : Any> using(nodeToChildrenFunction: (T) -> Iterable<T>): TreeTraverser<T> =
            object : TreeTraverser<T>() {
                override fun children(root: T): Iterable<T> = nodeToChildrenFunction(root)
            }
    }
}

/** A [PeekingIterator] whose mutation operation is always unsupported. */
internal abstract class UnmodifiablePeekingIterator<E> : UnmodifiableIterator<E>(), PeekingIterator<E>
