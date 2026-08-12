package com.bernaferrari.guavakt.annotations
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class GwtCompatible(val serializable: Boolean = false, val emulated: Boolean = false)
