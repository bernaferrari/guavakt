package dev.guavakt.eventbus

/**
 * Guava Subscriber — wraps a listener method invocation.
 */
internal class Subscriber private constructor(
    private val bus: EventBus,
    val target: Any,
    val eventType: kotlin.reflect.KClass<*>,
    private val registrationToken: Any,
    private val methodName: String,
    private val handler: (Any) -> Unit,
) {
    fun isSameRegistration(eventType: kotlin.reflect.KClass<*>, token: Any): Boolean =
        this.eventType == eventType && registrationToken === token

    fun dispatchEvent(event: Any) {
        try {
            handler(event)
        } catch (failure: Throwable) {
            bus.handleSubscriberException(failure, event, target, methodName)
        }
    }

    companion object {
        fun create(
            bus: EventBus,
            listener: Any,
            eventType: kotlin.reflect.KClass<*>,
            registrationToken: Any,
            methodName: String,
            handler: (Any) -> Unit,
        ): Subscriber = Subscriber(bus, listener, eventType, registrationToken, methodName, handler)
    }
}
