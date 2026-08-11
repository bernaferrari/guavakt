package dev.guavakt.eventbus

/**
 * A Kotlin Multiplatform event bus with explicit, type-safe handler registration.
 *
 * This is intentionally not a reflection port of Guava's [Subscribe] discovery: common Kotlin
 * cannot provide Java `Method` semantics. Register handlers with [registerHandler] or
 * [register]. Re-entrant posts are queued behind the currently dispatched event. Instances are
 * not safe for concurrent calls from different threads or coroutine execution contexts.
 */
class EventBus(
    private val identifier: String = "default",
    private val exceptionHandler: SubscriberExceptionHandler = NO_OP_EXCEPTION_HANDLER,
) {
    constructor(exceptionHandler: SubscriberExceptionHandler) : this("default", exceptionHandler)

    private val subscribers = ArrayList<SubscriberGroup>()
    private val dispatcher = Dispatcher.perThreadDispatchQueue()

    fun identifier(): String = identifier

    fun register(subscriber: Any, handlers: Map<kotlin.reflect.KClass<*>, (Any) -> Unit>) {
        for ((type, handler) in handlers) {
            registerHandlerInternal(subscriber, type, handler, handler)
        }
    }

    inline fun <reified E : Any> registerHandler(subscriber: Any, noinline handler: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        registerHandlerInternal(subscriber, E::class, handler) { event -> handler(event as E) }
    }

    fun unregister(subscriber: Any) {
        val index = subscribers.indexOfFirst { it.target === subscriber }
        require(index >= 0) { "Subscriber was not registered: $subscriber" }
        subscribers.removeAt(index)
    }

    fun post(event: Any) {
        val matchingSubscribers = subscribers
            .flatMap { it.handlers.toList() }
            .filter { it.eventType.isInstance(event) }
        if (matchingSubscribers.isEmpty()) {
            if (event !is DeadEvent) post(DeadEvent(this, event))
            return
        }
        dispatcher.dispatch(event, matchingSubscribers.iterator())
    }

    internal fun handleSubscriberException(
        failure: Throwable,
        event: Any,
        subscriber: Any,
        subscriberMethod: String,
    ) {
        try {
            exceptionHandler.handleException(
                failure,
                SubscriberExceptionContext(event, this, subscriber, subscriberMethod),
            )
        } catch (_: Throwable) {
            // A diagnostic hook must not prevent later subscribers from receiving the event.
        }
    }

    @PublishedApi
    internal fun registerHandlerInternal(
        subscriber: Any,
        eventType: kotlin.reflect.KClass<*>,
        registrationToken: Any,
        handler: (Any) -> Unit,
    ) {
        val group = subscribers.firstOrNull { it.target === subscriber }
            ?: SubscriberGroup(subscriber, ArrayList()).also { subscribers.add(it) }
        if (group.handlers.any { it.isSameRegistration(eventType, registrationToken) }) return
        group.handlers.add(
            Subscriber.create(
                bus = this,
                listener = subscriber,
                eventType = eventType,
                registrationToken = registrationToken,
                methodName = "${eventType.simpleName ?: "event"} handler",
                handler = handler,
            ),
        )
    }

    private companion object {
        val NO_OP_EXCEPTION_HANDLER = SubscriberExceptionHandler { _, _ -> }
    }
}

private class SubscriberGroup(
    val target: Any,
    val handlers: MutableList<Subscriber>,
)
