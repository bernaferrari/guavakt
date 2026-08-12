package com.bernaferrari.guavakt.graph

/**
 * Guava / Error Prone IgnoreJRERequirement — suppresses Android desugaring checks.
 * No-op marker on KMP.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class IgnoreJRERequirement
