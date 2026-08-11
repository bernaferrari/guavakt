package dev.guavakt.util.concurrent

/** Guava GwtFluentFutureCatchingSpecialization — fluent catching helper. */
internal object GwtFluentFutureCatchingSpecialization {
    fun <V, X : Throwable> catching(
        input: FluentFuture<V>,
        exceptionType: kotlin.reflect.KClass<X>,
        fallback: (X) -> V,
    ): FluentFuture<V> = input.catching(exceptionType, fallback)
}
