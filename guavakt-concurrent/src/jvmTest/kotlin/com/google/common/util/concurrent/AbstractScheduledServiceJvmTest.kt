package dev.guavakt.util.concurrent

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

class AbstractScheduledServiceJvmTest {
    @Test
    fun fixedDelayWaitsAfterCompletionWhileFixedRateTargetsScheduledStartTimes() {
        val fixedRateGap = runThreeIterations(fixedRate = true)
        val fixedDelayGap = runThreeIterations(fixedRate = false)

        assertTrue(
            fixedDelayGap >= fixedRateGap + 20L,
            "fixed delay=$fixedDelayGap ms should include body duration beyond fixed rate=$fixedRateGap ms",
        )
    }

    private fun runThreeIterations(fixedRate: Boolean): Long {
        val starts = CopyOnWriteArrayList<Long>()
        val completed = CountDownLatch(1)
        val service = object : AbstractScheduledService() {
            override fun scheduler(): Scheduler =
                if (fixedRate) {
                    Scheduler.newFixedRateSchedule(initialDelayMillis = 10, periodMillis = 100)
                } else {
                    Scheduler.newFixedDelaySchedule(initialDelayMillis = 10, delayMillis = 100)
                }

            override fun runOneIteration() {
                starts += System.nanoTime()
                Thread.sleep(40)
                if (starts.size == 3) {
                    completed.countDown()
                    stopAsync()
                }
            }
        }

        service.startAsync()
        assertTrue(completed.await(2, TimeUnit.SECONDS))
        service.awaitTerminated()
        return (starts[1] - starts[0]) / 1_000_000L
    }
}
