package dev.guavakt.util.concurrent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineFutureInteropTest {
    @Test
    fun awaitSuspendsWithoutBlockingAndReturnsCompletion() = runTest {
        val future = SettableFuture.create<Int>()
        val waiter = async { future.await() }

        runCurrent()
        assertFalse(waiter.isCompleted)
        assertTrue(future.set(42))

        assertEquals(42, waiter.await())
    }

    @Test
    fun awaitUnwrapsExecutionExceptionAndMapsFutureCancellation() = runTest {
        val boom = IllegalArgumentException("boom")
        val failure = try {
            Futures.immediateFailedFuture<Int>(boom).await()
            null
        } catch (thrown: Throwable) {
            thrown
        }
        assertSame(boom, failure)

        val cancellation = try {
            Futures.immediateCancelledFuture<Int>().await()
            null
        } catch (thrown: Throwable) {
            thrown
        }
        assertIs<kotlinx.coroutines.CancellationException>(cancellation)
    }

    @Test
    fun cancellingAwaiterCancelsOneShotFuture() = runTest {
        val future = SettableFuture.create<Int>()
        val waiter = async { future.await() }
        runCurrent()

        waiter.cancelAndJoin()

        assertTrue(future.isCancelled())
    }

    @Test
    fun nonCancellationPropagatingWrapperSupportsObserverWaits() = runTest {
        val source = SettableFuture.create<Int>()
        val observer = async { Futures.nonCancellationPropagating(source).await() }
        runCurrent()

        observer.cancelAndJoin()

        assertFalse(source.isCancelled())
        assertTrue(source.set(7))
    }

    @Test
    fun futureAsDeferredPropagatesValuesFailuresAndCancellationBothWays() = runTest {
        val source = SettableFuture.create<Int>()
        val deferred = source.asDeferred()
        assertTrue(source.set(11))
        assertEquals(11, deferred.await())

        val boom = IllegalStateException("broken")
        val failed = Futures.immediateFailedFuture<Int>(boom).asDeferred()
        val thrown = try {
            failed.await()
            null
        } catch (failure: Throwable) {
            failure
        }
        assertTrue(thrown === boom || thrown?.cause === boom)

        val cancellable = SettableFuture.create<Int>()
        val cancelledDeferred = cancellable.asDeferred()
        cancelledDeferred.cancel()
        assertTrue(cancellable.isCancelled())

        val cancelledFuture = SettableFuture.create<Int>()
        val fromCancelledFuture = cancelledFuture.asDeferred()
        cancelledFuture.cancel(false)
        assertTrue(fromCancelledFuture.isCancelled)
    }

    @Test
    fun deferredAsFuturePropagatesValuesFailuresAndCancellationBothWays() = runTest {
        val source = CompletableDeferred<Int>()
        val future = source.asListenableFuture()
        source.complete(19)
        assertEquals(19, future.get())

        val boom = IllegalArgumentException("nope")
        val failedSource = CompletableDeferred<Int>()
        val failedFuture = failedSource.asListenableFuture()
        failedSource.completeExceptionally(boom)
        val wrapped = assertFailsWith<ExecutionException> { failedFuture.get() }
        assertSame(boom, wrapped.cause)

        val cancelledSource = CompletableDeferred<Int>()
        val cancelledFuture = cancelledSource.asListenableFuture()
        cancelledSource.cancel()
        assertTrue(cancelledFuture.isCancelled())

        val externallyCancelledSource = CompletableDeferred<Int>()
        val externallyCancelledFuture = externallyCancelledSource.asListenableFuture()
        externallyCancelledFuture.cancel(false)
        assertTrue(externallyCancelledSource.isCancelled)
    }

    @Test
    fun futureBuilderIsStructuredAndRejectsLazyStart() = runTest {
        val result = future { 23 }
        assertEquals(23, result.await())

        assertFailsWith<IllegalArgumentException> {
            future(start = CoroutineStart.LAZY) { 1 }
        }

        val pending = future { CompletableDeferred<Unit>().await(); 1 }
        pending.cancel(false)
        assertTrue(pending.isCancelled())
    }
}
