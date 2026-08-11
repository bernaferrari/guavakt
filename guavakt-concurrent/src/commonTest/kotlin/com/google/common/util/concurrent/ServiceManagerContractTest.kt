package dev.guavakt.util.concurrent

import dev.guavakt.base.Ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceManagerContractTest {
    @Test
    fun constructorRequiresDistinctNewServices() {
        val service = ManualService()
        assertFailsWith<IllegalArgumentException> { ServiceManager(listOf(service, service)) }

        service.stopAsync()
        assertFailsWith<IllegalArgumentException> { ServiceManager(listOf(service)) }
    }

    @Test
    fun emptyManagerUsesInvisibleLifecycleAndFiresEventsOnce() {
        val manager = ServiceManager(emptyList())
        var healthy = 0
        var stopped = 0
        manager.addListener(object : ServiceManager.Listener() {
            override fun healthy() {
                healthy++
            }

            override fun stopped() {
                stopped++
            }
        })

        assertFalse(manager.isHealthy())
        assertTrue(manager.servicesByState().isEmpty())
        manager.startAsync()
        assertTrue(manager.isHealthy())
        manager.awaitHealthy()
        assertEquals(1, healthy)

        manager.stopAsync()
        manager.awaitStopped()
        assertEquals(1, stopped)
        assertTrue(manager.servicesByState().isEmpty())
    }

    @Test
    fun normalCallbacksAreOneShotAndStopOrderMatchesInputOrder() {
        val stopOrder = ArrayList<String>()
        val first = ImmediateService("first", stopOrder)
        val second = ImmediateService("second", stopOrder)
        val manager = ServiceManager(listOf(first, second))
        var healthy = 0
        var stopped = 0
        manager.addListener(object : ServiceManager.Listener() {
            override fun healthy() {
                healthy++
            }

            override fun stopped() {
                stopped++
            }
        })

        manager.startAsync().awaitHealthy()
        manager.stopAsync().awaitStopped()

        assertEquals(1, healthy)
        assertEquals(1, stopped)
        assertEquals(listOf("first", "second"), stopOrder)
    }

    @Test
    fun startupDurationsAreMeasuredAndSorted() {
        val ticker = MutableTicker()
        val slow = TimedService(ticker, 5_000_000L)
        val fast = TimedService(ticker, 2_000_000L)
        val manager = ServiceManager(listOf(slow, fast), ticker)

        manager.startAsync().awaitHealthy()

        assertEquals(listOf(fast, slow), manager.startupTimes().keys.toList())
        assertEquals(5L, manager.startupTimes().getValue(slow))
        assertEquals(2.milliseconds, manager.startupDurations().getValue(fast))
    }

    @Test
    fun timedBlockingWaitReportsTimeout() {
        val service = ManualService()
        val manager = ServiceManager(listOf(service))
        manager.startAsync()

        assertFailsWith<TimeoutException> { manager.awaitHealthy(Duration.ZERO) }
        assertFailsWith<TimeoutException> { manager.awaitStopped(Duration.ZERO) }
    }

    @Test
    fun suspendingWaitAndFlowTrackEveryAggregateTransition() = runTest {
        val service = ManualService()
        val manager = ServiceManager(listOf(service))
        val snapshots = async { manager.stateChanges().toList() }
        val healthy = async { manager.awaitHealthySuspend() }
        runCurrent()

        manager.startAsync()
        service.started()
        healthy.await()
        manager.stopAsync()
        service.stopped()

        val states = snapshots.await().map { snapshot ->
            snapshot.servicesByState.keys.single()
        }
        assertEquals(
            listOf(
                Service.State.NEW,
                Service.State.STARTING,
                Service.State.RUNNING,
                Service.State.STOPPING,
                Service.State.TERMINATED,
            ),
            states,
        )
    }

    @Test
    fun cancelledManagerObserverIsNonOwningUnlessRequested() = runTest {
        val observed = ManualService()
        val observedManager = ServiceManager(listOf(observed))
        observedManager.startAsync()
        observed.started()
        val observer = async { observedManager.awaitStoppedSuspend() }
        runCurrent()
        observer.cancelAndJoin()
        assertEquals(Service.State.RUNNING, observed.state())

        val owned = ManualService()
        val ownedManager = ServiceManager(listOf(owned))
        ownedManager.startAsync()
        owned.started()
        val owner = async { ownedManager.awaitStoppedSuspend(stopOnCancellation = true) }
        runCurrent()
        owner.cancelAndJoin()
        assertEquals(Service.State.STOPPING, owned.state())
    }

    @Test
    fun suspendingHealthyWaitPreservesFailureCause() = runTest {
        supervisorScope {
            val service = ManualService()
            val manager = ServiceManager(listOf(service))
            val waiter = async { manager.awaitHealthySuspend() }
            runCurrent()
            manager.startAsync()
            val boom = IllegalStateException("boom")
            service.fail(boom)

            val failure = try {
                waiter.await()
                null
            } catch (thrown: Throwable) {
                thrown
            }
            assertTrue(generateSequence<Throwable>(failure) { it.cause }.any { it === boom })
        }
    }

    private class ManualService : AbstractService() {
        override fun doStart() = Unit
        override fun doStop() = Unit
        fun started() = notifyStarted()
        fun stopped() = notifyStopped()
        fun fail(failure: Throwable) = notifyFailed(failure)
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

    private class MutableTicker(var now: Long = 0L) : Ticker() {
        override fun read(): Long = now
    }

    private class TimedService(
        private val ticker: MutableTicker,
        private val startupNanos: Long,
    ) : AbstractService() {
        override fun doStart() {
            ticker.now += startupNanos
            notifyStarted()
        }

        override fun doStop() = notifyStopped()
    }
}
