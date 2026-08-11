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
 * Suspend without blocking until every managed service is running.
 *
 * Failure or an early stopping/terminal transition resumes with [IllegalStateException]. Cancelling
 * this observer leaves the services alone unless [stopOnCancellation] is explicitly enabled.
 */
suspend fun ServiceManager.awaitHealthySuspend(stopOnCancellation: Boolean = false) {
    awaitManagerState(stopOnCancellation, ServiceManager::healthResult)
}

/**
 * Suspend without blocking until every managed service is terminated or failed.
 * Cancelling this observer leaves the services alone unless [stopOnCancellation] is enabled.
 */
suspend fun ServiceManager.awaitStoppedSuspend(stopOnCancellation: Boolean = false) {
    awaitManagerState(stopOnCancellation, ServiceManager::stoppedResult)
}

/**
 * Observe consistent aggregate snapshots as a cold [Flow].
 *
 * Collection begins with the current snapshot, emits after every component transition, and ends
 * when all services are terminal. The observer is detached on cancellation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun ServiceManager.stateChanges(): Flow<ServiceManagerSnapshot> = callbackFlow {
    val observer: (ServiceManagerSnapshot) -> Unit = { current ->
        trySend(current)
        if (current.isStopped) close()
    }
    addStateObserverAndEmitCurrent(observer)
    awaitClose { removeStateObserver(observer) }
}.distinctUntilChanged()

private suspend fun ServiceManager.awaitManagerState(
    stopOnCancellation: Boolean,
    result: ServiceManager.() -> Result<Unit>?,
) {
    result()?.let { it.getOrThrow(); return }

    suspendCancellableCoroutine<Unit> { continuation ->
        val completionLock = Any()
        var completed = false
        lateinit var observer: (ServiceManagerSnapshot) -> Unit

        fun claimCompletion(): Boolean = platformMonitorSync(completionLock) {
            if (completed) false else {
                completed = true
                true
            }
        }

        fun inspect() {
            val terminal = result() ?: return
            if (!claimCompletion()) return
            removeStateObserver(observer)
            terminal.fold(
                onSuccess = { continuation.resume(Unit) },
                onFailure = { continuation.resumeWithException(it) },
            )
        }

        observer = { _ -> inspect() }
        addStateObserver(observer)
        continuation.invokeOnCancellation {
            if (claimCompletion()) {
                removeStateObserver(observer)
                if (stopOnCancellation) {
                    try {
                        stopAsync()
                    } catch (_: Throwable) {
                    }
                }
            }
        }
        inspect()
    }
}
