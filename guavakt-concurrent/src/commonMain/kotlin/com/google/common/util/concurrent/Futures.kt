package dev.guavakt.util.concurrent

/**
 * Guava Futures — static helpers for [ListenableFuture] composition.
 */
object Futures {
    fun <V> immediateFuture(value: V): ListenableFuture<V> {
        val f = SettableFuture.create<V>()
        f.set(value)
        return f
    }

    fun <V> immediateFailedFuture(throwable: Throwable): ListenableFuture<V> {
        val f = SettableFuture.create<V>()
        f.setException(throwable)
        return f
    }

    fun <V> immediateCancelledFuture(): ListenableFuture<V> {
        val f = SettableFuture.create<V>()
        f.cancel(false)
        return f
    }

    fun <I, O> transform(
        input: ListenableFuture<I>,
        function: (I) -> O,
    ): ListenableFuture<O> = AbstractTransformFuture.create(input, function)

    fun <V, X : Throwable> catching(
        input: ListenableFuture<out V>,
        exceptionType: kotlin.reflect.KClass<X>,
        fallback: (X) -> V,
    ): ListenableFuture<V> = AbstractCatchingFuture.create(input, exceptionType, fallback)

    fun <V, X : Throwable> catchingAsync(
        input: ListenableFuture<out V>,
        exceptionType: kotlin.reflect.KClass<X>,
        fallback: (X) -> ListenableFuture<V>,
    ): ListenableFuture<V> {
        val out = SettableFuture.create<V>()
        input.addListener {
            try {
                if (input.isCancelled()) {
                    out.cancel(false)
                    return@addListener
                }
                out.set(input.get())
            } catch (t: Throwable) {
                val cause = if (t is ExecutionException) (t.cause ?: t) else t
                if (exceptionType.isInstance(cause)) {
                    @Suppress("UNCHECKED_CAST")
                    val x = cause as X
                    try {
                        val nested = fallback(x)
                        out.setFuture(nested)
                    } catch (fb: Throwable) {
                        out.setException(fb)
                    }
                } else {
                    out.setException(cause)
                }
            }
        }
        return out
    }

    fun <V> allAsList(futures: Iterable<ListenableFuture<out V>>): ListenableFuture<List<V>> =
        AggregateFuture.create(futures.toList(), allMustSucceed = true)

    fun <V> allAsList(vararg futures: ListenableFuture<out V>): ListenableFuture<List<V>> =
        allAsList(futures.asList())

    fun <V> successfulAsList(futures: Iterable<ListenableFuture<out V>>): ListenableFuture<List<V?>> {
        @Suppress("UNCHECKED_CAST")
        return AggregateFuture.create(futures.toList() as List<ListenableFuture<out V?>>, allMustSucceed = false)
    }

    fun <V> successfulAsList(vararg futures: ListenableFuture<out V>): ListenableFuture<List<V?>> =
        successfulAsList(futures.asList())

    /**
     * Like Guava nonCancellationPropagating: the returned future completes when [future] does,
     * including cancellation, but cancelling the returned future does not cancel [future].
     */
    fun <V> nonCancellationPropagating(future: ListenableFuture<V>): ListenableFuture<V> {
        val out = SettableFuture.create<V>()
        future.addListener {
            try {
                if (future.isCancelled()) {
                    out.cancel(false)
                } else out.set(future.get())
            } catch (t: Throwable) {
                val cause = if (t is ExecutionException) (t.cause ?: t) else t
                out.setException(cause)
            }
        }
        return out
    }

    /** Registers [callback] to run through [executor] once [future] reaches a terminal state. */
    fun <V> addCallback(
        future: ListenableFuture<V>,
        callback: FutureCallback<V>,
        executor: ListeningExecutorService = MoreExecutors.directExecutor(),
    ) {
        future.addListener {
            try {
                executor.execute {
                    try {
                        if (future.isCancelled()) {
                            throw CancellationException("Future was cancelled")
                        }
                        callback.onSuccess(future.get())
                    } catch (failure: Throwable) {
                        val cause = if (failure is ExecutionException) failure.cause ?: failure else failure
                        callback.onFailure(cause)
                    }
                }
            } catch (_: Throwable) {
                // A rejected or faulty callback executor cannot change the completed future.
            }
        }
    }

    fun <V> getDone(future: ListenableFuture<V>): V {
        check(future.isDone()) { "Future was expected to be done: $future" }
        return future.get()
    }

    fun <V> getUnchecked(future: ListenableFuture<V>): V =
        try {
            future.get()
        } catch (e: ExecutionException) {
            throw UncheckedExecutionException(e.cause ?: e)
        } catch (e: CancellationException) {
            throw UncheckedExecutionException(e)
        }

    /** Async transform: when [input] completes, [function] returns a future whose result is used. */
    fun <I, O> transformAsync(
        input: ListenableFuture<I>,
        function: (I) -> ListenableFuture<O>,
    ): ListenableFuture<O> {
        val out = SettableFuture.create<O>()
        input.addListener {
            try {
                if (input.isCancelled()) {
                    out.cancel(false)
                    return@addListener
                }
                val nested = function(input.get())
                out.setFuture(nested)
            } catch (t: Throwable) {
                val cause = if (t is ExecutionException) (t.cause ?: t) else t
                out.setException(cause)
            }
        }
        return out
    }

    /** Guava-shaped overload for migration code using [AsyncFunction]. */
    fun <I, O> transformAsync(
        input: ListenableFuture<I>,
        function: AsyncFunction<I, O>,
    ): ListenableFuture<O> = transformAsync(input) { function.apply(it) }

