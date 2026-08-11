package dev.guavakt.eventbus

/**
 * Receives failures thrown by an event handler.
 *
 * Event delivery continues after this method returns. Failures thrown by the exception handler
 * itself are contained so that one broken diagnostic hook cannot stop other subscribers.
 */
fun interface SubscriberExceptionHandler {
    fun handleException(exception: Throwable, context: SubscriberExceptionContext)
}
