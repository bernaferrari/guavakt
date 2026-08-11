package dev.guavakt.util.concurrent

/** Guava: Error wrapping Error from executing tasks. */
open class ExecutionError : Error {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
