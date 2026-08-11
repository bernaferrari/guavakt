package dev.guavakt.util.concurrent

/** Guava concurrent Platform — interrupt / restore helpers. */
internal object Platform {
    fun interruptCurrentThread() {
        // KMP cooperative no-op
    }

    fun restoreInterruptIfSet(interrupted: Boolean) {
        // KMP cooperative no-op
    }
}
