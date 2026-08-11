package dev.guavakt.io

/** Guava AppendableWriter — Writer that appends to an Appendable (StringBuilder). */
class AppendableWriter(private val target: Appendable) : AutoCloseable {
    private var closed = false
    private fun ensureOpen() { check(!closed) }
    fun write(cbuf: CharArray, off: Int, len: Int) {
        ensureOpen()
        for (i in off until off + len) target.append(cbuf[i])
    }
    fun write(c: Int) { ensureOpen(); target.append(c.toChar()) }
    fun write(str: String) { ensureOpen(); target.append(str) }
    fun append(csq: CharSequence?): AppendableWriter {
        ensureOpen(); target.append(csq ?: "null"); return this
    }
    override fun close() { closed = true }
}
