package dev.guavakt.io
import okio.Sink
import okio.Source

actual fun platformReadAllBytes(path: String): ByteArray =
    throw UnsupportedOperationException("platformReadAllBytes requires JVM filesystem")

actual fun platformWriteBytes(path: String, bytes: ByteArray) {
    throw UnsupportedOperationException("platformWriteBytes requires JVM filesystem")
}
actual fun platformSource(path: String): Source = throw UnsupportedOperationException("Legacy string paths are JVM-only; inject an Okio FileSystem")
actual fun platformSink(path: String): Sink = throw UnsupportedOperationException("Legacy string paths are JVM-only; inject an Okio FileSystem")

actual fun platformCreateTempDir(prefix: String): String =
    throw UnsupportedOperationException("platformCreateTempDir requires JVM filesystem")

actual fun platformCreateParentDirectories(path: String): String =
    throw UnsupportedOperationException("platformCreateParentDirectories requires JVM filesystem")

actual fun platformTraverse(root: String, breadthFirst: Boolean): List<String> =
    throw UnsupportedOperationException("platformTraverse requires JVM filesystem")

actual fun platformIsDirectory(path: String): Boolean =
    throw UnsupportedOperationException("platformIsDirectory requires JVM filesystem")
