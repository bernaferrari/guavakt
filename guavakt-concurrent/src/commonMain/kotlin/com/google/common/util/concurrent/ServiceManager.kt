package dev.guavakt.util.concurrent

import dev.guavakt.base.Preconditions
import dev.guavakt.base.Ticker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** An immutable, point-in-time view of a [ServiceManager]. */
data class ServiceManagerSnapshot(
    val servicesByState: Map<Service.State, List<Service>>,
    val isHealthy: Boolean,
    val isStopped: Boolean,
)

/**
 * Starts, stops, times, and observes a group of [Service] instances.
 *
 * The aggregate state machine follows Guava: all inputs must be distinct and `NEW`; empty groups
 * use an invisible no-op service so healthy/stopped events still occur; stop order is input order;
 * snapshots are consistent; startup times contain only services whose startup attempt reached a
 * state at or beyond `RUNNING`. Blocking waits are real and uninterruptible on JVM only. Common
 * callers should use the suspending adapters in `ServiceManagerCoroutines.kt`.
 */
class ServiceManager(
    services: Iterable<Service>,
    private val ticker: Ticker = Ticker.systemTicker(),
) {
    private val publicServices: List<Service> = services.toList()
    private val noOpService: Service? = if (publicServices.isEmpty()) NoOpService() else null
    private val servicesList: List<Service> = noOpService?.let(::listOf) ?: publicServices
    private val stateLock = Any()
    private val states = LinkedHashMap<Service, Service.State>()
    private val startupStartedNanos = LinkedHashMap<Service, Long>()
    private val startupElapsedNanos = LinkedHashMap<Service, Long>()
    private val listeners = ArrayList<ListenerRegistration>()
    private val stateObservers = ArrayList<StateObserverRegistration>()

    init {
        val seen = HashSet<Service>()
        for (service in servicesList) {
            Preconditions.checkArgument(seen.add(service), "Duplicate service: $service")
            Preconditions.checkArgument(
                service.state() == Service.State.NEW,
                "Can only manage NEW services, $service is ${service.state()}",
            )
            states[service] = Service.State.NEW
            service.addListener(managerListener(service))
            Preconditions.checkArgument(
                service.state() == Service.State.NEW,
                "Service transitioned while ServiceManager was being constructed: $service",
            )
        }
    }

    fun startAsync(): ServiceManager {
        platformMonitorSync(stateLock) {
            Preconditions.checkState(
                states.values.all { it == Service.State.NEW },
                "Not all services are NEW, cannot start $this",
            )
        }
        for (service in servicesList) {
            platformMonitorSync(stateLock) {
                if (service !in startupStartedNanos) startupStartedNanos[service] = ticker.read()
            }
            try {
                service.startAsync()
            } catch (_: IllegalStateException) {
                // A service/listener may have raced the preflight check, matching Guava's policy.
            }
        }
        return this
    }

    fun stopAsync(): ServiceManager {
        for (service in servicesList) service.stopAsync()
        return this
    }

    fun awaitHealthy() {
        val initial = platformMonitorSync(stateLock) { healthCanResolve() }
        if (!initial && !platformSupportsBlockingWait()) {
            throw UnsupportedOperationException(
                "Blocking manager waits are unavailable on this target; use awaitHealthySuspend()",
            )
        }
        platformMonitorAwaitUninterruptibly(stateLock) { healthCanResolve() }
        platformMonitorSync(stateLock) { checkHealthy() }
    }

    fun awaitHealthy(timeout: Duration) {
        val timeoutNanos = timeout.toNonNegativeNanos()
        if (
            timeoutNanos > 0L &&
            !platformSupportsBlockingWait() &&
            !platformMonitorSync(stateLock) { healthCanResolve() }
        ) {
            throw UnsupportedOperationException(
                "Blocking manager waits are unavailable on this target; use coroutine timeouts",
            )
        }
        val reached = platformMonitorAwaitUninterruptibly(
            stateLock,
            timeoutNanos,
        ) { healthCanResolve() }
        if (!reached) {
            throw TimeoutException(
                "Timeout waiting for services to become healthy; not started: ${notRunningSnapshot()}",
            )
        }
        platformMonitorSync(stateLock) { checkHealthy() }
    }

    fun awaitStopped() {
        val initial = platformMonitorSync(stateLock) { allStopped() }
        if (!initial && !platformSupportsBlockingWait()) {
            throw UnsupportedOperationException(
                "Blocking manager waits are unavailable on this target; use awaitStoppedSuspend()",
            )
        }
        platformMonitorAwaitUninterruptibly(stateLock) { allStopped() }
    }

    fun awaitStopped(timeout: Duration) {
        val timeoutNanos = timeout.toNonNegativeNanos()
        if (
            timeoutNanos > 0L &&
            !platformSupportsBlockingWait() &&
            !platformMonitorSync(stateLock) { allStopped() }
        ) {
            throw UnsupportedOperationException(
                "Blocking manager waits are unavailable on this target; use coroutine timeouts",
            )
        }
        val reached = platformMonitorAwaitUninterruptibly(
            stateLock,
            timeoutNanos,
        ) { allStopped() }
        if (!reached) {
            throw TimeoutException(
                "Timeout waiting for services to stop; not stopped: ${notStoppedSnapshot()}",
            )
        }
    }

    fun isHealthy(): Boolean = platformMonitorSync(stateLock) { allHealthy() }

    fun servicesByState(): Map<Service.State, List<Service>> =
        platformMonitorSync(stateLock) { servicesByStateSnapshot() }

    /** Completed startup attempts, ordered by elapsed time, in milliseconds. */
    fun startupTimes(): Map<Service, Long> =
        platformMonitorSync(stateLock) {
            startupElapsedNanos.entries
                .asSequence()
                .filterNot { it.key === noOpService }
                .sortedBy { it.value }
                .associateTo(LinkedHashMap()) { it.key to (it.value / 1_000_000L) }
        }

    /** Completed startup attempts, ordered by elapsed time, with Kotlin [Duration] precision. */
    fun startupDurations(): Map<Service, Duration> =
        platformMonitorSync(stateLock) {
            startupElapsedNanos.entries
                .asSequence()
                .filterNot { it.key === noOpService }
                .sortedBy { it.value }
                .associateTo(LinkedHashMap()) { it.key to it.value.nanoseconds }
        }

    fun snapshot(): ServiceManagerSnapshot =
        platformMonitorSync(stateLock) {
            ServiceManagerSnapshot(
                servicesByState = servicesByStateSnapshot(),
                isHealthy = allHealthy(),
                isStopped = allStopped(),
            )
        }

    fun addListener(listener: Listener) {
        platformMonitorSync(stateLock) { listeners += ListenerRegistration(listener) }
    }

    internal fun removeCoroutineListener(listener: Listener): Boolean =
        platformMonitorSync(stateLock) {
            val index = listeners.indexOfFirst { it.listener === listener }
            if (index < 0) false else {
                listeners.removeAt(index)
                true
            }
        }

    internal fun healthResult(): Result<Unit>? =
        platformMonitorSync(stateLock) {
            when {
                allHealthy() -> Result.success(Unit)
                healthCanResolve() -> Result.failure(unhealthyException())
                else -> null
            }
        }

    internal fun stoppedResult(): Result<Unit>? =
        platformMonitorSync(stateLock) { if (allStopped()) Result.success(Unit) else null }

    internal fun addStateObserver(observer: (ServiceManagerSnapshot) -> Unit) {
        platformMonitorSync(stateLock) { stateObservers += StateObserverRegistration(observer) }
    }

    /** Atomically queues the current snapshot before any later transition for this observer. */
    internal fun addStateObserverAndEmitCurrent(observer: (ServiceManagerSnapshot) -> Unit) {
        val registration = platformMonitorSync(stateLock) {
            StateObserverRegistration(observer).also {
                stateObservers += it
                it.enqueue(snapshotUnsafe())
            }
        }
        registration.drain()
    }

    internal fun removeStateObserver(observer: (ServiceManagerSnapshot) -> Unit): Boolean =
        platformMonitorSync(stateLock) {
            val index = stateObservers.indexOfFirst { it.observer === observer }
            if (index < 0) false else {
                stateObservers.removeAt(index)
                true
            }
        }

    private fun managerListener(service: Service): Service.Listener =
        object : Service.Listener() {
            override fun starting() = transition(service, Service.State.NEW, Service.State.STARTING)
            override fun running() = transition(service, Service.State.STARTING, Service.State.RUNNING)
            override fun stopping(from: Service.State) = transition(service, from, Service.State.STOPPING)
            override fun terminated(from: Service.State) = transition(service, from, Service.State.TERMINATED)
            override fun failed(from: Service.State, failure: Throwable) =
                transition(service, from, Service.State.FAILED)
        }

    private fun transition(service: Service, from: Service.State, to: Service.State) {
        val dispatch = ArrayList<ListenerRegistration>()
        val observerDispatch = ArrayList<StateObserverRegistration>()
        platformMonitorSync(stateLock) {
            check(from != to) { "Service transition must change state" }
            check(states[service] == from) {
                "Service $service expected in ${states[service]}, transition reported from $from"
            }
            states[service] = to

            val started = startupStartedNanos.getOrPut(service) { ticker.read() }
            if (to.ordinal >= Service.State.RUNNING.ordinal && service !in startupElapsedNanos) {
                startupElapsedNanos[service] = elapsedNanos(started, ticker.read())
            }

            val events = ArrayList<(Listener) -> Unit>(2)
            if (to == Service.State.FAILED) events += { it.failure(service) }
            if (allHealthy()) events += { it.healthy() }
            else if (allStopped()) events += { it.stopped() }

            for (registration in listeners) {
                for (event in events) {
                    if (registration.enqueue(event)) dispatch += registration
                }
            }
            val observerSnapshot = snapshotUnsafe()
            for (registration in stateObservers) {
                if (registration.enqueue(observerSnapshot)) observerDispatch += registration
            }
            platformMonitorNotifyAll(stateLock)
        }
        observerDispatch.forEach { it.drain() }
        dispatch.distinct().forEach { it.drain() }
    }

    private fun allHealthy(): Boolean = states.isNotEmpty() && states.values.all { it == Service.State.RUNNING }

    private fun allStopped(): Boolean = states.isNotEmpty() && states.values.all {
        it == Service.State.TERMINATED || it == Service.State.FAILED
    }

    private fun healthCanResolve(): Boolean = allHealthy() || states.values.any {
        it == Service.State.STOPPING || it == Service.State.TERMINATED || it == Service.State.FAILED
    }

    private fun checkHealthy() {
        if (!allHealthy()) throw unhealthyException()
    }

    private fun unhealthyException(): IllegalStateException {
        val failed = states.filterValues { it == Service.State.FAILED }.keys
        val firstCause = failed.firstOrNull()?.let { service ->
            try {
                service.failureCause()
            } catch (_: Throwable) {
                null
            }
        }
        return IllegalStateException(
            "Expected to be healthy; services not running: ${notRunningSnapshot()}",
            firstCause,
        )
    }

    private fun servicesByStateSnapshot(): Map<Service.State, List<Service>> {
        val result = LinkedHashMap<Service.State, MutableList<Service>>()
        for ((service, state) in states) {
            if (service === noOpService) continue
            result.getOrPut(state) { ArrayList() }.add(service)
        }
        return result.mapValuesTo(LinkedHashMap()) { it.value.toList() }
    }

    private fun snapshotUnsafe(): ServiceManagerSnapshot =
        ServiceManagerSnapshot(
            servicesByState = servicesByStateSnapshot(),
            isHealthy = allHealthy(),
            isStopped = allStopped(),
        )

    private fun notRunningSnapshot(): Map<Service.State, List<Service>> =
        servicesByStateSnapshot().filterKeys { it != Service.State.RUNNING }

    private fun notStoppedSnapshot(): Map<Service.State, List<Service>> =
        servicesByStateSnapshot().filterKeys {
            it != Service.State.TERMINATED && it != Service.State.FAILED
        }

    override fun toString(): String = "ServiceManager{services=$publicServices}"

    abstract class Listener {
        open fun healthy() {}
        open fun stopped() {}
        open fun failure(service: Service) {}
    }

    private class ListenerRegistration(val listener: Listener) {
        private val lock = Any()
        private val events = ArrayDeque<(Listener) -> Unit>()
        private var dispatching = false

        fun enqueue(event: (Listener) -> Unit): Boolean =
            platformMonitorSync(lock) {
                events.addLast(event)
                if (dispatching) false else {
                    dispatching = true
                    true
                }
            }

        fun drain() {
            while (true) {
                val event = platformMonitorSync(lock) {
                    if (events.isEmpty()) {
                        dispatching = false
                        null
                    } else events.removeFirst()
                } ?: return
                try {
                    event(listener)
                } catch (_: Throwable) {
                    // One listener failure must not corrupt aggregate lifecycle state.
                }
            }
        }
    }

    private class StateObserverRegistration(
        val observer: (ServiceManagerSnapshot) -> Unit,
    ) {
        private val lock = Any()
        private val snapshots = ArrayDeque<ServiceManagerSnapshot>()
        private var dispatching = false

        fun enqueue(snapshot: ServiceManagerSnapshot): Boolean =
            platformMonitorSync(lock) {
                snapshots.addLast(snapshot)
                if (dispatching) false else {
                    dispatching = true
                    true
                }
            }

        fun drain() {
            while (true) {
                val snapshot = platformMonitorSync(lock) {
                    if (snapshots.isEmpty()) {
                        dispatching = false
                        null
                    } else snapshots.removeFirst()
                } ?: return
                try {
                    observer(snapshot)
                } catch (_: Throwable) {
                    // Observer failure must not corrupt state or prevent later snapshots.
                }
            }
        }
    }

    private class NoOpService : AbstractIdleService() {
        override fun startUp() = Unit
        override fun shutDown() = Unit
    }
}

private fun elapsedNanos(started: Long, ended: Long): Long {
    val elapsed = ended - started
    return if (elapsed < 0L) 0L else elapsed
}

private fun Duration.toNonNegativeNanos(): Long =
    if (this <= Duration.ZERO) 0L else inWholeNanoseconds
