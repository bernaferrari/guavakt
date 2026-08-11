package dev.guavakt.util.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceJvmInterruptTest {
    @Test
    fun awaitRunningIsUninterruptibleAndRestoresInterruptStatus() {
        val service = ManualService()
        service.startAsync()
        val entered = CountDownLatch(1)
        val interruptedAfterWait = AtomicReference<Boolean>()
        val failure = AtomicReference<Throwable?>()

        val waiter = Thread {
            Thread.currentThread().interrupt()
            entered.countDown()
            try {
                service.awaitRunning()
                interruptedAfterWait.set(Thread.currentThread().isInterrupted)
            } catch (thrown: Throwable) {
                failure.set(thrown)
            }
        }
        waiter.start()
        assertTrue(entered.await(5, TimeUnit.SECONDS))

        service.started()
        waiter.join(5_000)

        assertTrue(!waiter.isAlive)
        assertNull(failure.get())
        assertTrue(interruptedAfterWait.get())
    }

    private class ManualService : AbstractService() {
        override fun doStart() = Unit
        override fun doStop() = Unit
        fun started() = notifyStarted()
    }
}
