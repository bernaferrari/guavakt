package dev.guavakt.util.concurrent

/** Guava: RuntimeException wrapping checked failures from executing tasks. */
open class UncheckedExecutionException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
