package dev.guavakt.eventbus

/**
 * Details of a failure delivered to [SubscriberExceptionHandler].
 *
 * Guava exposes a JVM `java.lang.reflect.Method` here. EventBus deliberately uses explicit
 * common-code handlers, so [subscriberMethod] is a descriptive handler label instead. It is
 * stable for a registration, but is not a portable reflection object.
 */
class SubscriberExceptionContext internal constructor(
    val event: Any,
    val eventBus: EventBus,
    val subscriber: Any,
    val subscriberMethod: String,
)
