package dev.guavakt.util.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

class ExecutionThreadServiceJvmTest {
    @Test
    fun startAsyncReturnsBeforeRunBodyFinishes() {
        val bodyStarted = CountDownLatch(1)
        val allowBodyToFinish = CountDownLatch(1)
        val startReturned = CountDownLatch(1)
        val service = object : AbstractExecutionThreadService() {
            override fun run() {
                bodyStarted.countDown()
                allowBodyToFinish.await()
            }
        }
        val starter = Thread {
            service.startAsync()
            startReturned.countDown()
        }

        starter.start()
        try {
            assertTrue(startReturned.await(2, TimeUnit.SECONDS), "startAsync must not run the body inline")
            assertTrue(bodyStarted.await(2, TimeUnit.SECONDS))
            service.stopAsync()
        } finally {
            allowBodyToFinish.countDown()
        }

        service.awaitTerminated()
        starter.join(2_000)
        assertTrue(!starter.isAlive)
    }
}
