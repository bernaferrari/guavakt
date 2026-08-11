package dev.guavakt.util.concurrent

/**
 * Guava ListenerCallQueue — queues listener events and dispatches on an executor.
 */
class ListenerCallQueue<L> {
    private data class Event<L>(val label: String, val call: (L) -> Unit)
    private val listeners = ArrayList<Pair<L, DirectExecutorLike>>()
    private val waitQueue = ArrayList<Event<L>>()
    private val lock = Any()
    @kotlin.concurrent.Volatile private var isDispatching = false

    fun addListener(listener: L, executor: DirectExecutorLike) {
        monitorSync(lock) { listeners.add(listener to executor) }
    }

    fun enqueue(event: (L) -> Unit) = enqueue("event", event)

    fun enqueue(label: String, event: (L) -> Unit) {
        monitorSync(lock) { waitQueue.add(Event(label, event)) }
    }

    fun dispatch() {
        while (true) {
            var toRun: List<Pair<Event<L>, List<Pair<L, DirectExecutorLike>>>>? = null
            monitorSync(lock) {
                if (isDispatching) return
                if (waitQueue.isEmpty()) return
                isDispatching = true
                val events = waitQueue.toList()
                waitQueue.clear()
                val snap = listeners.toList()
                toRun = events.map { it to snap }
            }
            try {
                for ((event, snaps) in toRun!!) {
                    for ((listener, executor) in snaps) {
                        try {
                            executor.execute { event.call(listener) }
                        } catch (_: Throwable) {}
                    }
                }
            } finally {
                monitorSync(lock) { isDispatching = false }
            }
        }
    }
}
