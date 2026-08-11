package dev.guavakt.util.concurrent

import dev.guavakt.base.Preconditions

/**
 * Guava-shaped service lifecycle.
 *
 * State transitions and listener ordering follow Guava's `AbstractService`, including the
 * easily missed `stopAsync()`-while-`STARTING` rule: shutdown is advertised immediately but
 * [doStop] is deferred until startup calls [notifyStarted]. JVM supports blocking await methods;
 * JS, Wasm, and Native reject an await that would need to block. Common callers should prefer
 * the suspending adapters in `ServiceCoroutines.kt`.
 */
abstract class AbstractService : Service {
    private val lock = Any()
    private var state: Service.State = Service.State.NEW
    private var failure: Throwable? = null
    private var shutdownWhenStartupFinishes = false
    private val listeners = ArrayList<Service.Listener>()

    protected abstract fun doStart()
    protected abstract fun doStop()

    /** Called once when shutdown is requested while startup is still incomplete. */
    protected open fun doCancelStart() {}

    override fun startAsync(): Service {
        platformMonitorSync(lock) {
            Preconditions.checkState(state == Service.State.NEW, "Service already started")
            state = Service.State.STARTING
            platformMonitorNotifyAll(lock)
        }
        dispatch { it.starting() }
        try {
            doStart()
        } catch (failure: Throwable) {
            notifyFailed(failure)
        }
        return this
    }

    override fun stopAsync(): Service {
        var previous: Service.State? = null
        var callDoStop = false
        platformMonitorSync(lock) {
            when (state) {
                Service.State.NEW -> {
                    previous = state
                    state = Service.State.TERMINATED
                    platformMonitorNotifyAll(lock)
                }
                Service.State.STARTING -> {
                    previous = state
                    shutdownWhenStartupFinishes = true
                    state = Service.State.STOPPING
                    platformMonitorNotifyAll(lock)
                }
                Service.State.RUNNING -> {
                    previous = state
                    state = Service.State.STOPPING
                    callDoStop = true
                    platformMonitorNotifyAll(lock)
                }
                Service.State.STOPPING,
                Service.State.TERMINATED,
                Service.State.FAILED,
                -> Unit
            }
        }

        when (previous) {
            Service.State.NEW -> dispatch { it.terminated(Service.State.NEW) }
            Service.State.STARTING,
            Service.State.RUNNING,
            -> dispatch { it.stopping(previous) }
            else -> Unit
        }

        if (previous == Service.State.STARTING) {
            try {
                doCancelStart()
            } catch (failure: Throwable) {
                notifyFailed(failure)
            }
        }
        if (callDoStop) {
            try {
                doStop()
            } catch (failure: Throwable) {
                notifyFailed(failure)
            }
        }
        return this
    }

    protected fun notifyStarted() {
        var callDoStop = false
        var notifyRunning = false
        var invalidState: Service.State? = null
        platformMonitorSync(lock) {
            when {
                state == Service.State.STARTING -> {
                    state = Service.State.RUNNING
                    notifyRunning = true
                    platformMonitorNotifyAll(lock)
                }
                state == Service.State.STOPPING && shutdownWhenStartupFinishes -> {
                    shutdownWhenStartupFinishes = false
                    callDoStop = true
                }
                else -> invalidState = state
            }
        }

        invalidState?.let { invalid ->
            val failure = IllegalStateException("Cannot notifyStarted() when the service is $invalid")
            notifyFailed(failure)
            throw failure
        }

        if (notifyRunning) dispatch { it.running() }
        if (callDoStop) doStop()
    }

    protected fun notifyStopped() {
        val previous = platformMonitorSync(lock) {
            Preconditions.checkState(
                state == Service.State.STARTING ||
                    state == Service.State.RUNNING ||
                    state == Service.State.STOPPING,
                "Cannot notifyStopped() when the service is $state",
            )
            val prior = state
            state = Service.State.TERMINATED
            platformMonitorNotifyAll(lock)
            prior
        }
        dispatch { it.terminated(previous) }
    }

    protected fun notifyFailed(cause: Throwable) {
        val previous: Service.State? = platformMonitorSync(lock) {
            when (state) {
                Service.State.NEW,
                Service.State.TERMINATED,
                -> throw IllegalStateException("Failed while in state:$state", cause)
                Service.State.FAILED -> null
                Service.State.STARTING,
                Service.State.RUNNING,
                Service.State.STOPPING,
                -> {
                    val prior = state
                    state = Service.State.FAILED
                    failure = cause
                    platformMonitorNotifyAll(lock)
                    prior
                }
            }
        }
        if (previous != null) dispatch { it.failed(previous, cause) }
    }

    override fun awaitRunning() {
        val initial = state()
        if ((initial == Service.State.NEW || initial == Service.State.STARTING) &&
            !platformSupportsBlockingWait()
        ) {
            throw UnsupportedOperationException(
                "Blocking service waits are unavailable on this target; use awaitRunningSuspend()",
            )
        }
        platformMonitorAwaitUninterruptibly(lock) {
            state != Service.State.NEW && state != Service.State.STARTING
        }
        platformMonitorSync(lock) {
            if (state != Service.State.RUNNING) throw unexpectedState(Service.State.RUNNING)
        }
    }

    override fun awaitTerminated() {
        val initial = state()
        if (initial != Service.State.TERMINATED && initial != Service.State.FAILED &&
            !platformSupportsBlockingWait()
        ) {
            throw UnsupportedOperationException(
                "Blocking service waits are unavailable on this target; use awaitTerminatedSuspend()",
            )
        }
        platformMonitorAwaitUninterruptibly(lock) {
            state == Service.State.TERMINATED || state == Service.State.FAILED
        }
        platformMonitorSync(lock) {
            if (state != Service.State.TERMINATED) throw unexpectedState(Service.State.TERMINATED)
        }
    }

    override fun state(): Service.State = platformMonitorSync(lock) { state }

    override fun failureCause(): Throwable =
        platformMonitorSync(lock) {
            if (state != Service.State.FAILED) throw IllegalStateException("No failure while service is $state")
            failure ?: throw IllegalStateException("FAILED service has no failure cause")
        }

    override fun addListener(listener: Service.Listener) {
        platformMonitorSync(lock) {
            if (state != Service.State.TERMINATED && state != Service.State.FAILED) listeners.add(listener)
        }
    }

    internal fun removeCoroutineListener(listener: Service.Listener): Boolean =
        platformMonitorSync(lock) { listeners.remove(listener) }

    private fun unexpectedState(expected: Service.State): IllegalStateException {
        val message = "Expected the service to be $expected, but the service has $state"
        return if (state == Service.State.FAILED) IllegalStateException(message, failure) else IllegalStateException(message)
    }

    private fun listenerSnapshot(): List<Service.Listener> = platformMonitorSync(lock) { listeners.toList() }

    private inline fun dispatch(event: (Service.Listener) -> Unit) {
        for (listener in listenerSnapshot()) {
            try {
                event(listener)
            } catch (_: Throwable) {
                // A listener is an observer; one broken observer must not corrupt lifecycle state.
            }
        }
    }
}
