package dev.guavakt.eventbus

/**
 * Guava AsyncEventBus — dispatches events on an executor.
 */
class AsyncEventBus(
    private val identifier: String,
    private val executor: ExecutorLike,
    exceptionHandler: SubscriberExceptionHandler = NO_OP_EXCEPTION_HANDLER,
) {
    constructor(executor: ExecutorLike) : this("default", executor)
    constructor(executor: ExecutorLike, exceptionHandler: SubscriberExceptionHandler) :
        this("default", executor, exceptionHandler)

    private val delegate = EventBus(identifier, exceptionHandler)

    fun interface ExecutorLike {
        fun execute(command: () -> Unit)
    }

    fun identifier(): String = identifier

    fun register(subscriber: Any, handlers: Map<kotlin.reflect.KClass<*>, (Any) -> Unit>) {
        delegate.register(subscriber, handlers)
    }

    fun <E : Any> registerHandler(subscriber: Any, eventType: kotlin.reflect.KClass<E>, handler: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        delegate.register(subscriber, mapOf(eventType to { e -> handler(e as E) }))
    }

    fun unregister(subscriber: Any) {
        delegate.unregister(subscriber)
    }

    fun post(event: Any) {
        executor.execute { delegate.post(event) }
    }

    private companion object {
        val NO_OP_EXCEPTION_HANDLER = SubscriberExceptionHandler { _, _ -> }
    }
}
