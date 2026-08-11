package dev.guavakt.util.concurrent

import kotlin.time.Duration

/**
 * Portable scheduled executor backed by GuavaKt's internal delayed-callback facility.
 *
 * The supplied [delegate] executes the scheduled task body; it does not own timer creation.
 * `shutdown()` cancels queued and periodic work and rejects later submissions. For structured
 * common-code work, prefer a coroutine loop in an application-owned scope.
 */
open class WrappingScheduledExecutorService(
    private val delegate: ListeningExecutorService = MoreExecutors.directExecutor(),
) : AbstractListeningExecutorService(), ListeningScheduledExecutorService {
    private val scheduledLock = Any()
    private val scheduled = ArrayList<ScheduledListenableFuture<*>>()
    @kotlin.concurrent.Volatile private var accepting = true

    override fun execute(command: () -> Unit) {
        check(accepting) { "Scheduled executor is shut down" }
        delegate.execute(command)
    }

    override fun <V> schedule(delay: Duration, task: () -> V): ListenableScheduledFuture<V> {
        require(delay >= Duration.ZERO) { "delay must be non-negative" }
        check(accepting) { "Scheduled executor is shut down" }
        val output = ScheduledListenableFuture<V>(delay)
        track(output)
        val generation = output.reserveHandle()
        val handle = platformSchedule(delay.inWholeMilliseconds) {
            if (output.isDone()) return@platformSchedule
            try {
                output.completeFrom(delegate.submit(task))
            } catch (failure: Throwable) {
                output.fail(failure)
            }
        }
        output.installHandle(generation, handle)
        return output
    }

    override fun scheduleAtFixedRate(
        initialDelay: Duration,
        period: Duration,
        command: () -> Unit,
    ): ListenableScheduledFuture<Unit> {
        require(initialDelay >= Duration.ZERO) { "initialDelay must be non-negative" }
        require(period > Duration.ZERO) { "period must be positive" }
        check(accepting) { "Scheduled executor is shut down" }
        val output = ScheduledListenableFuture<Unit>(initialDelay)
        track(output)
        val generation = output.reserveHandle()
        val handle = platformScheduleAtFixedRate(
            initialDelay.inWholeMilliseconds,
            period.inWholeMilliseconds.coerceAtLeast(1L),
        ) {
            if (!output.isDone()) {
                output.resetDeadline(period)
                startPeriodicRun(output, command) { /* the next fixed-rate tick drives the next run */ }
            }
        }
        output.installHandle(generation, handle)
        return output
    }

    override fun scheduleWithFixedDelay(
        initialDelay: Duration,
        delay: Duration,
        command: () -> Unit,
    ): ListenableScheduledFuture<Unit> {
        require(initialDelay >= Duration.ZERO) { "initialDelay must be non-negative" }
        require(delay > Duration.ZERO) { "delay must be positive" }
        check(accepting) { "Scheduled executor is shut down" }
        val output = ScheduledListenableFuture<Unit>(initialDelay)
        track(output)
        scheduleFixedDelayRun(output, initialDelay, delay, command)
        return output
    }

    override fun shutdown() {
        accepting = false
        super<AbstractListeningExecutorService>.shutdown()
        val toCancel = platformMonitorSync(scheduledLock) {
            scheduled.toList().also { scheduled.clear() }
        }
        toCancel.forEach { it.cancel(false) }
    }

    private fun scheduleFixedDelayRun(
        output: ScheduledListenableFuture<Unit>,
        nextDelay: Duration,
        delayBetweenRuns: Duration,
        command: () -> Unit,
    ) {
        if (output.isDone()) return
        output.resetDeadline(nextDelay)
        val generation = output.reserveHandle()
        val handle = platformSchedule(nextDelay.inWholeMilliseconds) {
            startPeriodicRun(output, command) {
                scheduleFixedDelayRun(output, delayBetweenRuns, delayBetweenRuns, command)
            }
        }
        output.installHandle(generation, handle)
    }

    private fun startPeriodicRun(
        output: ScheduledListenableFuture<Unit>,
        command: () -> Unit,
        onSuccess: () -> Unit,
    ) {
        if (!output.tryBeginRun()) return
        val run = try {
            delegate.submit(command)
        } catch (failure: Throwable) {
            output.fail(failure)
            return
        }
        if (!output.attachRun(run)) return
        run.addListener {
            output.finishRun(run)
            if (output.isDone()) return@addListener
            try {
                if (run.isCancelled()) {
                    output.cancel(false)
                } else {
                    run.get()
                    onSuccess()
                }
            } catch (failure: Throwable) {
                val cause = if (failure is ExecutionException) failure.cause ?: failure else failure
                output.fail(cause)
            }
        }
    }

    private fun track(future: ScheduledListenableFuture<*>) {
        val cancel = platformMonitorSync(scheduledLock) {
            if (accepting) {
                scheduled.add(future)
                false
            } else {
                true
            }
        }
        future.addListener {
            platformMonitorSync(scheduledLock) { scheduled.remove(future) }
        }
        if (cancel) future.cancel(false)
    }
}
