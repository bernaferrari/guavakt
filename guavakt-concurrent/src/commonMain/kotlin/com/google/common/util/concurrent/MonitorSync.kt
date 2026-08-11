package dev.guavakt.util.concurrent

/** Real JVM synchronization and a single-threaded critical section on other targets. */
internal inline fun <T> monitorSync(lock: Any, block: () -> T): T = platformMonitorSync(lock, block)
