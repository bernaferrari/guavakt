package dev.guavakt.io

/** Guava CountingInputStream — counts bytes read. */
class CountingInputStream(private val input: ByteArrayInputLike) : AutoCloseable {
    private var count = 0L
    fun getCount(): Long = count
    fun read(buf: ByteArray): Int {
        val n = input.read(buf)
        if (n > 0) count += n
        return n
    }
    fun read(): Int {
        val b = input.read()
        if (b >= 0) count++
        return b
    }
    fun skip(n: Long): Long {
        val buf = ByteArray(minOf(n, 8192L).toInt())
        var skipped = 0L
        while (skipped < n) {
            val r = read(buf)
            if (r < 0) break
            skipped += r
        }
        return skipped
    }
    override fun close() = input.close()
}
