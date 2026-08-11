package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals

class FutureCallbackContractTest {
    @Test
    fun callbackReceivesSuccessFailureAndCancellationExactlyOnce() {
        val events = mutableListOf<String>()
        val callback = object : FutureCallback<String> {
            override fun onSuccess(result: String) {
                events += "success:$result"
            }
            override fun onFailure(throwable: Throwable) {
                events += "failure:${throwable::class.simpleName}"
            }
        }

        Futures.addCallback(Futures.immediateFuture("ok"), callback)
        Futures.addCallback(Futures.immediateFailedFuture(IllegalArgumentException("bad")), callback)
        Futures.addCallback(Futures.immediateCancelledFuture<String>(), callback)

        assertEquals(
            listOf("success:ok", "failure:IllegalArgumentException", "failure:CancellationException"),
            events,
        )
    }

    @Test
    fun callbackIsDispatchedThroughTheProvidedExecutor() {
        val queued = ArrayDeque<() -> Unit>()
        val executor = object : AbstractListeningExecutorService() {
            override fun execute(command: () -> Unit) {
                queued += command
            }
        }
        val events = mutableListOf<String>()
        val future = SettableFuture.create<String>()
        Futures.addCallback(
            future,
            object : FutureCallback<String> {
                override fun onSuccess(result: String) { events += result }
                override fun onFailure(throwable: Throwable) { events += "failure" }
            },
            executor,
        )

        future.set("later")
        assertEquals(emptyList(), events)
        while (queued.isNotEmpty()) queued.removeFirst()()
        assertEquals(listOf("later"), events)
    }
}
