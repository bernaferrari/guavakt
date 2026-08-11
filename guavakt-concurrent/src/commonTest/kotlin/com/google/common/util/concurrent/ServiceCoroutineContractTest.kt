package dev.guavakt.util.concurrent

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceCoroutineContractTest {
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
        fun fail(cause: Throwable) = notifyFailed(cause)
    }

    @Test
    fun stopDuringStartupIsDeferredAndSkipsRunning() {
        val service = ManualService()
        val events = ArrayList<String>()
        service.addListener(recordingListener(events))

        service.startAsync()
        service.stopAsync()

        assertEquals(Service.State.STOPPING, service.state())
        assertEquals(0, service.stopCalls)
        assertEquals(1, service.cancelStartCalls)
        service.started()
        assertEquals(1, service.stopCalls)
        assertEquals(Service.State.STOPPING, service.state())
        service.stopped()

        assertEquals(
            listOf("starting", "stopping:STARTING", "terminated:STOPPING"),
            events,
        )
    }

    @Test
    fun stopFromNewTerminatesWithoutStarting() {
        val service = ManualService()
        val events = ArrayList<String>()
        service.addListener(recordingListener(events))

        service.stopAsync()

        assertEquals(Service.State.TERMINATED, service.state())
        assertEquals(0, service.startCalls)
        assertEquals(0, service.stopCalls)
        assertEquals(listOf("terminated:NEW"), events)
    }

    @Test
    fun awaitRunningSuspendTracksManualStartup() = runTest {
        val service = ManualService()
        val waiter = async { service.awaitRunningSuspend() }
        runCurrent()
        assertFalse(waiter.isCompleted)

        service.startAsync()
        assertFalse(waiter.isCompleted)
        service.started()

        waiter.await()
        assertEquals(Service.State.RUNNING, service.state())
    }

    @Test
    fun cancelledObserverDoesNotOwnServiceByDefault() = runTest {
        val service = ManualService()
        service.startAsync()
        service.started()
        val waiter = async { service.awaitTerminatedSuspend() }
        runCurrent()

        waiter.cancelAndJoin()

        assertEquals(Service.State.RUNNING, service.state())
        assertEquals(0, service.stopCalls)
    }

    @Test
    fun ownedWaitCanStopServiceOnCancellation() = runTest {
        val service = ManualService()
        service.startAsync()
        service.started()
        val waiter = async { service.awaitTerminatedSuspend(stopOnCancellation = true) }
        runCurrent()

        waiter.cancelAndJoin()

        assertEquals(Service.State.STOPPING, service.state())
        assertEquals(1, service.stopCalls)
    }

    @Test
    fun failedWaitPreservesServiceFailure() = runTest {
        supervisorScope {
            val service = ManualService()
            val waiter = async { service.awaitRunningSuspend() }
            runCurrent()
            service.startAsync()
            val boom = IllegalStateException("boom")
            service.fail(boom)

            val thrown = try {
                waiter.await()
                null
            } catch (failure: Throwable) {
                failure
            }
            assertTrue(thrown is IllegalStateException)
            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it === boom })
        }
    }

    @Test
    fun stateChangesEmitsCurrentAndOrderedLifecycleUntilTerminal() = runTest {
        val service = ManualService()
        val states = async { service.stateChanges().toList() }
        runCurrent()

        service.startAsync()
        service.started()
        service.stopAsync()
        service.stopped()

        assertEquals(
            listOf(
                Service.State.NEW,
                Service.State.STARTING,
                Service.State.RUNNING,
                Service.State.STOPPING,
                Service.State.TERMINATED,
            ),
            states.await(),
        )
    }

    @Test
    fun listenerFailureCannotCorruptLifecycle() {
        val service = ManualService()
        service.addListener(object : Service.Listener() {
            override fun starting() = error("listener")
            override fun running() = error("listener")
        })

        service.startAsync()
        service.started()

        assertEquals(Service.State.RUNNING, service.state())
    }

    private fun recordingListener(events: MutableList<String>): Service.Listener =
        object : Service.Listener() {
            override fun starting() {
                events += "starting"
            }

            override fun running() {
                events += "running"
            }

            override fun stopping(from: Service.State) {
                events += "stopping:$from"
            }

            override fun terminated(from: Service.State) {
                events += "terminated:$from"
            }

            override fun failed(from: Service.State, failure: Throwable) {
                events += "failed:$from"
            }
        }
}
