package dev.guavakt.eventbus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EventBusTest {
    @Test
    fun explicitHandler_receivesEvents() {
        val bus = EventBus("test")
        val received = ArrayList<String>()
        val sub = Any()
        bus.registerHandler<String>(sub) { received.add(it) }
        bus.post("hello")
        bus.post(1) // dead event path for unmatched — should not add to received
        assertEquals(listOf("hello"), received)
    }

    @Test
    fun subscriberFailures_areReported_andDoNotStopLaterSubscribers() {
        val expectedFailure = IllegalStateException("boom")
        var reportedFailure: Throwable? = null
        var reportedContext: SubscriberExceptionContext? = null
        val bus = EventBus("failures") { failure, context ->
            reportedFailure = failure
            reportedContext = context
        }
        val failingSubscriber = Any()
        val healthySubscriber = Any()
        val received = ArrayList<String>()

        bus.registerHandler<String>(failingSubscriber) { throw expectedFailure }
        bus.registerHandler<String>(healthySubscriber) { received.add(it) }

        bus.post("event")

        assertSame(expectedFailure, reportedFailure)
        assertSame(failingSubscriber, reportedContext?.subscriber)
        assertSame(bus, reportedContext?.eventBus)
        assertEquals("event", reportedContext?.event)
        assertEquals("String handler", reportedContext?.subscriberMethod)
        assertEquals(listOf("event"), received)
    }

    @Test
    fun reentrantPosts_areDeliveredAfterTheCurrentEvent() {
        val bus = EventBus()
        val trace = ArrayList<String>()

        bus.registerHandler<String>(Any()) { event ->
            trace.add("first:$event")
            if (event == "outer") bus.post("inner")
        }
        bus.registerHandler<String>(Any()) { event -> trace.add("second:$event") }

        bus.post("outer")

        assertEquals(
            listOf("first:outer", "second:outer", "first:inner", "second:inner"),
            trace,
        )
    }

    @Test
    fun aFailingExceptionHandler_isContained() {
        val bus = EventBus("diagnostics") { _, _ -> error("diagnostic failure") }
        var delivered = false
        bus.registerHandler<String>(Any()) { throw IllegalStateException("subscriber failure") }
        bus.registerHandler<String>(Any()) { delivered = true }

        bus.post("event")

        assertEquals(true, delivered)
    }

    @Test
    fun unregisteringAnAbsentSubscriber_matchesGuavasFailureContract() {
        assertFailsWith<IllegalArgumentException> { EventBus().unregister(Any()) }
    }

    @Test
    fun duplicateRegistration_isIdempotent_andSubscribersUseIdentity() {
        val bus = EventBus()
        val duplicateTarget = EqualTarget()
        val equalButDistinctTarget = EqualTarget()
        val received = ArrayList<String>()
        val duplicateHandler: (String) -> Unit = { received.add("duplicate:$it") }

        bus.registerHandler(duplicateTarget, duplicateHandler)
        bus.registerHandler(duplicateTarget, duplicateHandler)
        bus.registerHandler<String>(equalButDistinctTarget) { received.add("distinct:$it") }
        bus.post("event")

        assertEquals(listOf("duplicate:event", "distinct:event"), received)
        assertFailsWith<IllegalArgumentException> { bus.unregister(EqualTarget()) }
    }

    @Test
    fun asyncEventBus_reportsFailuresAfterItsExecutorRunsThePostedWork() {
        val commands = ArrayDeque<() -> Unit>()
        var reported = false
        val bus = AsyncEventBus(
            identifier = "async",
            executor = AsyncEventBus.ExecutorLike { commands.addLast(it) },
            exceptionHandler = SubscriberExceptionHandler { _, _ -> reported = true },
        )
        bus.registerHandler(Any(), String::class) { throw IllegalStateException("boom") }

        bus.post("event")

        assertEquals(false, reported)
        commands.removeFirst().invoke()
        assertEquals(true, reported)
    }

    private class EqualTarget {
        override fun equals(other: Any?): Boolean = other is EqualTarget
        override fun hashCode(): Int = 0
    }
}
