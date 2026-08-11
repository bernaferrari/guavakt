package dev.guavakt.parity

import com.google.common.util.concurrent.GuavaServiceHarness
import dev.guavakt.util.concurrent.AbstractService
import dev.guavakt.util.concurrent.Service
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceDifferentialTest {
    @Test
    fun normalLifecycleTraceMatchesGuava() {
        assertEquals(GuavaServiceHarness.normalLifecycleTrace(), guavaKtNormalLifecycleTrace())
    }

    @Test
    fun stopDuringStartupTraceMatchesGuava() {
        assertEquals(GuavaServiceHarness.stopDuringStartupTrace(), guavaKtStopDuringStartupTrace())
    }

    @Test
    fun stopFromNewTraceMatchesGuava() {
        assertEquals(GuavaServiceHarness.stopFromNewTrace(), guavaKtStopFromNewTrace())
    }

    @Test
    fun failureTraceAndInvalidTransitionMatchGuava() {
        assertEquals(GuavaServiceHarness.failureTrace(), guavaKtFailureTrace())
        assertEquals(GuavaServiceHarness.notifyFailedFromNewException(), guavaKtNotifyFailedFromNewException())
        assertEquals(GuavaServiceHarness.notificationEdgeTrace(), guavaKtNotificationEdgeTrace())
    }

    private fun guavaKtNormalLifecycleTrace(): List<String> {
        val service = ManualService()
        val trace = attachTrace(service)
        service.startAsync()
        service.started()
        service.stopAsync()
        service.stopped()
        trace += "state:${service.state()}"
        trace += "starts:${service.startCalls}"
        trace += "stops:${service.stopCalls}"
        return trace
    }

    private fun guavaKtStopDuringStartupTrace(): List<String> {
        val service = ManualService()
        val trace = attachTrace(service)
        service.startAsync()
        service.stopAsync()
        trace += "state-before-started:${service.state()}"
        trace += "cancel-start-before-started:${service.cancelStartCalls}"
        trace += "stops-before-started:${service.stopCalls}"
        service.started()
        trace += "state-after-started:${service.state()}"
        trace += "stops-after-started:${service.stopCalls}"
        service.stopped()
        trace += "state-final:${service.state()}"
        return trace
    }

    private fun guavaKtStopFromNewTrace(): List<String> {
        val service = ManualService()
        val trace = attachTrace(service)
        service.stopAsync()
        trace += "state:${service.state()}"
        trace += "starts:${service.startCalls}"
        trace += "stops:${service.stopCalls}"
        return trace
    }

    private fun guavaKtFailureTrace(): List<String> {
        val service = ManualService()
        val trace = attachTrace(service)
        service.startAsync()
        val failure = IllegalStateException("boom")
        service.failed(failure)
        trace += "state:${service.state()}"
        trace += "same-cause:${service.failureCause() === failure}"
        return trace
    }

    private fun guavaKtNotifyFailedFromNewException(): String {
        val service = ManualService()
        return try {
            service.failed(IllegalStateException("boom"))
            "none"
        } catch (failure: Throwable) {
            failure::class.simpleName ?: "unknown"
        }
    }

    private fun guavaKtNotificationEdgeTrace(): List<String> {
        val trace = ArrayList<String>()

        val stoppedWhileStarting = ManualService()
        stoppedWhileStarting.startAsync()
        stoppedWhileStarting.stopped()
        trace += "stopped-from-starting:${stoppedWhileStarting.state()}"

        val repeatedFailure = ManualService()
        repeatedFailure.startAsync()
        val first = IllegalStateException("first")
        repeatedFailure.failed(first)
        repeatedFailure.failed(IllegalStateException("second"))
        trace += "repeated-failure-keeps-first:${repeatedFailure.failureCause() === first}"

        val duplicateStarted = ManualService()
        attachTrace(duplicateStarted).also { events ->
            duplicateStarted.startAsync()
            duplicateStarted.started()
            try {
                duplicateStarted.started()
                events += "duplicate-started:none"
            } catch (failure: Throwable) {
                events += "duplicate-started:${failure::class.simpleName}"
            }
            events += "duplicate-started-state:${duplicateStarted.state()}"
            events += "duplicate-started-cause:${duplicateStarted.failureCause().message}"
            trace += events
        }
        return trace
    }

    private fun attachTrace(service: Service): MutableList<String> {
        val trace = ArrayList<String>()
        service.addListener(object : Service.Listener() {
            override fun starting() {
                trace += "starting"
            }

            override fun running() {
                trace += "running"
            }

            override fun stopping(from: Service.State) {
                trace += "stopping:$from"
            }

            override fun terminated(from: Service.State) {
                trace += "terminated:$from"
            }

            override fun failed(from: Service.State, failure: Throwable) {
                trace += "failed:$from:${failure.message}"
            }
        })
        return trace
    }

    private class ManualService : AbstractService() {
        var startCalls = 0
        var stopCalls = 0
        var cancelStartCalls = 0

        override fun doStart() {
            startCalls++
        }

        override fun doStop() {
            stopCalls++
        }

        override fun doCancelStart() {
            cancelStartCalls++
        }

        fun started() = notifyStarted()
        fun stopped() = notifyStopped()
        fun failed(failure: Throwable) = notifyFailed(failure)
    }
}
