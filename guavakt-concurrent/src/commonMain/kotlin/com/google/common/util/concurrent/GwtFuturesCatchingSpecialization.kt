package dev.guavakt.util.concurrent

/** Guava GwtFuturesCatchingSpecialization — delegates catching to [AbstractCatchingFuture]. */
internal object GwtFuturesCatchingSpecialization {
    fun <V, X : Throwable> catching(
        input: ListenableFuture<out V>,
        exceptionType: kotlin.reflect.KClass<X>,
        fallback: (X) -> V,
    ): ListenableFuture<V> = AbstractCatchingFuture.create(input, exceptionType, fallback)
}
