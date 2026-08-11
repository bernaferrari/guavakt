package dev.guavakt.util.concurrent

/**
 * Guava AbstractExecutionThreadService — [startUp], [run], [shutDown] on one logical thread.
 * The body is dispatched away from [startAsync]. JVM uses a dedicated daemon worker; other
 * targets use a private coroutine dispatcher. [run] remains a synchronous migration hook, so it
 * must cooperate with [triggerShutdown] / [isShutdownRequested] and must not block an event loop.
 * New common services should generally own a [kotlinx.coroutines.CoroutineScope] instead.
 */
abstract class AbstractExecutionThreadService : AbstractService() {
    @kotlin.concurrent.Volatile private var shutdownRequested = false

    protected open fun startUp() {}
    /** Main service body; implementations should return when [shutdownRequested] or work completes. */
    protected abstract fun run()
    protected open fun shutDown() {}
    protected open fun triggerShutdown() {
        shutdownRequested = true
    }

    protected fun isShutdownRequested(): Boolean = shutdownRequested

    override fun doStart() {
        shutdownRequested = false
        platformExecute {
            try {
                startUp()
                notifyStarted()
                if (state() == Service.State.RUNNING) {
                    try {
                        run()
                    } catch (t: Throwable) {
                        try {
                            shutDown()
                        } catch (_: Throwable) {
                        }
                        notifyFailed(t)
                        return@platformExecute
                    }
                }
                shutDown()
                notifyStopped()
            } catch (t: Throwable) {
                notifyFailed(t)
            }
        }
    }

    override fun doStop() {
        triggerShutdown()
    }

    protected open fun serviceName(): String = this::class.simpleName ?: "AbstractExecutionThreadService"
}
