package dev.guavakt.util.concurrent

/** Guava LazyLogger — defers logger creation; KMP records to in-memory list. */
internal class LazyLogger(private val owner: Any) {
    private val messages = ArrayList<String>()
    fun log(message: String) { messages.add(message) }
    fun log(message: String, t: Throwable) { messages.add("$message: ${t.message}") }
    fun messages(): List<String> = messages.toList()
}
