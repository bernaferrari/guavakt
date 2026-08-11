package dev.guavakt.parity

import com.google.common.util.concurrent.AsyncCallable as GuavaAsyncCallable
import com.google.common.util.concurrent.AsyncFunction as GuavaAsyncFunction
import com.google.common.util.concurrent.Futures as GuavaFutures
import com.google.common.util.concurrent.MoreExecutors as GuavaMoreExecutors
import dev.guavakt.util.concurrent.AsyncCallable
import dev.guavakt.util.concurrent.AsyncFunction
import dev.guavakt.util.concurrent.Futures
import dev.guavakt.util.concurrent.MoreExecutors
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncFunctionCallableDifferentialTest {
    @Test
    fun asyncFunctionTransformationMatchesGuava() {
        val guava = GuavaFutures.transformAsync(
            GuavaFutures.immediateFuture(4),
            GuavaAsyncFunction<Int, String> { GuavaFutures.immediateFuture("value=${it * 2}") },
            GuavaMoreExecutors.directExecutor(),
        )
        val kotlin = Futures.transformAsync(
            Futures.immediateFuture(4),
            AsyncFunction<Int, String> { Futures.immediateFuture("value=${it * 2}") },
        )

        assertEquals(guava.get(), kotlin.get())
    }

    @Test
    fun asyncCallableSubmissionMatchesGuava() {
        val guava = GuavaFutures.submitAsync(
            GuavaAsyncCallable { GuavaFutures.immediateFuture("done") },
            GuavaMoreExecutors.directExecutor(),
        )
        val kotlin = Futures.submitAsync(
            AsyncCallable { Futures.immediateFuture("done") },
            MoreExecutors.directExecutor(),
        )

        assertEquals(guava.get(), kotlin.get())
    }
}
