package com.bernaferrari.guavakt.concurrent

/** Snapshot of reservations observed by a [CoroutineRateLimiter]. */
data class CoroutineRateLimiterStats(
    val completedAcquisitionCount: Long,
    val acquiredPermitCount: Long,
    val cancelledAcquisitionCount: Long,
    val cancelledPermitCount: Long,
    val rejectedAcquisitionCount: Long,
    val totalReservedWaitTimeNanos: Long,
) {
    val acceptedAcquisitionCount: Long
        get() = if (completedAcquisitionCount > Long.MAX_VALUE - cancelledAcquisitionCount) {
            Long.MAX_VALUE
        } else {
            completedAcquisitionCount + cancelledAcquisitionCount
        }
}
