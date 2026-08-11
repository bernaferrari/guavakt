package dev.guavakt.io

import dev.guavakt.base.Preconditions
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer

/**
 * Kotlin-first file helpers backed by an explicitly supplied Okio [FileSystem].
 *
 * Every operation that accesses storage takes both a [FileSystem] and a [Path]. That makes the
 * storage boundary explicit in common code: a caller can choose a system, sandboxed, browser, or
 * in-memory filesystem without compiling an API that will fail on another target.
 */
object Files {
    fun getFileExtension(fullName: String): String {
        Preconditions.checkNotNull(fullName)
        val fileName = fullName.substringAfterLast('/').substringAfterLast('\\')
        val dot = fileName.lastIndexOf('.')
        return if (dot == -1) "" else fileName.substring(dot + 1)
    }

    fun getNameWithoutExtension(file: String): String {
        Preconditions.checkNotNull(file)
        val fileName = file.substringAfterLast('/').substringAfterLast('\\')
        val dot = fileName.lastIndexOf('.')
        return if (dot == -1) fileName else fileName.substring(0, dot)
    }

    fun simplifyPath(pathname: String): String {
        Preconditions.checkNotNull(pathname)
        if (pathname.isEmpty()) return "."
        val parts = pathname.split('/').filter { it.isNotEmpty() && it != "." }
        val stack = ArrayList<String>()
        for (p in parts) {
            if (p == "..") {
                if (stack.isNotEmpty() && stack.last() != "..") stack.removeAt(stack.lastIndex)
                else if (!pathname.startsWith('/')) stack.add("..")
            } else stack.add(p)
        }
        val joined = stack.joinToString("/")
        return when {
            pathname.startsWith("/") -> "/$joined"
            joined.isEmpty() -> "."
            else -> joined
        }
    }

    fun toByteArray(bytes: ByteArray): ByteArray = bytes.copyOf()
    fun write(from: ByteArray, to: MutableList<Byte>) {
        for (b in from) to.add(b)
    }

    /** Reads [path] on every target supported by the supplied [fileSystem]. */
    fun readAllBytes(fileSystem: FileSystem, path: Path): ByteArray =
        fileSystem.read(path) { readByteArray() }

    /** Writes [bytes] through an injected Okio filesystem. */
    fun write(fileSystem: FileSystem, path: Path, bytes: ByteArray, mustCreate: Boolean = false) {
        fileSystem.write(path, mustCreate) { write(bytes) }
    }

    /** Okio-backed byte source for common code and testable/in-memory filesystems. */
    fun asByteSource(fileSystem: FileSystem, path: Path): ByteSource = object : ByteSource() {
        override fun openStream(): ByteArrayInputLike = ByteArrayInputLike(readAllBytes(fileSystem, path))
        override fun openSource(): Source = fileSystem.source(path)
        override fun sizeIfKnown(): Long? = fileSystem.metadataOrNull(path)?.size
    }

    /** Okio-backed UTF-8 character source. */
    fun asCharSource(fileSystem: FileSystem, path: Path): CharSource = object : CharSource() {
        override fun openStream(): CharReaderLike =
            CharReaderLike(fileSystem.read(path) { readUtf8() })
        override fun openReader(): CharReaderLike =
            CharReaderLike.fromUtf8(fileSystem.source(path).buffer())
    }

    /** Okio-backed byte sink for common code and testable/in-memory filesystems. */
    fun asByteSink(
        fileSystem: FileSystem,
        path: Path,
        mustCreate: Boolean = false,
    ): ByteSink = object : ByteSink() {
        override fun openSink(): Sink = fileSystem.sink(path, mustCreate)
    }

    fun asCharSink(fileSystem: FileSystem, path: Path, mustCreate: Boolean = false): CharSink =
        asByteSink(fileSystem, path, mustCreate).asCharSink()
}
