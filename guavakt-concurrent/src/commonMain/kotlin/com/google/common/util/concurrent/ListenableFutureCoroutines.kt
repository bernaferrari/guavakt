package dev.guavakt.util.concurrent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalForInheritanceCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException as CoroutineCancellationException

/**
 * Await this future without blocking a thread.
 *
 * Failure is rethrown without GuavaKt's [ExecutionException] wrapper. Cancellation propagates
 * bidirectionally: cancelling the waiting coroutine attempts `cancel(false)` on this one-shot
 * future, and future cancellation cancels the waiter. Wrap the future with
 * [Futures.nonCancellationPropagating] when the coroutine is only an observer.
 */
suspend fun <T> ListenableFuture<T>.await(): T {
    if (isDone()) return completedValueForCoroutine()

    return suspendCancellableCoroutine { continuation ->
        addListener {
            if (isCancelled()) {
                continuation.cancel(coroutineCancellation("ListenableFuture was cancelled"))
            } else {
                try {
                    continuation.resume(completedValueForCoroutine())
                } catch (failure: Throwable) {
                    continuation.resumeWithException(failure)
                }
            }
        }
        continuation.invokeOnCancellation { cancel(false) }
    }
}

/**
 * Return a [Deferred] entangled with this future.
 *
 * Completion and cancellation are propagated in both directions. As with the official
 * kotlinx-coroutines Guava adapter, the two terminal writes are not atomic: an external
 * cancellation racing normal completion may win on one side.
 */
fun <T> ListenableFuture<T>.asDeferred(): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    fun completeFromFuture() {
        if (isCancelled()) {
            deferred.cancel(coroutineCancellation("ListenableFuture was cancelled"))
            return
        }
        try {
            deferred.complete(completedValueForCoroutine())
        } catch (failure: Throwable) {
            deferred.completeExceptionally(failure)
        }
    }

    if (isDone()) completeFromFuture() else addListener(::completeFromFuture)
    deferred.invokeOnCompletion {
        if (deferred.isCancelled) cancel(false)
    }

    // Hide CompletableDeferred's mutation methods: callers receive only the promised view.
    @OptIn(InternalForInheritanceCoroutinesApi::class)
    return object : Deferred<T> by deferred {}
}

/**
 * Return a [ListenableFuture] entangled with this deferred value.
 *
 * Normal values and failure causes propagate directly (coroutine stack-trace recovery may copy an
 * exception while retaining the original as its cause). Coroutine cancellation maps to Future
 * cancellation, whose smaller state model cannot retain a cancellation cause. Cancellation of
 * the returned future synchronously requests cancellation of this deferred value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Deferred<T>.asListenableFuture(): ListenableFuture<T> {
    val future = SettableFuture.create<T>()
    future.addListener {
        if (future.isCancelled()) this.cancel(coroutineCancellation("ListenableFuture was cancelled"))
    }
    invokeOnCompletion { failure ->
        when {
            failure == null -> {
                try {
                    future.set(getCompleted())
                } catch (unexpected: Throwable) {
                    future.setException(unexpected)
                }
            }
            failure is CoroutineCancellationException -> future.cancel(false)
            else -> future.setException(failure)
        }
    }
    return future
}

/**
 * Start a structured child coroutine and expose its outcome as a [ListenableFuture].
 *
 * The API mirrors kotlinx-coroutines-guava's `future` builder, but is available from commonMain
 * for GuavaKt's multiplatform future. Lazy start is rejected because Future has no operation that
 * can start a lazy computation. Cancellation propagates bidirectionally.
 */
fun <T> CoroutineScope.future(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): ListenableFuture<T> {
    require(start != CoroutineStart.LAZY) { "$start start is not supported" }
    return async(context = context, start = start, block = block).asListenableFuture()
}

private fun coroutineCancellation(message: String): CoroutineCancellationException =
    CoroutineCancellationException(message)

private fun <T> ListenableFuture<T>.completedValueForCoroutine(): T {
    if (isCancelled()) throw coroutineCancellation("ListenableFuture was cancelled")
    return try {
        get()
    } catch (failure: ExecutionException) {
        throw failure.cause ?: failure
    } catch (_: CancellationException) {
        throw coroutineCancellation("ListenableFuture was cancelled")
    }
}
