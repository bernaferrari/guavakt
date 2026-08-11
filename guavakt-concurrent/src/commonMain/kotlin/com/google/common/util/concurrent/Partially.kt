package dev.guavakt.util.concurrent

/** Guava Partially.GwtIncompatible marker retained as annotation. */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class GwtIncompatible(val value: String = "")

object Partially {
    annotation class GwtIncompatible(val value: String = "")
}
