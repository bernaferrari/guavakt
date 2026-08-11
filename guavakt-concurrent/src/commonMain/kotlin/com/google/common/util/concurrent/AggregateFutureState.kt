package dev.guavakt.util.concurrent

/** Guava AggregateFutureState — tracks remaining futures for [AggregateFuture]. */
internal class AggregateFutureState(initialRemaining: Int) {
    @kotlin.concurrent.Volatile var remaining: Int = initialRemaining
    fun decrementRemaining(): Int = --remaining
}
