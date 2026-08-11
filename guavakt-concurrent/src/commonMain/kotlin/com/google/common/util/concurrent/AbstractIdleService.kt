package dev.guavakt.util.concurrent

/** Guava AbstractIdleService — start/stop with notify in same call. */
abstract class AbstractIdleService : AbstractService() {
    protected abstract fun startUp()
    protected abstract fun shutDown()
    override fun doStart() {
        startUp()
        notifyStarted()
    }
    override fun doStop() {
        shutDown()
        notifyStopped()
    }
}
