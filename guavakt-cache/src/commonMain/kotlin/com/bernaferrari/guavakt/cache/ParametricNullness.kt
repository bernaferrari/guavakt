package com.bernaferrari.guavakt.cache

/**
 * Guava ParametricNullness marker — documents type-parameter nullness for static analysis.
 * On KMP this is a source-level annotation with no runtime effect.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
)
annotation class ParametricNullness
