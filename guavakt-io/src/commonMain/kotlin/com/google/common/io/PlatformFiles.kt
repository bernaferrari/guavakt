package dev.guavakt.io

import okio.Sink
import okio.Source

/** Platform filesystem bytes — full Guava Files I/O on JVM. */
expect fun platformReadAllBytes(path: String): ByteArray
expect fun platformWriteBytes(path: String, bytes: ByteArray)
expect fun platformSource(path: String): Source
expect fun platformSink(path: String): Sink
expect fun platformCreateTempDir(prefix: String): String
expect fun platformCreateParentDirectories(path: String): String
expect fun platformTraverse(root: String, breadthFirst: Boolean): List<String>
expect fun platformIsDirectory(path: String): Boolean
