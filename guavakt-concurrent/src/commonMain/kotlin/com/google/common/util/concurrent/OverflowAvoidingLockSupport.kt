package dev.guavakt.util.concurrent

/** Guava OverflowAvoidingLockSupport — park nanos capped to avoid overflow. */
internal object OverflowAvoidingLockSupport {
    private const val MAX_NANOSECONDS = 1000L * 1000L * 1000L * 100L // 100s like Guava

    fun parkNanos(blocker: Any?, nanos: Long) {
        // KMP: no LockSupport; no-op with API preserved
        val toPark = if (nanos > MAX_NANOSECONDS) MAX_NANOSECONDS else nanos
        if (toPark <= 0) return
    }
}
