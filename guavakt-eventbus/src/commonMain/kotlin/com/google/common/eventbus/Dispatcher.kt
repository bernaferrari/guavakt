package dev.guavakt.eventbus

/**
 * Event dispatch strategies.
 *
 * Common Kotlin has no portable thread-local primitive. The default queue therefore guarantees
 * deterministic ordering for re-entrant [EventBus.post] calls made during one synchronous
 * delivery, but an EventBus must not be used concurrently from multiple execution contexts.
 */
internal abstract class Dispatcher {
    abstract fun dispatch(event: Any, subscribers: Iterator<Subscriber>)

    companion object {
        fun perThreadDispatchQueue(): Dispatcher = object : Dispatcher() {
            private val queue = ArrayDeque<Pair<Any, Iterator<Subscriber>>>()
            private var dispatching = false
            override fun dispatch(event: Any, subscribers: Iterator<Subscriber>) {
                queue.addLast(event to subscribers)
                if (!dispatching) {
                    dispatching = true
                    try {
                        while (queue.isNotEmpty()) {
                            val (e, subs) = queue.removeFirst()
                            while (subs.hasNext()) subs.next().dispatchEvent(e)
                        }
                    } finally {
                        dispatching = false
                    }
                }
            }
        }

        fun immediate(): Dispatcher = object : Dispatcher() {
            override fun dispatch(event: Any, subscribers: Iterator<Subscriber>) {
                while (subscribers.hasNext()) subscribers.next().dispatchEvent(event)
            }
        }

        fun legacyAsync(): Dispatcher = immediate() // KMP: same as immediate without threads
    }
}
