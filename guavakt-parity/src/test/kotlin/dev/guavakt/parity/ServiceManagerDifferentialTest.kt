package dev.guavakt.parity

import com.google.common.util.concurrent.GuavaServiceManagerHarness
import dev.guavakt.util.concurrent.AbstractService
import dev.guavakt.util.concurrent.Service
import dev.guavakt.util.concurrent.ServiceManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class ServiceManagerDifferentialTest {
    @Test
    fun normalLifecycleMatchesGuava() {
        assertEquals(GuavaServiceManagerHarness.normalLifecycleTrace(), normalLifecycleTrace())
    }

    @Test
    fun emptyLifecycleMatchesGuava() {
        assertEquals(GuavaServiceManagerHarness.emptyLifecycleTrace(), emptyLifecycleTrace())
    }

    @Test
    fun failureEventsMatchGuava() {
        assertEquals(GuavaServiceManagerHarness.failureTrace(), failureTrace())
    }

    @Test
    fun validationAndZeroTimeoutsMatchGuava() {
        val guava = GuavaServiceManagerHarness.validationAndTimeoutTrace()
        val guavaKt = validationAndTimeoutTrace()

        // Guava 33.6.0 documents duplicate services as illegal but accidentally accepts them,
        // leaving a manager whose set-backed state count can never reach its list-backed size.
        // GuavaKt deliberately enforces Guava's documented contract instead of reproducing that
        // unusable state. Every other validation and zero-timeout result remains differential.
        assertEquals("duplicate:none", guava.first())
        assertEquals("duplicate:IllegalArgumentException", guavaKt.first())
        assertEquals(guava.drop(1), guavaKt.drop(1))
    }

    private fun normalLifecycleTrace(): List<String> {
        val stopOrder = ArrayList<String>()
        val first = ImmediateService("first", stopOrder)
        val second = ImmediateService("second", stopOrder)
        val manager = ServiceManager(listOf(first, second))
        val events = attachEvents(manager)
        val trace = ArrayList<String>()
        trace += "healthy-before:${manager.isHealthy()}"
        manager.startAsync()
        manager.awaitHealthy()
        trace += "healthy-after:${manager.isHealthy()}"
        trace += "running-count:${manager.servicesByState()[Service.State.RUNNING]?.size ?: 0}"
        trace += "events-after-start:$events"
        manager.stopAsync()
        manager.awaitStopped()
        trace += "stop-order:$stopOrder"
        trace += "terminal-count:${manager.servicesByState()[Service.State.TERMINATED]?.size ?: 0}"
        trace += "events-final:$events"
        return trace
    }

    private fun emptyLifecycleTrace(): List<String> {
        val manager = ServiceManager(emptyList())
        val events = attachEvents(manager)
        val trace = ArrayList<String>()
        trace += "healthy-before:${manager.isHealthy()}"
        trace += "states-before-empty:${manager.servicesByState().isEmpty()}"
        manager.startAsync()
        manager.awaitHealthy()
        trace += "healthy-after:${manager.isHealthy()}"
        trace += "events-after-start:$events"
        manager.stopAsync()
        manager.awaitStopped()
        trace += "states-final-empty:${manager.servicesByState().isEmpty()}"
        trace += "events-final:$events"
        return trace
    }

    private fun failureTrace(): List<String> {
        val service = FailingService()
        val manager = ServiceManager(listOf(service))
        val events = attachEvents(manager)
        manager.startAsync()
        val trace = ArrayList<String>()
        trace += "state:${service.state()}"
        trace += "events:$events"
        try {
            manager.awaitHealthy()
            trace += "await-healthy:none"
        } catch (failure: Throwable) {
            trace += "await-healthy:${failure::class.simpleName}"
        }
        manager.awaitStopped()
        trace += "stopped:true"
        return trace
    }

    private fun validationAndTimeoutTrace(): List<String> {
        val trace = ArrayList<String>()
        val duplicate = ManualService()
        trace += "duplicate:${exceptionName { ServiceManager(listOf(duplicate, duplicate)) }}"
        val nonNew = ManualService()
        nonNew.stopAsync()
        trace += "non-new:${exceptionName { ServiceManager(listOf(nonNew)) }}"
        val pending = ManualService()
        val manager = ServiceManager(listOf(pending))
        manager.startAsync()
        trace += "healthy-timeout:${exceptionName { manager.awaitHealthy(Duration.ZERO) }}"
        trace += "stopped-timeout:${exceptionName { manager.awaitStopped(Duration.ZERO) }}"
        return trace
    }

    private fun attachEvents(manager: ServiceManager): MutableList<String> {
        val events = ArrayList<String>()
        manager.addListener(object : ServiceManager.Listener() {
            override fun healthy() {
                events += "healthy"
            }

            override fun stopped() {
                events += "stopped"
            }

            override fun failure(service: Service) {
                events += "failure"
            }
        })
        return events
    }

    private fun exceptionName(action: () -> Unit): String =
        try {
            action()
            "none"
        } catch (failure: Throwable) {
            failure::class.simpleName ?: "unknown"
        }

    private open class ManualService : AbstractService() {
        override fun doStart() = Unit
        override fun doStop() = Unit
    }

    private class ImmediateService(
        private val name: String,
        private val stopOrder: MutableList<String>,
    ) : AbstractService() {
        override fun doStart() = notifyStarted()
        override fun doStop() {
            stopOrder += name
            notifyStopped()
        }
    }

    private class FailingService : AbstractService() {
        override fun doStart() = notifyFailed(IllegalStateException("boom"))
        override fun doStop() = Unit
    }
}
