package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals

class QueuesTest {
    @Test
    fun priorityQueue_order() {
        val q = Queues.newPriorityQueue<Int>()
        q.offer(3); q.offer(1); q.offer(2)
        assertEquals(1, q.poll())
        assertEquals(2, q.poll())
        assertEquals(3, q.poll())
    }

    @Test
    fun drain_arrayDeque() {
        val q = Queues.newArrayDeque(listOf(1, 2, 3, 4))
        val buf = ArrayList<Int>()
        assertEquals(2, Queues.drain(q, buf, 2))
        assertEquals(listOf(1, 2), buf)
        assertEquals(2, q.size)
    }
}
