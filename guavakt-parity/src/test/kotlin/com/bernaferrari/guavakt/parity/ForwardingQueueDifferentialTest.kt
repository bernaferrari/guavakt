package com.bernaferrari.guavakt.parity

import com.google.common.collect.ForwardingCollection as GuavaForwardingCollection
import com.google.common.collect.ForwardingDeque as GuavaForwardingDeque
import com.google.common.collect.ForwardingQueue as GuavaForwardingQueue
import com.bernaferrari.guavakt.collect.ForwardingCollection as GuavaKtForwardingCollection
import com.bernaferrari.guavakt.collect.ForwardingDeque as GuavaKtForwardingDeque
import com.bernaferrari.guavakt.collect.ForwardingQueue as GuavaKtForwardingQueue
import java.util.ArrayDeque
import java.util.Deque
import java.util.Queue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ForwardingQueueDifferentialTest {
    @Test
    fun collectionForwardingAndStandardMethodsMatchGuava() {
        val guavaBacking = arrayListOf(1, 2, 3, 2)
        val guavaKtBacking = mutableListOf(1, 2, 3, 2)
        val guava = GuavaCollection(guavaBacking)
        val guavaKt = GuavaKtCollection(guavaKtBacking)

        assertEquals(guava.callStandardContains(2), guavaKt.callStandardContains(2))
        assertEquals(
            guava.callStandardContainsAll(listOf(1, 3)),
            guavaKt.callStandardContainsAll(listOf(1, 3)),
        )
        assertEquals(guava.callStandardRemove(2), guavaKt.callStandardRemove(2))
        assertEquals(guavaBacking.toList(), guavaKtBacking)
        assertEquals(
            guava.callStandardRemoveAll(listOf(1, 2)),
            guavaKt.callStandardRemoveAll(listOf(1, 2)),
        )
        assertEquals(guavaBacking.toList(), guavaKtBacking)
        assertEquals(
            guava.callStandardAddAll(listOf(4, 5)),
            guavaKt.callStandardAddAll(listOf(4, 5)),
        )
        assertEquals(guava.callStandardToString(), guavaKt.callStandardToString())
    }

    @Test
    fun fifoTraceAndEmptyBehaviorMatchGuava() {
        val guava = GuavaQueue(ArrayDeque())
        val guavaKt = GuavaKtQueue(mutableListOf())

        assertEquals(guava.add(1), guavaKt.add(1))
        assertEquals(guava.offer(2), guavaKt.offer(2))
        assertEquals(guava.peek(), guavaKt.peek())
        assertEquals(guava.element(), guavaKt.element())
        assertEquals(guava.poll(), guavaKt.poll())
        assertEquals(guava.remove(), guavaKt.remove())
        assertEquals(guava.peek(), guavaKt.peek())
        assertEquals(guava.poll(), guavaKt.poll())
        assertFailsWith<NoSuchElementException> { guava.element() }
        assertFailsWith<NoSuchElementException> { guavaKt.element() }
        assertFailsWith<NoSuchElementException> { guava.remove() }
        assertFailsWith<NoSuchElementException> { guavaKt.remove() }
    }

    @Test
    fun dequeTraceAndDescendingIteratorRemovalMatchGuava() {
        val guavaBacking = ArrayDeque<Int>()
        val guavaKtBacking = mutableListOf<Int>()
        val guava = GuavaDeque(guavaBacking)
        val guavaKt = GuavaKtDeque(guavaKtBacking)

        guava.addLast(1)
        guavaKt.addLast(1)
        guava.addFirst(0)
        guavaKt.addFirst(0)
        assertEquals(guava.offerLast(2), guavaKt.offerLast(2))
        assertEquals(guava.offerFirst(-1), guavaKt.offerFirst(-1))
        assertEquals(guava.peekFirst(), guavaKt.peekFirst())
        assertEquals(guava.peekLast(), guavaKt.peekLast())
        assertEquals(guava.pollFirst(), guavaKt.pollFirst())
        assertEquals(guava.pollLast(), guavaKt.pollLast())
        guava.push(9)
        guavaKt.push(9)
        assertEquals(guava.pop(), guavaKt.pop())
        guava.addAll(listOf(2, 1, 2))
        guavaKt.addAll(listOf(2, 1, 2))
        assertEquals(guava.removeFirstOccurrence(2), guavaKt.removeFirstOccurrence(2))
        assertEquals(guava.removeLastOccurrence(2), guavaKt.removeLastOccurrence(2))

        val guavaDescending = guava.descendingIterator()
        val guavaKtDescending = guavaKt.descendingIterator()
        assertEquals(guavaDescending.next(), guavaKtDescending.next())
        guavaDescending.remove()
        guavaKtDescending.remove()
        assertEquals(guava.toList(), guavaKt.toList())
        assertEquals(
            guava.descendingIterator().asSequence().toList(),
            guavaKt.descendingIterator().asSequence().toList(),
        )
    }

    private class GuavaCollection(private val backing: MutableCollection<Int>) :
        GuavaForwardingCollection<Int>() {
        override fun delegate(): MutableCollection<Int> = backing

        fun callStandardContains(element: Int) = standardContains(element)
        fun callStandardContainsAll(elements: kotlin.collections.Collection<Int>) = standardContainsAll(elements)
        fun callStandardAddAll(elements: kotlin.collections.Collection<Int>) = standardAddAll(elements)
        fun callStandardRemove(element: Int) = standardRemove(element)
        fun callStandardRemoveAll(elements: kotlin.collections.Collection<Int>) = standardRemoveAll(elements)
        fun callStandardToString() = standardToString()
    }

    private class GuavaKtCollection(private val backing: MutableCollection<Int>) :
        GuavaKtForwardingCollection<Int>() {
        override fun delegate(): MutableCollection<Int> = backing

        fun callStandardContains(element: Int) = standardContains(element)
        fun callStandardContainsAll(elements: kotlin.collections.Collection<Int>) = standardContainsAll(elements)
        fun callStandardAddAll(elements: kotlin.collections.Collection<Int>) = standardAddAll(elements)
        fun callStandardRemove(element: Int) = standardRemove(element)
        fun callStandardRemoveAll(elements: kotlin.collections.Collection<Int>) = standardRemoveAll(elements)
        fun callStandardToString() = standardToString()
    }

    private class GuavaQueue(private val backing: Queue<Int>) : GuavaForwardingQueue<Int>() {
        override fun delegate(): Queue<Int> = backing
    }

    private class GuavaKtQueue(private val backing: MutableList<Int>) : GuavaKtForwardingQueue<Int>() {
        override fun delegate(): MutableList<Int> = backing
    }

    private class GuavaDeque(private val backing: Deque<Int>) : GuavaForwardingDeque<Int>() {
        override fun delegate(): Deque<Int> = backing
    }

    private class GuavaKtDeque(private val backing: MutableList<Int>) : GuavaKtForwardingDeque<Int>() {
        override fun delegate(): MutableList<Int> = backing
    }
}
