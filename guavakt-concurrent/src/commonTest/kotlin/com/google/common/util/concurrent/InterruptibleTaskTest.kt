package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterruptibleTaskTest {
    @Test
    fun runsAtMostOnceAndForwardsNullableSuccess() {
        val task = RecordingTask(result = null)

        task()
        task()

        assertEquals(1, task.runCount)
        assertEquals(1, task.successes.size)
        assertEquals(null, task.successes.single())
        assertTrue(task.failures.isEmpty())
    }

    @Test
    fun completedTaskIsClaimedWithoutRunningOrNotifying() {
        val task = RecordingTask(result = "unused", alreadyDone = true)

        task()
        task()

        assertEquals(0, task.runCount)
        assertTrue(task.successes.isEmpty())
        assertTrue(task.failures.isEmpty())
    }

    @Test
    fun forwardsFailureAndExposesCooperativeInterruptRequest() {
        val task = RecordingTask(
            failure = IllegalStateException("boom"),
            requestInterruptDuringRun = true,
        )

        task()

        assertEquals(1, task.runCount)
        assertTrue(task.observedInterruptRequest)
        assertEquals(1, task.failures.size)
        assertTrue(task.failures.single() is IllegalStateException)
    }

    private class RecordingTask(
        private val result: String? = "result",
        private val failure: Throwable? = null,
        private val alreadyDone: Boolean = false,
        private val requestInterruptDuringRun: Boolean = false,
    ) : InterruptibleTask<String?>() {
        var runCount = 0
        val successes = mutableListOf<String?>()
        val failures = mutableListOf<Throwable>()
        var observedInterruptRequest = false

        override fun runInterruptibly(): String? {
            runCount++
            if (requestInterruptDuringRun) interruptTask()
            observedInterruptRequest = wasInterruptRequested()
            failure?.let { throw it }
            return result
        }

        override fun afterRanInterruptiblySuccess(result: String?) {
            successes += result
        }

        override fun afterRanInterruptiblyFailure(error: Throwable) {
            failures += error
        }

        override fun isDone(): Boolean = alreadyDone

        override fun toString(): String = "RecordingTask"
    }
}
