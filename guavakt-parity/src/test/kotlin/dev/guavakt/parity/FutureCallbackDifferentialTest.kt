package dev.guavakt.parity

import com.google.common.util.concurrent.FutureCallback as GuavaFutureCallback
import com.google.common.util.concurrent.Futures as GuavaFutures
import com.google.common.util.concurrent.MoreExecutors as GuavaMoreExecutors
import dev.guavakt.util.concurrent.FutureCallback
import dev.guavakt.util.concurrent.Futures
import dev.guavakt.util.concurrent.MoreExecutors
import kotlin.test.Test
import kotlin.test.assertEquals

class FutureCallbackDifferentialTest {
    @Test
    fun completedSuccessAndFailureCallbacksMatchGuava() {
        val guavaEvents = mutableListOf<String>()
        val kotlinEvents = mutableListOf<String>()
        val guavaCallback = object : GuavaFutureCallback<String> {
            override fun onSuccess(result: String) { guavaEvents += "success:$result" }
            override fun onFailure(throwable: Throwable) { guavaEvents += "failure:${throwable::class.simpleName}" }
        }
        val kotlinCallback = object : FutureCallback<String> {
            override fun onSuccess(result: String) { kotlinEvents += "success:$result" }
            override fun onFailure(throwable: Throwable) { kotlinEvents += "failure:${throwable::class.simpleName}" }
        }

        GuavaFutures.addCallback(GuavaFutures.immediateFuture("ok"), guavaCallback, GuavaMoreExecutors.directExecutor())
        GuavaFutures.addCallback(GuavaFutures.immediateFailedFuture<String>(IllegalArgumentException("bad")), guavaCallback, GuavaMoreExecutors.directExecutor())
        Futures.addCallback(Futures.immediateFuture("ok"), kotlinCallback, MoreExecutors.directExecutor())
        Futures.addCallback(Futures.immediateFailedFuture<String>(IllegalArgumentException("bad")), kotlinCallback, MoreExecutors.directExecutor())

        assertEquals(guavaEvents, kotlinEvents)
    }
}
