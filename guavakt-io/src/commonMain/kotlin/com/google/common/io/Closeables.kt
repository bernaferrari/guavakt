package dev.guavakt.io

/**
 * Guava Closeables — close helpers (KMP uses Closeable-like lambdas).
 */
object Closeables {
    fun close(closeable: AutoCloseable?, swallowIOException: Boolean) {
        if (closeable == null) return
        try {
            closeable.close()
        } catch (e: Exception) {
            if (!swallowIOException) throw e
        }
    }

    fun closeQuietly(closeable: AutoCloseable?) {
        close(closeable, swallowIOException = true)
    }
}

/** Minimal AutoCloseable for KMP without java.io. */
fun interface AutoCloseable {
    fun close()
}