    /**
     * Fails [input] with [TimeoutException] if not done within [timeoutMillis].
     * The returned timeout future cancels [input] with interruption requested when it wins the
     * race. On non-JVM targets, await the result with [await] rather than calling blocking `get`.
     */
    fun <V> withTimeout(
        input: ListenableFuture<V>,
        timeoutMillis: Long,
    ): ListenableFuture<V> =
        if (input.isDone()) input else TimeoutFuture.create(input, timeoutMillis)

    /** Submit [callable] on [executor] and return a listenable future of its result. */
    fun <V> submitAsync(callable: () -> V, executor: ListeningExecutorService): ListenableFuture<V> =
        executor.submit(callable)

    /**
     * Schedules an [AsyncCallable] and delegates to the future it returns.
     *
     * Cancelling the returned future before the executor starts it skips the callable. Once the
     * callable has supplied a nested future, normal `setFuture` cancellation propagation applies.
     */
    fun <V> submitAsync(
        callable: AsyncCallable<V>,
        executor: ListeningExecutorService,
    ): ListenableFuture<V> {
        val output = SettableFuture.create<V>()
        executor.execute {
            if (output.isCancelled()) return@execute
            try {
                output.setFuture(checkNotNull(callable.call()) { "AsyncCallable.call returned null" })
            } catch (failure: Throwable) {
                output.setException(failure)
            }
        }
        return output
    }

    fun submitRunnable(runnable: () -> Unit, executor: ListeningExecutorService): ListenableFuture<Unit> =
        executor.submit<Unit> { runnable() }

    /**
     * Returns a future that completes when all of [futures] complete (successfully or not).
     * The resulting list preserves input order; failed futures yield null entries only when
     * using [successfulAsList] — this method waits then returns the input list for chaining.
     */
    fun <V> whenAllComplete(futures: Iterable<ListenableFuture<out V>>): ListenableFuture<List<ListenableFuture<out V>>> {
        val list = futures.toList()
        if (list.isEmpty()) return immediateFuture(emptyList())
        val out = SettableFuture.create<List<ListenableFuture<out V>>>()
        val remaining = intArrayOf(list.size)
        val lock = Any()
        for (f in list) {
            f.addListener {
                val doneAll = platformMonitorSync(lock) {
                    remaining[0]--
                    remaining[0] == 0
                }
                if (doneAll) out.set(list)
            }
        }
        return out
    }

    fun <V> whenAllSucceed(futures: Iterable<ListenableFuture<out V>>): ListenableFuture<List<V>> =
        allAsList(futures)

    /**
     * Returns futures in the order that [futures] finish, preserving each success, failure, or
     * cancellation. Cancelling every returned future cancels still-pending inputs, matching
     * Guava's coordinated cancellation rule.
     */
    fun <V> inCompletionOrder(futures: Iterable<ListenableFuture<out V>>): List<ListenableFuture<V>> {
        val input = futures.toList()
        val state = CompletionOrderState(input)
        val delegates = List(input.size) { CompletionOrderFuture(state) }
        input.forEachIndexed { index, future ->
            future.addListener { state.recordInputCompletion(delegates, index) }
        }
        return delegates
    }
}

/** Shared state for Guava's `Futures.inCompletionOrder` cancellation and assignment semantics. */
private class CompletionOrderState<V>(input: List<ListenableFuture<out V>>) {
    private val lock = Any()
    private val pendingInputs = input.map { it as ListenableFuture<out V>? }.toMutableList()
    private var nextOutputIndex = 0
    private var incompleteOutputCount = input.size
    private var wasOutputCancelled = false
    private var shouldInterrupt = true

    fun recordInputCompletion(outputs: List<CompletionOrderFuture<V>>, inputIndex: Int) {
        val input = platformMonitorSync(lock) {
            val current = pendingInputs[inputIndex] ?: return@platformMonitorSync null
            pendingInputs[inputIndex] = null
            current
        } ?: return

        while (true) {
            val output = platformMonitorSync(lock) {
                if (nextOutputIndex >= outputs.size) null else outputs[nextOutputIndex++]
            } ?: return
            if (output.setInput(input)) {
                recordCompletion()
                return
            }
        }
    }

    fun recordOutputCancellation(mayInterruptIfRunning: Boolean) {
        val cancellation = platformMonitorSync(lock) {
            wasOutputCancelled = true
            if (!mayInterruptIfRunning) shouldInterrupt = false
            recordCompletionLocked()
        }
        cancellation?.inputs?.forEach { it.cancel(cancellation.mayInterruptIfRunning) }
    }

    private fun recordCompletion() {
        val cancellation = platformMonitorSync(lock) { recordCompletionLocked() }
        cancellation?.inputs?.forEach { it.cancel(cancellation.mayInterruptIfRunning) }
    }

    private fun recordCompletionLocked(): PendingInputCancellation<V>? {
        incompleteOutputCount--
        if (incompleteOutputCount != 0 || !wasOutputCancelled) return null
        return PendingInputCancellation<V>(
            inputs = pendingInputs.filterNotNull(),
            mayInterruptIfRunning = shouldInterrupt,
        )
    }

    private data class PendingInputCancellation<V>(
        val inputs: List<ListenableFuture<out V>>,
        val mayInterruptIfRunning: Boolean,
    )
}

/** Output slot used by [CompletionOrderState]; only direct cancellation affects input futures. */
private class CompletionOrderFuture<V>(
    private var state: CompletionOrderState<V>?,
) : AbstractFuture<V>() {
    fun setInput(input: ListenableFuture<out V>): Boolean = completeWithFuture(input)

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        val currentState = state
        if (!super.cancel(mayInterruptIfRunning)) return false
        currentState?.recordOutputCancellation(mayInterruptIfRunning)
        return true
    }

    override fun afterDone() {
        state = null
    }
}
