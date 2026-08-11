package dev.guavakt.util.concurrent

/**
 * Guava ThreadFactoryBuilder — builds thread naming/daemon/priority config.
 * KMP has no java.lang.Thread; exposes configuration for platform adapters.
 */
class ThreadFactoryBuilder {
    private var nameFormat: String? = null
    private var daemon: Boolean? = null
    private var priority: Int? = null
    private var uncaughtExceptionHandler: ((Throwable) -> Unit)? = null

    fun setNameFormat(nameFormat: String): ThreadFactoryBuilder {
        // validate format has %d
        format(nameFormat, 0)
        this.nameFormat = nameFormat
        return this
    }

    fun setDaemon(daemon: Boolean): ThreadFactoryBuilder {
        this.daemon = daemon
        return this
    }

    fun setPriority(priority: Int): ThreadFactoryBuilder {
        this.priority = priority
        return this
    }

    fun setUncaughtExceptionHandler(handler: (Throwable) -> Unit): ThreadFactoryBuilder {
        this.uncaughtExceptionHandler = handler
        return this
    }

    fun build(): KmpThreadFactory {
        val nf = nameFormat
        val d = daemon
        val p = priority
        val h = uncaughtExceptionHandler
        var count = 0
        return KmpThreadFactory { runnable ->
            val name = nf?.let { format(it, count++) }
            KmpThreadConfig(runnable, name, d, p, h)
        }
    }

    private fun format(format: String, arg: Int): String =
        format.replace("%d", arg.toString())
}

fun interface KmpThreadFactory {
    fun newThread(runnable: () -> Unit): KmpThreadConfig
}

data class KmpThreadConfig(
    val runnable: () -> Unit,
    val name: String?,
    val daemon: Boolean?,
    val priority: Int?,
    val uncaughtExceptionHandler: ((Throwable) -> Unit)?,
)
