package dev.guavakt.util.concurrent

/** Guava ServiceManagerBridge — idle service with no-op start/stop (override in apps). */
open class ServiceManagerBridge : AbstractIdleService() {
    override fun startUp() {}
    override fun shutDown() {}
}
