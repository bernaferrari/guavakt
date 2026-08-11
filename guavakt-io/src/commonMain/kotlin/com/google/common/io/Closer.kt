package dev.guavakt.io

/**
 * Guava Closer — suppresses secondary exceptions when closing multiple resources.
 */
class Closer private constructor() {
    private val stack = ArrayDeque<AutoCloseable>()
    private var thrown: Throwable? = null

    fun <C : AutoCloseable> register(closeable: C): C {
        stack.addFirst(closeable)
        return closeable
    }

    fun rethrow(e: Exception): RuntimeException {
        thrown = e
        throw e
    }

    fun <X : Exception> rethrow(e: Exception, declaredType: Any): Nothing {
        thrown = e
        throw e
    }

    fun close() {
        var throwable = thrown
        while (stack.isNotEmpty()) {
            val c = stack.removeFirst()
            try {
                c.close()
            } catch (e: Throwable) {
                if (throwable == null) throwable = e
                // else suppress
            }
        }
        if (thrown == null && throwable != null) {
            when (throwable) {
                is Exception -> throw throwable
                is Error -> throw throwable
                else -> throw RuntimeException(throwable)
            }
        }
    }

    companion object {
        fun create(): Closer = Closer()
    }
}
