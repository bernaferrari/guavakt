package dev.guavakt.base

internal object Java8Compatibility {
    fun <T> closeAsQuietly(closeable: T?, closer: (T) -> Unit) {
        if (closeable != null) {
            try {
                closer(closeable)
            } catch (_: RuntimeException) {
            }
        }
    }
}
