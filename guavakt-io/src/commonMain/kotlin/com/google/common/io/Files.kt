package dev.guavakt.io

import dev.guavakt.base.Preconditions
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer

/**
 * Guava Files — KMP helpers on in-memory byte/char content and path strings
 * (no java.io.File on all targets).
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

    /** JVM: reads file via NIO; non-JVM: throws. */
    fun readAllBytes(path: String): ByteArray = platformReadAllBytes(path)

    /** Reads [path] from an injected Okio filesystem on every target supported by that filesystem. */
    fun readAllBytes(fileSystem: FileSystem, path: Path): ByteArray =
        fileSystem.read(path) { readByteArray() }

    /** JVM: writes file via NIO; non-JVM: throws. */
    fun write(path: String, bytes: ByteArray) = platformWriteBytes(path, bytes)

    /** Writes [bytes] through an injected Okio filesystem. */
    fun write(fileSystem: FileSystem, path: Path, bytes: ByteArray, mustCreate: Boolean = false) {
        fileSystem.write(path, mustCreate) { write(bytes) }
    }

    fun createTempDir(prefix: String = "guavakt"): String = platformCreateTempDir(prefix)

    /**
     * Guava Files.asByteSource — path-backed [ByteSource] (JVM filesystem; other targets throw on read).
     */
    fun asByteSource(path: String): ByteSource = object : ByteSource() {
        override fun openStream(): ByteArrayInputLike =
            ByteArrayInputLike(platformReadAllBytes(path))
        override fun openSource(): Source = platformSource(path)
        override fun sizeIfKnown(): Long? = null
    }

    /** Okio-backed byte source for common code and testable/in-memory filesystems. */
    fun asByteSource(fileSystem: FileSystem, path: Path): ByteSource = object : ByteSource() {
        override fun openStream(): ByteArrayInputLike = ByteArrayInputLike(readAllBytes(fileSystem, path))
        override fun openSource(): Source = fileSystem.source(path)
        override fun sizeIfKnown(): Long? = fileSystem.metadataOrNull(path)?.size
    }

    /**
     * Guava Files.asCharSource — path-backed [CharSource] using portable UTF-8 only.
     *
     * Common code deliberately rejects other charset names instead of silently decoding them as
     * UTF-8. JVM-specific charset bridges can be added without weakening this KMP contract.
     */
    fun asCharSource(path: String, charsetName: String = "UTF-8"): CharSource {
        require(charsetName.equals("UTF-8", ignoreCase = true) || charsetName.equals("UTF8", ignoreCase = true)) {
            "Only UTF-8 CharSource is supported on common; got $charsetName"
        }
        return object : CharSource() {
        override fun openStream(): CharReaderLike {
            val bytes = platformReadAllBytes(path)
            val text = bytes.decodeToString()
            return CharReaderLike(text)
        }
        override fun openReader(): CharReaderLike =
            CharReaderLike.fromUtf8(platformSource(path).buffer())
        }
    }

    /** Okio-backed UTF-8 character source. */
    fun asCharSource(fileSystem: FileSystem, path: Path): CharSource = object : CharSource() {
        override fun openStream(): CharReaderLike =
            CharReaderLike(fileSystem.read(path) { readUtf8() })
        override fun openReader(): CharReaderLike =
            CharReaderLike.fromUtf8(fileSystem.source(path).buffer())
    }

    /** Guava Files.asByteSink — an Okio-backed JVM string-path migration API. */
    fun asByteSink(path: String): ByteSink = object : ByteSink() {
        override fun openSink(): Sink = platformSink(path)
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

    /** UTF-8 character sink using the legacy platform path bridge. */
    fun asCharSink(path: String): CharSink = asByteSink(path).asCharSink()
}
