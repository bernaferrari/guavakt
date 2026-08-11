package dev.guavakt.io

/** Guava CountingOutputStream — counts bytes written. */
class CountingOutputStream(private val output: ByteArrayOutputLike) : AutoCloseable {
    private var count = 0L
    fun getCount(): Long = count
    fun write(b: Int) { output.write(b); count++ }
    fun write(bytes: ByteArray, off: Int, len: Int) {
        output.write(bytes, off, len)
        count += len
    }
    override fun close() = output.close()
}
