package dev.guavakt.util.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConcurrencySemanticsTest {
    @Test fun inCompletionOrderAssignsDelegatesByCompletion() {
        val first = SettableFuture.create<Int>()
        val second = SettableFuture.create<Int>()
        val third = SettableFuture.create<Int>()
        val ordered = Futures.inCompletionOrder(listOf(first, second, third))
        second.set(2)
        third.set(3)
        first.set(1)
        assertEquals(listOf(2, 3, 1), ordered.map { it.get() })
    }

    @Test fun monitorExcludesOtherThreadsUntilLeave() {
        val monitor = Monitor()
        val attempted = CountDownLatch(1)
        val acquired = CountDownLatch(1)
        monitor.enter()
        val worker = thread {
            attempted.countDown()
            monitor.enter()
            try { acquired.countDown() } finally { monitor.leave() }
        }
        attempted.await()
        assertFalse(acquired.await(50, TimeUnit.MILLISECONDS))
        monitor.leave()
        assertTrue(acquired.await(2, TimeUnit.SECONDS))
        worker.join()
    }

    @Test fun awaitRunningActuallyWaitsForAsyncTransition() {
        val releaseStart = CountDownLatch(1)
        val service = object : AbstractService() {
            override fun doStart() {
                thread {
                    releaseStart.await()
                    notifyStarted()
                }
            }
            override fun doStop() = notifyStopped()
        }
        service.startAsync()
        val returned = CountDownLatch(1)
        val waiter = thread { service.awaitRunning(); returned.countDown() }
        assertFalse(returned.await(50, TimeUnit.MILLISECONDS))
        releaseStart.countDown()
        assertTrue(returned.await(2, TimeUnit.SECONDS))
        service.stopAsync().awaitTerminated()
        waiter.join()
    }
}
