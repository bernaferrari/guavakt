package dev.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForwardingQueueTest {
    @Test
    fun forwardingObjectForwardsOnlyStringRepresentation() {
        val delegate = mutableListOf(1, 2)
        val first = ObjectForwarder(delegate)
        val second = ObjectForwarder(delegate)

        assertEquals("[1, 2]", first.toString())
        assertFalse(first.equals(delegate))
        assertFalse(first == second)
    }

    @Test
    fun collectionOperationsForwardIndependently() {
        val backing = mutableListOf(1, 2, 3)
        val forwarding = CollectionForwarder(backing)

        assertTrue(forwarding.containsAll(listOf(1, 3)))
        assertTrue(forwarding.addAll(listOf(4, 5)))
        assertTrue(forwarding.removeAll(listOf(2, 5)))
        assertTrue(forwarding.retainAll(listOf(1, 4)))
        assertEquals(listOf(1, 4), backing)
        forwarding.clear()
        assertTrue(forwarding.isEmpty())
    }

    @Test
    fun standardCollectionMethodsComposeFromOverridablePrimitives() {
        val backing = mutableListOf(1, 2, 3, 2)
        val forwarding = CollectionForwarder(backing)

        assertTrue(forwarding.callStandardContains(2))
        assertTrue(forwarding.callStandardContainsAll(listOf(1, 3)))
        assertTrue(forwarding.callStandardRemove(2))
        assertEquals(listOf(1, 3, 2), backing)
        assertTrue(forwarding.callStandardRemoveAll(listOf(1, 2)))
        assertEquals(listOf(3), backing)
        assertTrue(forwarding.callStandardAddAll(listOf(4, 5)))
        assertTrue(forwarding.callStandardRetainAll(listOf(3, 5)))
        assertEquals("[3, 5]", forwarding.callStandardToString())
        forwarding.callStandardClear()
        assertTrue(forwarding.callStandardIsEmpty())
    }

    @Test
    fun queueUsesFifoSemanticsAndDistinguishesThrowingOperations() {
        val backing = mutableListOf<Int>()
        val queue = QueueForwarder(backing)

        assertTrue(queue.add(1))
        assertTrue(queue.offer(2))
        assertEquals(1, queue.peek())
        assertEquals(1, queue.element())
        assertEquals(1, queue.poll())
        assertEquals(2, queue.remove())
        assertEquals(null, queue.peek())
        assertEquals(null, queue.poll())
        assertFailsWith<NoSuchElementException> { queue.element() }
        assertFailsWith<NoSuchElementException> { queue.remove() }
    }

    @Test
    fun queueStandardMethodsTranslateCapacityAndEmptyFailures() {
        val queue = object : QueueForwarder(mutableListOf()) {
            override fun add(element: Int): Boolean = throw IllegalStateException("full")
        }

        assertFalse(queue.callStandardOffer(1))
        assertEquals(null, queue.callStandardPeek())
        assertEquals(null, queue.callStandardPoll())
    }

    @Test
    fun eachQueueOperationResolvesItsDelegateOnce() {
        var calls = 0
        val backing = mutableListOf(1)
        val queue = object : ForwardingQueue<Int>() {
            override fun delegate(): MutableList<Int> {
                calls++
                return backing
            }
        }

        assertEquals(1, queue.poll())
        assertEquals(1, calls)
        calls = 0
        assertFailsWith<NoSuchElementException> { queue.remove() }
        assertEquals(1, calls)
    }

    @Test
    fun dequeForwardsBothEndsAndDescendingIteratorMutation() {
        val backing = mutableListOf<Int>()
        val deque = DequeForwarder(backing)

        deque.addLast(1)
        deque.addFirst(0)
        assertTrue(deque.offerLast(2))
        assertTrue(deque.offerFirst(-1))
        assertEquals(-1, deque.getFirst())
        assertEquals(2, deque.getLast())
        assertEquals(-1, deque.pollFirst())
        assertEquals(2, deque.pollLast())
        deque.push(9)
        assertEquals(9, deque.pop())
        deque.addAll(listOf(2, 1, 2))
        assertTrue(deque.removeFirstOccurrence(2))
        assertTrue(deque.removeLastOccurrence(2))
        assertEquals(listOf(0, 1, 1), backing)

        val descending = deque.descendingIterator()
        assertEquals(1, descending.next())
        descending.remove()
        assertEquals(listOf(0, 1), backing)
        assertEquals(listOf(1, 0), deque.descendingIterator().asSequence().toList())
    }

    private class ObjectForwarder(private val backing: Any) : ForwardingObject() {
        override fun delegate(): Any = backing
    }

    private open class CollectionForwarder(private val backing: MutableCollection<Int>) :
        ForwardingCollection<Int>() {
        override fun delegate(): MutableCollection<Int> = backing

        fun callStandardContains(element: Int) = standardContains(element)
        fun callStandardContainsAll(elements: Collection<Int>) = standardContainsAll(elements)
        fun callStandardAddAll(elements: Collection<Int>) = standardAddAll(elements)
        fun callStandardRemove(element: Int) = standardRemove(element)
        fun callStandardRemoveAll(elements: Collection<Int>) = standardRemoveAll(elements)
        fun callStandardRetainAll(elements: Collection<Int>) = standardRetainAll(elements)
        fun callStandardClear() = standardClear()
        fun callStandardIsEmpty() = standardIsEmpty()
        fun callStandardToString() = standardToString()
    }

    private open class QueueForwarder(private val backing: MutableList<Int>) : ForwardingQueue<Int>() {
        override fun delegate(): MutableList<Int> = backing

        fun callStandardOffer(element: Int) = standardOffer(element)
        fun callStandardPeek() = standardPeek()
        fun callStandardPoll() = standardPoll()
    }

    private class DequeForwarder(private val backing: MutableList<Int>) : ForwardingDeque<Int>() {
        override fun delegate(): MutableList<Int> = backing
    }
}
