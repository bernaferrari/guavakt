package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class TreeTraverserTest {
    private data class Node(
        val label: Char,
        val children: List<Node> = emptyList(),
    )

    private val tree =
        Node(
            'h',
            listOf(
                Node('d', listOf(Node('a'), Node('b'), Node('c'))),
                Node('e'),
                Node('g', listOf(Node('f'))),
            ),
        )

    private val traverser = TreeTraverser.using<Node> { it.children }

    @Test
    fun traversalOrdersPreserveChildOrderAndViewsAreReusable() {
        val preorder = traverser.preOrderTraversal(tree)
        val postorder = traverser.postOrderTraversal(tree)
        val breadthFirst = traverser.breadthFirstTraversal(tree)

        assertEquals("hdabcegf", preorder.labels())
        assertEquals("abcdefgh", postorder.labels())
        assertEquals("hdegabcf", breadthFirst.labels())
        assertEquals("hdabcegf", preorder.labels())
        assertEquals("abcdefgh", postorder.labels())
        assertEquals("hdegabcf", breadthFirst.labels())
    }

    @Test
    fun traversalAndIteratorLazinessMatchEachTraversalContract() {
        var preorderCalls = 0
        val preorder = TreeTraverser.using<Node> { node ->
            preorderCalls++
            node.children
        }.preOrderTraversal(tree)
        assertEquals(0, preorderCalls)
        val preorderIterator = preorder.iterator()
        assertEquals(0, preorderCalls)
        assertEquals('h', preorderIterator.next().label)
        assertEquals(1, preorderCalls)

        var postorderCalls = 0
        val postorder = TreeTraverser.using<Node> { node ->
            postorderCalls++
            node.children
        }.postOrderTraversal(tree)
        assertEquals(0, postorderCalls)
        val postorderIterator = postorder.iterator()
        assertEquals(1, postorderCalls)
        assertEquals('a', postorderIterator.next().label)
        assertEquals(3, postorderCalls)

        var breadthCalls = 0
        val breadth = TreeTraverser.using<Node> { node ->
            breadthCalls++
            node.children
        }.breadthFirstTraversal(tree)
        assertEquals(0, breadthCalls)
        val breadthIterator = breadth.iterator() as PeekingIterator<Node>
        assertEquals(0, breadthCalls)
        assertSame(tree, breadthIterator.peek())
        assertEquals(0, breadthCalls)
        assertSame(tree, breadthIterator.next())
        assertEquals(1, breadthCalls)
    }

    @Test
    fun breadthFirstIteratorPeeksWithoutConsuming() {
        val iterator = traverser.breadthFirstTraversal(tree).iterator() as PeekingIterator<Node>

        assertSame(tree, iterator.peek())
        assertSame(tree, iterator.peek())
        assertSame(tree, iterator.next())
        assertEquals('d', iterator.peek().label)
        assertEquals("degabcf", iterator.asSequence().joinToString("") { it.label.toString() })
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.peek() }
    }

    @Test
    fun iteratorsAreUnmodifiableAndExhaustionThrows() {
        listOf(
            traverser.preOrderTraversal(tree),
            traverser.postOrderTraversal(tree),
            traverser.breadthFirstTraversal(tree),
        ).forEach { traversal ->
            val iterator = traversal.iterator() as MutableIterator<Node>
            assertTrue(iterator.hasNext())
            iterator.next()
            assertFailsWith<UnsupportedOperationException> { iterator.remove() }
            while (iterator.hasNext()) iterator.next()
            assertFailsWith<NoSuchElementException> { iterator.next() }
        }
    }

    @Test
    fun deepTreesDoNotUseTheCallStack() {
        val last = 20_000
        val deep = TreeTraverser.using<Int> { value ->
            if (value < last) listOf(value + 1) else emptyList()
        }

        val preorder = deep.preOrderTraversal(0).iterator()
        assertEquals(0, preorder.next())
        var preorderLast = 0
        var preorderCount = 1
        while (preorder.hasNext()) {
            preorderLast = preorder.next()
            preorderCount++
        }
        assertEquals(last, preorderLast)
        assertEquals(last + 1, preorderCount)

        val postorder = deep.postOrderTraversal(0).iterator()
        assertEquals(last, postorder.next())
        var postorderLast = last
        var postorderCount = 1
        while (postorder.hasNext()) {
            postorderLast = postorder.next()
            postorderCount++
        }
        assertEquals(0, postorderLast)
        assertEquals(last + 1, postorderCount)
    }

    @Test
    fun forgedNullChildrenAreRejectedAtTheTraversalBoundary() {
        @Suppress("UNCHECKED_CAST")
        val nullChild = listOf<Node?>(null) as Iterable<Node>
        val bad = TreeTraverser.using<Node> { nullChild }

        val preorder = bad.preOrderTraversal(Node('x')).iterator()
        assertEquals('x', preorder.next().label)
        assertFailsWith<IllegalArgumentException> { preorder.next() }

        val breadth = bad.breadthFirstTraversal(Node('x')).iterator()
        assertFailsWith<IllegalArgumentException> { breadth.next() }
    }

    private fun Iterable<Node>.labels(): String = joinToString("") { it.label.toString() }
}
