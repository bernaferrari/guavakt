package dev.guavakt.util.concurrent

/**
 * Guava AbstractScheduledService — [runOneIteration] on a schedule.
 * Every target uses [platformScheduleAtFixedRate] or [platformScheduleWithFixedDelay] according
 * to [scheduler]. The JVM scheduler is a daemon executor; other targets use a private coroutine
 * scheduler. Apps needing structured ownership should prefer a coroutine loop in their own scope.
 */
abstract class AbstractScheduledService : AbstractService() {
    @kotlin.concurrent.Volatile private var runningFlag = false
    private var handle: PlatformScheduledHandle? = null

    protected abstract fun runOneIteration()
    protected open fun startUp() {}
    protected open fun shutDown() {}
    protected open fun scheduler(): Scheduler = Scheduler.newFixedDelaySchedule(0, 0)

    class Scheduler private constructor(
        val initialDelayMillis: Long,
        val delayMillis: Long,
        val fixedRate: Boolean,
    ) {
        companion object {
            fun newFixedDelaySchedule(initialDelayMillis: Long, delayMillis: Long): Scheduler =
                Scheduler(initialDelayMillis, delayMillis, fixedRate = false)
            fun newFixedRateSchedule(initialDelayMillis: Long, periodMillis: Long): Scheduler =
                Scheduler(initialDelayMillis, periodMillis, fixedRate = true)
        }
    }

    override fun doStart() {
        try {
            startUp()
            notifyStarted()
            runningFlag = true
            val sched = scheduler()
            if (sched.delayMillis <= 0) {
                // One-shot (or zero-period): run once then stop if still running
                try {
                    if (runningFlag && state() == Service.State.RUNNING) runOneIteration()
                } catch (t: Throwable) {
                    notifyFailed(t)
                    return
                }
                if (state() == Service.State.RUNNING) {
                    runningFlag = false
                    shutDown()
                    notifyStopped()
                }
            } else {
                val scheduledIteration = scheduledIteration@{
                    if (!runningFlag || state() != Service.State.RUNNING) return@scheduledIteration
                    try {
                        runOneIteration()
                    } catch (t: Throwable) {
                        handle?.cancel()
                        runningFlag = false
                        notifyFailed(t)
                    }
                }
                handle = if (sched.fixedRate) {
                    platformScheduleAtFixedRate(
                        sched.initialDelayMillis,
                        sched.delayMillis,
                        scheduledIteration,
                    )
                } else {
                    platformScheduleWithFixedDelay(
                        sched.initialDelayMillis,
                        sched.delayMillis,
                        scheduledIteration,
                    )
                }
            }
        } catch (t: Throwable) {
            notifyFailed(t)
        }
    }

    override fun doStop() {
        runningFlag = false
        handle?.cancel()
        handle = null
        try {
            shutDown()
            if (state() == Service.State.STOPPING || state() == Service.State.RUNNING) {
                notifyStopped()
            }
        } catch (t: Throwable) {
            notifyFailed(t)
        }
    }
}
