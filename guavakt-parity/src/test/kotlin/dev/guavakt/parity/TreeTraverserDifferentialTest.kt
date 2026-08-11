package dev.guavakt.parity

import com.google.common.collect.PeekingIterator as GuavaPeekingIterator
import com.google.common.collect.TreeTraverser as GuavaTreeTraverser
import dev.guavakt.collect.PeekingIterator as GuavaKtPeekingIterator
import dev.guavakt.collect.TreeTraverser as GuavaKtTreeTraverser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
class TreeTraverserDifferentialTest {
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

    @Test
    fun traversalOrdersMatchGuava() {
        val guava = GuavaTreeTraverser.using<Node> { it.children }
        val guavaKt = GuavaKtTreeTraverser.using<Node> { it.children }

        assertEquals(guava.preOrderTraversal(tree).labels(), guavaKt.preOrderTraversal(tree).labels())
        assertEquals(guava.postOrderTraversal(tree).labels(), guavaKt.postOrderTraversal(tree).labels())
        assertEquals(guava.breadthFirstTraversal(tree).labels(), guavaKt.breadthFirstTraversal(tree).labels())
    }

    @Test
    fun evaluationTimingMatchesGuava() {
        var guavaCalls = 0
        var guavaKtCalls = 0
        val guava = GuavaTreeTraverser.using<Node> { node ->
            guavaCalls++
            node.children
        }
        val guavaKt = GuavaKtTreeTraverser.using<Node> { node ->
            guavaKtCalls++
            node.children
        }

        val guavaPreorder = guava.preOrderTraversal(tree)
        val guavaKtPreorder = guavaKt.preOrderTraversal(tree)
        assertEquals(guavaCalls, guavaKtCalls)
        val guavaPreorderIterator = guavaPreorder.iterator()
        val guavaKtPreorderIterator = guavaKtPreorder.iterator()
        assertEquals(guavaCalls, guavaKtCalls)
        assertEquals(guavaPreorderIterator.next().label, guavaKtPreorderIterator.next().label)
        assertEquals(guavaCalls, guavaKtCalls)

        guavaCalls = 0
        guavaKtCalls = 0
        val guavaPostorder = guava.postOrderTraversal(tree)
        val guavaKtPostorder = guavaKt.postOrderTraversal(tree)
        assertEquals(guavaCalls, guavaKtCalls)
        val guavaPostorderIterator = guavaPostorder.iterator()
        val guavaKtPostorderIterator = guavaKtPostorder.iterator()
        assertEquals(guavaCalls, guavaKtCalls)
        assertEquals(guavaPostorderIterator.next().label, guavaKtPostorderIterator.next().label)
        assertEquals(guavaCalls, guavaKtCalls)

        guavaCalls = 0
        guavaKtCalls = 0
        val guavaBreadth = guava.breadthFirstTraversal(tree)
        val guavaKtBreadth = guavaKt.breadthFirstTraversal(tree)
        assertEquals(guavaCalls, guavaKtCalls)
        val guavaBreadthIterator = guavaBreadth.iterator() as GuavaPeekingIterator<Node>
        val guavaKtBreadthIterator = guavaKtBreadth.iterator() as GuavaKtPeekingIterator<Node>
        assertEquals(guavaCalls, guavaKtCalls)
        assertEquals(guavaBreadthIterator.peek().label, guavaKtBreadthIterator.peek().label)
        assertEquals(guavaCalls, guavaKtCalls)
        assertEquals(guavaBreadthIterator.next().label, guavaKtBreadthIterator.next().label)
        assertEquals(guavaCalls, guavaKtCalls)
    }

    @Test
    fun iteratorMutationAndExhaustionFailuresMatchGuava() {
        val guava = GuavaTreeTraverser.using<Node> { it.children }
        val guavaKt = GuavaKtTreeTraverser.using<Node> { it.children }
        val guavaTraversals =
            listOf(
                guava.preOrderTraversal(tree),
                guava.postOrderTraversal(tree),
                guava.breadthFirstTraversal(tree),
            )
        val guavaKtTraversals =
            listOf(
                guavaKt.preOrderTraversal(tree),
                guavaKt.postOrderTraversal(tree),
                guavaKt.breadthFirstTraversal(tree),
            )

        guavaTraversals.zip(guavaKtTraversals).forEach { (guavaTraversal, guavaKtTraversal) ->
            val guavaIterator = guavaTraversal.iterator()
            val guavaKtIterator = guavaKtTraversal.iterator() as MutableIterator<Node>
            guavaIterator.next()
            guavaKtIterator.next()
            assertFailsWith<UnsupportedOperationException> { guavaIterator.remove() }
            assertFailsWith<UnsupportedOperationException> { guavaKtIterator.remove() }
            while (guavaIterator.hasNext()) guavaIterator.next()
            while (guavaKtIterator.hasNext()) guavaKtIterator.next()
            assertFailsWith<NoSuchElementException> { guavaIterator.next() }
            assertFailsWith<NoSuchElementException> { guavaKtIterator.next() }
        }
    }

    private fun Iterable<Node>.labels(): String = joinToString("") { it.label.toString() }
}
