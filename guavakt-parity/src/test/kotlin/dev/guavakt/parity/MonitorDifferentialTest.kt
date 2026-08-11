package dev.guavakt.parity

import com.google.common.util.concurrent.GuavaMonitorHarness
import dev.guavakt.util.concurrent.Monitor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.ZERO

class MonitorDifferentialTest {
    @Test
    fun immediateReentrantAndValidationBehaviorMatchesGuava() {
        assertEquals(GuavaMonitorHarness.immediateTrace(), immediateTrace())
    }

    @Test
    fun predicateFailureReleasesOwnershipLikeGuava() {
        assertEquals(GuavaMonitorHarness.predicateFailureTrace(), predicateFailureTrace())
    }

    @Test
    fun guardedWaiterAccountingAndSignallingMatchGuava() {
        assertEquals(GuavaMonitorHarness.waiterTrace(), waiterTrace())
    }

    @Test
    fun preExistingInterruptSemanticsMatchGuava() {
        assertEquals(GuavaMonitorHarness.interruptTrace(), interruptTrace())
    }

    private fun immediateTrace(): List<String> {
        val monitor = Monitor(fair = true)
        val ready = AtomicBoolean()
        val guard = monitor.newGuard(ready::get)
        val trace = ArrayList<String>()
        trace += "fair:${monitor.isFair()}"
        trace += "occupied-before:${monitor.isOccupied()}"
        trace += "try-false:${monitor.tryEnterIf(guard)}"
        trace += "occupied-after-false:${monitor.isOccupied()}"
        trace += "zero-enter:${monitor.enter(ZERO)}"
        trace += "depth-one:${monitor.getOccupiedDepth()}"
        monitor.enter()
        trace += "depth-two:${monitor.getOccupiedDepth()}"
        monitor.leave()
        monitor.leave()
        trace += "zero-guard-false:${monitor.enterWhen(guard, ZERO)}"
        ready.set(true)
        trace += "zero-guard-true:${monitor.enterWhen(guard, ZERO)}"
        if (monitor.isOccupiedByCurrentThread()) monitor.leave()
        trace += "wrong-monitor:${exceptionName { Monitor().tryEnterIf(guard) }}"
        trace += "unoccupied-wait:${exceptionName { monitor.waitFor(guard) }}"
        return trace
    }

    private fun predicateFailureTrace(): List<String> {
        val monitor = Monitor()
        val broken = monitor.newGuard { error("boom") }
        return listOf(
            "enter-if:${exceptionName { monitor.enterIf(broken) }}",
            "occupied-after-enter-if:${monitor.isOccupiedByCurrentThread()}",
            "try-enter-if:${exceptionName { monitor.tryEnterIf(broken) }}",
            "occupied-after-try:${monitor.isOccupiedByCurrentThread()}",
        )
    }

    private fun waiterTrace(): List<String> {
        val monitor = Monitor()
        val ready = AtomicBoolean()
        val guard = monitor.newGuard(ready::get)
        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        val observedSatisfied = AtomicBoolean()
        val waiter = Thread {
            started.countDown()
            try {
                monitor.enterWhen(guard)
                try {
                    observedSatisfied.set(ready.get())
                } finally {
                    monitor.leave()
                }
            } finally {
                done.countDown()
            }
        }
        waiter.start()
        check(started.await(2, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (!monitor.hasWaiters(guard)) {
            check(System.nanoTime() < deadline) { "guard waiter was not registered" }
            Thread.sleep(1)
        }

        val trace = ArrayList<String>()
        trace += "has-waiters:${monitor.hasWaiters(guard)}"
        trace += "wait-count:${monitor.getWaitQueueLength(guard)}"
        monitor.enter()
        try {
            ready.set(true)
        } finally {
            monitor.leave()
        }
        check(done.await(2, TimeUnit.SECONDS))
        waiter.join(2_000)
        trace += "saw-satisfied:${observedSatisfied.get()}"
        trace += "wait-count-after:${monitor.getWaitQueueLength(guard)}"
        return trace
    }

    private fun interruptTrace(): List<String> {
        val monitor = Monitor()
        val never = monitor.newGuard { false }
        val trace = ArrayList<String>()
        Thread.interrupted()
        try {
            Thread.currentThread().interrupt()
            trace += "uninterruptible-enter:${monitor.enter(ZERO)}"
            trace += "status-restored:${Thread.currentThread().isInterrupted}"
            Thread.interrupted()
            monitor.leave()

            Thread.currentThread().interrupt()
            trace += "interruptible-enter:${exceptionName { monitor.enterInterruptibly() }}"
            trace += "status-cleared:${!Thread.currentThread().isInterrupted}"

            Thread.currentThread().interrupt()
            trace += "interruptible-guard:${exceptionName { monitor.enterWhen(never, ZERO) }}"
            trace += "guard-status-cleared:${!Thread.currentThread().isInterrupted}"
            return trace
        } finally {
            Thread.interrupted()
            while (monitor.isOccupiedByCurrentThread()) monitor.leave()
        }
    }

    private fun exceptionName(action: () -> Unit): String = try {
        action()
        "none"
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
