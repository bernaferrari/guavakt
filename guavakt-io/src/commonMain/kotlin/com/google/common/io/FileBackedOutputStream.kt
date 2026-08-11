package dev.guavakt.io

import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer

/**
 * A byte accumulator that keeps small values in memory and spills larger values to [spillFile].
 *
 * Supplying [fileSystem] and a unique [spillFile] enables real file-backed behavior on every
 * target supported by that Okio filesystem. Without them, this remains a memory-only accumulator;
 * it never silently assumes a system temp directory. [resetOnFinalize] is retained for source
 * compatibility but deliberately has no effect: portable finalization is not a resource-cleanup
 * mechanism, so callers must use [reset] explicitly.
 */
class FileBackedOutputStream private constructor(
    private val fileThreshold: Int,
    @Suppress("unused") private val resetOnFinalize: Boolean = false,
    private val fileSystem: FileSystem? = null,
    private val spillFile: Path? = null,
) {
    private var memory: ByteArrayOutputLike? = ByteArrayOutputLike()
    private var fileSink: BufferedSink? = null
    private var spilledPath: Path? = null
    private var count = 0

    private val byteSource = object : ByteSource() {
        override fun openStream(): ByteArrayInputLike = ByteArrayInputLike(currentBytes())

        override fun openSource(): Source {
            val path = spilledPath
            if (path != null) {
                closeFileSinkForRead()
                return requireNotNull(fileSystem).source(path)
            }
            return Buffer().write(requireNotNull(memory).toByteArray())
        }

        override fun sizeIfKnown(): Long = count.toLong()
    }

    init {
        require(fileThreshold >= 0) { "fileThreshold must be non-negative: $fileThreshold" }
        require((fileSystem == null) == (spillFile == null)) {
            "fileSystem and spillFile must be supplied together"
        }
    }

    /** Creates a memory-only accumulator, preserving GuavaKt's original constructor ABI. */
    constructor(fileThreshold: Int, resetOnFinalize: Boolean = false) :
        this(fileThreshold, resetOnFinalize, null, null)

    /**
     * Creates a portable file-backed accumulator using a caller-owned unique [spillFile].
     *
     * The file's parent must already exist in [fileSystem].
     */
    constructor(
        fileThreshold: Int,
        fileSystem: FileSystem,
        spillFile: Path,
        resetOnFinalize: Boolean = false,
    ) : this(fileThreshold, resetOnFinalize, fileSystem, spillFile)

    fun write(b: Int) {
        maybeSpill(1)
        if (spilledPath != null) activeFileSink().writeByte(b) else requireNotNull(memory).write(b)
        count++
    }

    fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)

    fun write(bytes: ByteArray, off: Int, len: Int) {
        require(off >= 0 && len >= 0 && off <= bytes.size - len) { "invalid offset/length" }
        if (len == 0) return
        maybeSpill(len)
        if (spilledPath != null) activeFileSink().write(bytes, off, len) else requireNotNull(memory).write(bytes, off, len)
        count += len
    }

    /** A cached, live view that observes later writes and [reset] calls. */
    fun asByteSource(): ByteSource = byteSource

    /** Flushes a currently spilled file without closing this accumulator. */
    fun flush() {
        fileSink?.flush()
    }

    /** Closes the active file sink, if any. Memory-backed streams remain reusable as in Guava. */
    fun close() {
        fileSink?.close()
        fileSink = null
    }

    /** Clears accumulated data and deletes a spilled file through the injected filesystem. */
    fun reset() {
        val path = spilledPath
        try {
            fileSink?.close()
        } finally {
            fileSink = null
            if (path != null) requireNotNull(fileSystem).delete(path, mustExist = false)
            spilledPath = null
            memory = ByteArrayOutputLike()
            count = 0
        }
    }

    /** Number of bytes currently held by this accumulator. */
    fun getCount(): Int = count

    /** The current temporary file, or null when this instance is memory-backed. */
    fun spilledPathOrNull(): Path? = spilledPath

    private fun maybeSpill(nextWriteSize: Int) {
        if (memory == null || count.toLong() + nextWriteSize <= fileThreshold) return
        val fs = fileSystem ?: return
        val path = requireNotNull(spillFile)
        val sink = fs.sink(path, mustCreate = true).buffer()
        try {
            sink.write(requireNotNull(memory).toByteArray())
            sink.flush()
        } catch (failure: Throwable) {
            try {
                sink.close()
            } finally {
                fs.delete(path, mustExist = false)
            }
            throw failure
        }
        fileSink = sink
        spilledPath = path
        memory = null
    }

    private fun currentBytes(): ByteArray {
        val path = spilledPath
        if (path != null) {
            closeFileSinkForRead()
            return requireNotNull(fileSystem).read(path) { readByteArray() }
        }
        return requireNotNull(memory).toByteArray()
    }

    /** FakeFileSystem, like several KMP backends, requires a writer to close before a reader opens. */
    private fun closeFileSinkForRead() {
        fileSink?.close()
        fileSink = null
    }

    private fun activeFileSink(): BufferedSink {
        fileSink?.let { return it }
        val sink = requireNotNull(fileSystem).appendingSink(requireNotNull(spilledPath), mustExist = true).buffer()
        fileSink = sink
        return sink
    }
}
