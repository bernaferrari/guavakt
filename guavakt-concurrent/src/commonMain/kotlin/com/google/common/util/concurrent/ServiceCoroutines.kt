package dev.guavakt.util.concurrent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspend until this service reaches [Service.State.RUNNING] without blocking a thread.
 *
 * Cancellation normally stops only the observation. Set [stopOnCancellation] when this coroutine
 * owns the service and should request [Service.stopAsync] as part of structured cancellation.
 * A service that stops or fails before running resumes with [IllegalStateException].
 */
suspend fun Service.awaitRunningSuspend(stopOnCancellation: Boolean = false) {
    awaitServiceState(Service.State.RUNNING, stopOnCancellation)
}

/**
 * Suspend until this service reaches [Service.State.TERMINATED] without blocking a thread.
 * A transition to [Service.State.FAILED] resumes with [IllegalStateException] and preserves the
 * service failure as its cause. Cancelling the observer does not stop the service unless
 * [stopOnCancellation] is true.
 */
suspend fun Service.awaitTerminatedSuspend(stopOnCancellation: Boolean = false) {
    awaitServiceState(Service.State.TERMINATED, stopOnCancellation)
}

/**
 * Observe lifecycle states as a cold, duplicate-free [Flow].
 *
 * The current state is emitted on collection, future transitions follow listener order, and the
 * flow completes normally after `TERMINATED` or `FAILED`. Guava's listener contract cannot replay
 * a transition that finished before subscription. GuavaKt services detach the listener when the
 * collector is cancelled; foreign [Service] implementations may retain it until termination.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Service.stateChanges(): Flow<Service.State> = callbackFlow {
    lateinit var listener: Service.Listener

    fun emit(state: Service.State) {
        trySend(state)
        if (state == Service.State.TERMINATED || state == Service.State.FAILED) close()
    }

    listener = object : Service.Listener() {
        override fun starting() = emit(Service.State.STARTING)
        override fun running() = emit(Service.State.RUNNING)
        override fun stopping(from: Service.State) = emit(Service.State.STOPPING)
        override fun terminated(from: Service.State) = emit(Service.State.TERMINATED)
        override fun failed(from: Service.State, failure: Throwable) = emit(Service.State.FAILED)
    }

    addListener(listener)
    emit(state())
    awaitClose { detachCoroutineListener(listener) }
}.distinctUntilChanged()

private suspend fun Service.awaitServiceState(
    expected: Service.State,
    stopOnCancellation: Boolean,
) {
    terminalResult(expected)?.let { it.getOrThrow(); return }

    suspendCancellableCoroutine<Unit> { continuation ->
        lateinit var listener: Service.Listener
        val completionLock = Any()
        var completed = false

        fun claimCompletion(): Boolean = platformMonitorSync(completionLock) {
            if (completed) false else {
                completed = true
                true
            }
        }

        fun inspect() {
            val result = terminalResult(expected) ?: return
            if (!claimCompletion()) return
            detachCoroutineListener(listener)
            result.fold(
                onSuccess = { continuation.resume(Unit) },
                onFailure = { continuation.resumeWithException(it) },
            )
        }

        listener = object : Service.Listener() {
            override fun starting() = inspect()
            override fun running() = inspect()
            override fun stopping(from: Service.State) = inspect()
            override fun terminated(from: Service.State) = inspect()
            override fun failed(from: Service.State, failure: Throwable) = inspect()
        }

        addListener(listener)
        continuation.invokeOnCancellation {
            if (claimCompletion()) {
                detachCoroutineListener(listener)
                if (stopOnCancellation) {
                    try {
                        stopAsync()
                    } catch (_: Throwable) {
                        // Cancellation handlers must not throw; lifecycle failure remains on Service.
                    }
                }
            }
        }
        inspect()
    }
}

private fun Service.terminalResult(expected: Service.State): Result<Unit>? {
    val current = state()
    return when (expected) {
        Service.State.RUNNING -> when (current) {
            Service.State.NEW, Service.State.STARTING -> null
            Service.State.RUNNING -> Result.success(Unit)
            Service.State.FAILED -> Result.failure(unexpectedServiceState(expected, current))
            Service.State.STOPPING, Service.State.TERMINATED ->
                Result.failure(unexpectedServiceState(expected, current))
        }
        Service.State.TERMINATED -> when (current) {
            Service.State.TERMINATED -> Result.success(Unit)
            Service.State.FAILED -> Result.failure(unexpectedServiceState(expected, current))
            else -> null
        }
        else -> error("Unsupported awaited service state: $expected")
    }
}

private fun Service.unexpectedServiceState(
    expected: Service.State,
    actual: Service.State,
): IllegalStateException {
    val message = "Expected the service to be $expected, but the service has $actual"
    return if (actual == Service.State.FAILED) {
        IllegalStateException(message, failureCause())
    } else {
        IllegalStateException(message)
    }
}

private fun Service.detachCoroutineListener(listener: Service.Listener) {
    (this as? AbstractService)?.removeCoroutineListener(listener)
}
