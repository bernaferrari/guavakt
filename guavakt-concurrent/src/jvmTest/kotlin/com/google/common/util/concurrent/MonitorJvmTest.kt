package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class MonitorJvmTest {
    @Test
    fun enterWhen_waitsUntilGuardSatisfied() {
        val monitor = Monitor()
        val ready = AtomicInteger(0)
        val guard = object : Monitor.Guard(monitor) {
            override fun isSatisfied(): Boolean = ready.get() == 1
        }
        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        val t = Thread {
            started.countDown()
            monitor.enterWhen(guard)
            try {
                assertEquals(1, ready.get())
            } finally {
                monitor.leave()
                done.countDown()
            }
        }
        t.start()
        assertTrue(started.await(2, TimeUnit.SECONDS))
        Thread.sleep(50)
        monitor.enter()
        try {
            ready.set(1)
        } finally {
            monitor.leave() // notifies waiters
        }
        assertTrue(done.await(5, TimeUnit.SECONDS), "enterWhen should complete after guard satisfied")
        t.join(2000)
    }

    @Test
    fun interruptibleEntryThrowsAndDoesNotAcquire() {
        val monitor = Monitor()
        val holderReady = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val holder = Thread {
            monitor.enter()
            try {
                holderReady.countDown()
                releaseHolder.await()
            } finally {
                monitor.leave()
            }
        }
        holder.start()
        assertTrue(holderReady.await(2, TimeUnit.SECONDS))

        val waiting = CountDownLatch(1)
        val interrupted = AtomicInteger()
        val contender = Thread {
            waiting.countDown()
            try {
                monitor.enterInterruptibly()
                monitor.leave()
            } catch (_: InterruptedException) {
                interrupted.incrementAndGet()
            }
        }
        contender.start()
        assertTrue(waiting.await(2, TimeUnit.SECONDS))
        contender.interrupt()
        contender.join(2_000)
        releaseHolder.countDown()
        holder.join(2_000)

        assertEquals(1, interrupted.get())
        assertFalse(contender.isAlive)
        assertFalse(monitor.isOccupied())
    }

    @Test
    fun uninterruptibleGuardWaitRestoresInterruptStatus() {
        val monitor = Monitor()
        val ready = AtomicInteger()
        val guard = monitor.newGuard { ready.get() == 1 }
        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        val interruptRestored = AtomicInteger()
        val waiterFailure = AtomicReference<Throwable?>()
        val waiter = Thread {
            started.countDown()
            try {
                check(monitor.enterWhenUninterruptibly(guard, 20.seconds)) { "guard wait timed out" }
                try {
                    if (Thread.currentThread().isInterrupted) interruptRestored.incrementAndGet()
                } finally {
                    monitor.leave()
                }
            } catch (failure: Throwable) {
                waiterFailure.set(failure)
            } finally {
                done.countDown()
            }
        }
        waiter.start()
        assertTrue(started.await(2, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (!monitor.hasWaiters(guard)) {
            assertTrue(System.nanoTime() < deadline, "guard waiter was not registered")
            Thread.sleep(1)
        }
        waiter.interrupt()
        monitor.enter()
        try {
            ready.set(1)
        } finally {
            monitor.leave()
        }

        assertTrue(done.await(10, TimeUnit.SECONDS))
        waiter.join(2_000)
        waiterFailure.get()?.let { throw AssertionError("waiter failed", it) }
        assertEquals(1, interruptRestored.get())
        assertEquals(0, monitor.getWaitQueueLength(guard))
    }
}
