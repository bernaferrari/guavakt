package dev.guavakt.util.concurrent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceManagerConcurrencyJvmTest {
    @Test
    fun concurrentComponentTransitionsRemainOrderedForEachFlowCollector() = runTest {
        val first = ManualService()
        val second = ManualService()
        val manager = ServiceManager(listOf(first, second))
        val snapshots = async { manager.stateChanges().toList() }
        runCurrent()

        manager.startAsync()
        runConcurrently(first::started, second::started)
        manager.stopAsync()
        runConcurrently(first::stopped, second::stopped)

        val observed = snapshots.await()
        assertEquals(9, observed.size)
        assertTrue(observed.last().isStopped)
        for (service in listOf(first, second)) {
            val serviceStates = observed.map { snapshot ->
                snapshot.servicesByState.entries.single { service in it.value }.key
            }
            assertTrue(serviceStates.zipWithNext().all { (before, after) -> before.ordinal <= after.ordinal })
        }
    }

    private fun runConcurrently(first: () -> Unit, second: () -> Unit) {
        val firstThread = Thread(first)
        val secondThread = Thread(second)
        firstThread.start()
        secondThread.start()
        firstThread.join(2_000)
        secondThread.join(2_000)
        check(!firstThread.isAlive && !secondThread.isAlive)
    }

    private class ManualService : AbstractService() {
        override fun doStart() = Unit
        override fun doStop() = Unit
        fun started() = notifyStarted()
        fun stopped() = notifyStopped()
    }
}
