package com.bernaferrari.guavakt.collect

open class ComputationException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    fun hasMessage(): Boolean = message != null
}
