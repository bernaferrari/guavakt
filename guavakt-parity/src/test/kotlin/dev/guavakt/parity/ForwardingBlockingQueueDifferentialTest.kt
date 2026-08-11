package dev.guavakt.parity

import com.google.common.collect.ForwardingBlockingDeque as GuavaCollectBlockingDeque
import com.google.common.util.concurrent.ForwardingBlockingDeque as GuavaBlockingDeque
import com.google.common.util.concurrent.ForwardingBlockingQueue as GuavaBlockingQueue
import dev.guavakt.collect.ForwardingBlockingDeque as GuavaKtCollectBlockingDeque
import dev.guavakt.util.concurrent.ForwardingBlockingDeque as GuavaKtBlockingDeque
import dev.guavakt.util.concurrent.ForwardingBlockingQueue as GuavaKtBlockingQueue
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingDeque
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
class ForwardingBlockingQueueDifferentialTest {
    @Test
    fun blockingQueueCapacityTimeoutAndDrainTraceMatchesGuava() {
        val guava = GuavaQueue(ArrayBlockingQueue(2))
        val guavaKt = GuavaKtQueue(ArrayBlockingQueue(2))

        assertEquals(queueTrace(guava), queueTrace(guavaKt))
    }

    @Test
    fun concurrentBlockingDequeTraceMatchesGuava() {
        val guava = GuavaDeque(LinkedBlockingDeque(4))
        val guavaKt = GuavaKtDeque(LinkedBlockingDeque(4))

        assertEquals(dequeTrace(guava), dequeTrace(guavaKt))
    }

    @Test
    fun deprecatedCollectBlockingDequeTraceMatchesGuava() {
        val guava = GuavaCollectDeque(LinkedBlockingDeque(4))
        val guavaKt = GuavaKtCollectDeque(LinkedBlockingDeque(4))

        assertEquals(dequeTrace(guava), dequeTrace(guavaKt))
    }

    @Test
    fun interruptedTakeIsForwardedRatherThanSwallowed() {
        val guava = GuavaQueue(ArrayBlockingQueue(1))
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> { guava.take() }
        } finally {
            Thread.interrupted()
        }

        val guavaKt = GuavaKtQueue(ArrayBlockingQueue(1))
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> { guavaKt.take() }
        } finally {
            Thread.interrupted()
        }
    }

    private fun queueTrace(queue: BlockingQueue<Int>): List<Any?> {
        val trace = mutableListOf<Any?>()
        trace += queue.remainingCapacity()
        queue.put(1)
        trace += queue.offer(2, 0, TimeUnit.MILLISECONDS)
        trace += queue.offer(3, 0, TimeUnit.MILLISECONDS)
        trace += queue.remainingCapacity()
        trace += queue.poll(0, TimeUnit.MILLISECONDS)
        trace += queue.take()
        trace += queue.poll()
        queue.addAll(listOf(4, 5))
        val drained = mutableListOf<Int>()
        trace += queue.drainTo(drained, 1)
        trace.add(drained.toList())
        trace.add(queue.toList())
        trace += queue.toString()
        return trace
    }

    private fun dequeTrace(deque: BlockingDeque<Int>): List<Any?> {
        val trace = mutableListOf<Any?>()
        deque.putFirst(2)
        deque.putLast(3)
        trace += deque.offerFirst(1, 0, TimeUnit.MILLISECONDS)
        trace += deque.offerLast(4, 0, TimeUnit.MILLISECONDS)
        trace += deque.offerLast(5, 0, TimeUnit.MILLISECONDS)
        trace += deque.remainingCapacity()
        trace += deque.takeFirst()
        trace += deque.takeLast()
        trace += deque.pollFirst(0, TimeUnit.MILLISECONDS)
        trace += deque.pollLast(0, TimeUnit.MILLISECONDS)
        trace += deque.pollFirst(0, TimeUnit.MILLISECONDS)
        deque.addAll(listOf(7, 8, 9))
        val drained = mutableListOf<Int>()
        trace += deque.drainTo(drained, 2)
        trace.add(drained.toList())
        trace.add(deque.toList())
        return trace
    }

    private class GuavaQueue(private val backing: BlockingQueue<Int>) : GuavaBlockingQueue<Int>() {
        override fun delegate(): BlockingQueue<Int> = backing
    }

    private class GuavaKtQueue(private val backing: BlockingQueue<Int>) : GuavaKtBlockingQueue<Int>() {
        override fun delegate(): BlockingQueue<Int> = backing
    }

    private class GuavaDeque(private val backing: BlockingDeque<Int>) : GuavaBlockingDeque<Int>() {
        override fun delegate(): BlockingDeque<Int> = backing
    }

    private class GuavaKtDeque(private val backing: BlockingDeque<Int>) : GuavaKtBlockingDeque<Int>() {
        override fun delegate(): BlockingDeque<Int> = backing
    }

    private class GuavaCollectDeque(private val backing: BlockingDeque<Int>) :
        GuavaCollectBlockingDeque<Int>() {
        override fun delegate(): BlockingDeque<Int> = backing
    }

    private class GuavaKtCollectDeque(private val backing: BlockingDeque<Int>) :
        GuavaKtCollectBlockingDeque<Int>() {
        override fun delegate(): BlockingDeque<Int> = backing
    }
}
