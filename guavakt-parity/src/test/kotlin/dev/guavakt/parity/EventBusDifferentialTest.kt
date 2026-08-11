package dev.guavakt.parity

import com.google.common.eventbus.EventBus as GuavaEventBus
import com.google.common.eventbus.Subscribe as GuavaSubscribe
import dev.guavakt.eventbus.EventBus
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBusDifferentialTest {
    @Test
    fun subscriberFailureIsReportedAndDoesNotPreventLaterDelivery() {
        val expectedFailure = IllegalStateException("boom")
        val guavaDelivered = ArrayList<String>()
        val guavaBus = GuavaEventBus("differential")
        val guavaFailing = GuavaFailingSubscriber(expectedFailure)
        val guavaHealthy = GuavaHealthySubscriber(guavaDelivered)
        guavaBus.register(guavaFailing)
        guavaBus.register(guavaHealthy)

        val guavaKtDelivered = ArrayList<String>()
        val guavaKtBus = EventBus("differential")
        val guavaKtFailing = Any()
        guavaKtBus.registerHandler<String>(guavaKtFailing) { throw expectedFailure }
        guavaKtBus.registerHandler<String>(Any()) { guavaKtDelivered.add(it) }

        guavaBus.post("event")
        guavaKtBus.post("event")

        assertEquals(guavaDelivered, guavaKtDelivered)
        assertEquals(listOf("event"), guavaKtDelivered)
    }

    @Test
    fun repeatedRegistrationDeliversOnce_likeGuava() {
        val guavaReceived = ArrayList<String>()
        val guavaSubscriber = GuavaHealthySubscriber(guavaReceived)
        val guavaBus = GuavaEventBus()
        guavaBus.register(guavaSubscriber)
        guavaBus.register(guavaSubscriber)

        val guavaKtReceived = ArrayList<String>()
        val guavaKtSubscriber = Any()
        val handler: (String) -> Unit = { guavaKtReceived.add(it) }
        val guavaKtBus = EventBus()
        guavaKtBus.registerHandler(guavaKtSubscriber, handler)
        guavaKtBus.registerHandler(guavaKtSubscriber, handler)

        guavaBus.post("event")
        guavaKtBus.post("event")

        assertEquals(guavaReceived, guavaKtReceived)
        assertEquals(listOf("event"), guavaKtReceived)
    }

    private class GuavaFailingSubscriber(private val failure: Throwable) {
        @GuavaSubscribe
        fun receive(event: String) {
            throw failure
        }
    }

    private class GuavaHealthySubscriber(private val received: MutableList<String>) {
        @GuavaSubscribe
        fun receive(event: String) {
            received.add(event)
        }
    }
}
