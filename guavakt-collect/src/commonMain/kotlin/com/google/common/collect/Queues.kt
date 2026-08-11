package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Guava Queues — queue factories and drain helpers (KMP; no java.util.concurrent queues).
 */
object Queues {
    fun <E> newArrayDeque(): ArrayDeque<E> = ArrayDeque()
    fun <E> newArrayDeque(elements: Iterable<E>): ArrayDeque<E> =
        ArrayDeque<E>().also { it.addAll(elements.toList()) }

    /** KMP stand-in for LinkedBlockingQueue — list used as FIFO. */
    fun <E> newLinkedBlockingQueue(): MutableList<E> = mutableListOf()
    fun <E> newLinkedBlockingQueue(capacity: Int): MutableList<E> {
        Preconditions.checkArgument(capacity >= 0)
        return mutableListOf()
    }

    fun <E> newConcurrentLinkedQueue(): MutableList<E> = mutableListOf()

    fun <E : Comparable<E>> newPriorityQueue(): PriorityQueue<E> = PriorityQueue()
    fun <E> newPriorityQueue(comparator: Comparator<in E>): PriorityQueue<E> = PriorityQueue(comparator)
    fun <E : Comparable<E>> newPriorityQueue(elements: Iterable<E>): PriorityQueue<E> =
        PriorityQueue<E>().also { for (e in elements) it.offer(e) }
    fun <E> newPriorityQueue(elements: Iterable<E>, comparator: Comparator<in E>): PriorityQueue<E> =
        PriorityQueue(comparator).also { for (e in elements) it.offer(e) }

    fun <E> drain(q: ArrayDeque<E>, buffer: MutableCollection<in E>, numElements: Int): Int {
        Preconditions.checkNotNull(buffer)
        var added = 0
        while (added < numElements && q.isNotEmpty()) {
            buffer.add(q.removeFirst())
            added++
        }
        return added
    }

    fun <E> drain(q: MutableList<E>, buffer: MutableCollection<in E>, numElements: Int): Int {
        Preconditions.checkNotNull(buffer)
        var added = 0
        while (added < numElements && q.isNotEmpty()) {
            buffer.add(q.removeAt(0))
            added++
        }
        return added
    }

    fun <E> drain(q: PriorityQueue<E>, buffer: MutableCollection<in E>, numElements: Int): Int {
        var added = 0
        while (added < numElements) {
            val e = q.poll() ?: break
            buffer.add(e)
            added++
        }
        return added
    }

    /** KMP: no real lock; returns the queue unchanged. */
    fun <E> synchronizedQueue(queue: MutableList<E>): MutableList<E> = queue
    fun <E> synchronizedQueue(queue: ArrayDeque<E>): ArrayDeque<E> = queue
}

/** Minimal priority queue (binary heap) for Guava Queues API on KMP. */
class PriorityQueue<E>(private val comparator: Comparator<in E>? = null) {
    private val heap = ArrayList<E>()

    fun size(): Int = heap.size
    fun isEmpty(): Boolean = heap.isEmpty()
    fun peek(): E? = heap.firstOrNull()

    fun offer(element: E): Boolean {
        heap.add(element)
        siftUp(heap.size - 1)
        return true
    }

    fun poll(): E? {
        if (heap.isEmpty()) return null
        val result = heap[0]
        val last = heap.removeAt(heap.lastIndex)
        if (heap.isNotEmpty()) {
            heap[0] = last
            siftDown(0)
        }
        return result
    }

    private fun compare(a: E, b: E): Int =
        if (comparator != null) comparator.compare(a, b)
        else {
            @Suppress("UNCHECKED_CAST")
            (a as Comparable<E>).compareTo(b)
        }

    private fun siftUp(index: Int) {
        var i = index
        while (i > 0) {
            val parent = (i - 1) ushr 1
            if (compare(heap[i], heap[parent]) >= 0) break
            val t = heap[i]; heap[i] = heap[parent]; heap[parent] = t
            i = parent
        }
    }

    private fun siftDown(index: Int) {
        var i = index
        val n = heap.size
        while (true) {
            val left = i * 2 + 1
            if (left >= n) break
            var smallest = left
            val right = left + 1
            if (right < n && compare(heap[right], heap[left]) < 0) smallest = right
            if (compare(heap[i], heap[smallest]) <= 0) break
            val t = heap[i]; heap[i] = heap[smallest]; heap[smallest] = t
            i = smallest
        }
    }
}
