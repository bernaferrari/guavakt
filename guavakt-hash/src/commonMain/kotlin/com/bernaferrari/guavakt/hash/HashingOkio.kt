package com.bernaferrari.guavakt.hash

import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout

/**
 * An Okio [Source] that updates a fresh hasher for [hashFunction] with exactly the bytes read.
 *
 * This is the Kotlin Multiplatform counterpart to Guava's `HashingInputStream`: it composes with
 * Okio filesystems, sockets, and buffers without exposing `java.io.InputStream`. Closing this
 * wrapper closes [source]. Calling [hash] finalizes the accumulated prefix; do not read further
 * after doing so.
 */
class HashingSource(
    private val source: Source,
    hashFunction: HashFunction,
) : Source {
    private val hasher = hashFunction.newHasher()

    override fun read(sink: Buffer, byteCount: Long): Long {
        val startOffset = sink.size
        val read = source.read(sink, byteCount)
        if (read > 0L) {
            val copied = Buffer()
            sink.copyTo(copied, startOffset, read)
            hasher.putBytes(copied.readByteArray())
        }
        return read
    }

    /** Finalizes and returns the hash of bytes successfully returned by [read]. */
    fun hash(): HashCode = hasher.hash()

    override fun timeout(): Timeout = source.timeout()

    override fun close() = source.close()
}

/**
 * An Okio [Sink] that updates a fresh hasher for [hashFunction] with exactly the bytes written.
 *
 * This is the Kotlin Multiplatform counterpart to Guava's `HashingOutputStream`. The hash is
 * updated before delegating a write, matching Guava's observable behavior when the downstream
 * sink fails. Closing this wrapper closes [sink].
 */
class HashingSink(
    private val sink: Sink,
    hashFunction: HashFunction,
) : Sink {
    private val hasher = hashFunction.newHasher()

    override fun write(source: Buffer, byteCount: Long) {
        val copied = Buffer()
        source.copyTo(copied, 0L, byteCount)
        hasher.putBytes(copied.readByteArray())
        sink.write(source, byteCount)
    }

    /** Finalizes and returns the hash of bytes accepted for writing. */
    fun hash(): HashCode = hasher.hash()

    override fun flush() = sink.flush()

    override fun timeout(): Timeout = sink.timeout()

    override fun close() = sink.close()
}

/** Wraps this source in a KMP-native hashing source. */
fun Source.hashing(hashFunction: HashFunction): HashingSource = HashingSource(this, hashFunction)

/** Wraps this sink in a KMP-native hashing sink. */
fun Sink.hashing(hashFunction: HashFunction): HashingSink = HashingSink(this, hashFunction)
