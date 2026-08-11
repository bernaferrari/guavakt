package dev.guavakt.io

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files as JvmFiles
import java.nio.file.Path
import okio.FileSystem
import okio.Sink
import okio.Source
import okio.buffer
import okio.Path.Companion.toPath

/**
 * JVM-only `java.nio.file.Path` conveniences for [Files].
 *
 * These retain an ergonomic bridge for Kotlin/JVM migrations without leaking a string path API
 * into common code. New multiplatform code must use the [Files] overloads that accept an injected
 * Okio [FileSystem] and `okio.Path`.
 */
fun Files.readAllBytes(path: Path): ByteArray = JvmFiles.readAllBytes(path)

fun Files.write(path: Path, bytes: ByteArray) {
    JvmFiles.write(path, bytes)
}

/** JVM equivalent of Guava's `Files.createTempDir`; no common system-temp-directory API exists. */
fun Files.createTempDir(prefix: String = "guavakt"): Path =
    JvmFiles.createTempDirectory(prefix)

fun Files.asByteSource(path: Path): ByteSource = object : ByteSource() {
    override fun openStream(): ByteArrayInputLike = ByteArrayInputLike(JvmFiles.readAllBytes(path))
    override fun openSource(): Source = FileSystem.SYSTEM.source(path.toString().toPath())
    override fun sizeIfKnown(): Long? = JvmFiles.size(path)
}

/**
 * JVM-only character source. Unlike the common UTF-8 API, this bridge honors [charset].
 */
fun Files.asCharSource(path: Path, charset: Charset = StandardCharsets.UTF_8): CharSource = object : CharSource() {
    override fun openStream(): CharReaderLike = CharReaderLike(JvmFiles.readString(path, charset))
    override fun openReader(): CharReaderLike {
        if (charset == StandardCharsets.UTF_8) {
            return CharReaderLike.fromUtf8(FileSystem.SYSTEM.source(path.toString().toPath()).buffer())
        }
        return CharReaderLike(JvmFiles.readString(path, charset))
    }
}

fun Files.asByteSink(path: Path): ByteSink = object : ByteSink() {
    override fun openSink(): Sink = FileSystem.SYSTEM.sink(path.toString().toPath())
}

fun Files.asCharSink(path: Path): CharSink = asByteSink(path).asCharSink()
