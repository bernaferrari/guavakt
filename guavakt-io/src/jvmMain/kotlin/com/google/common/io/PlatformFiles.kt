package dev.guavakt.io

import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import okio.FileSystem
import okio.Sink
import okio.Source
import okio.Path.Companion.toPath

actual fun platformReadAllBytes(path: String): ByteArray =
    Files.readAllBytes(Path.of(path))

actual fun platformWriteBytes(path: String, bytes: ByteArray) {
    Files.write(Path.of(path), bytes)
}

actual fun platformSource(path: String): Source = FileSystem.SYSTEM.source(path.toPath())

actual fun platformSink(path: String): Sink = FileSystem.SYSTEM.sink(path.toPath())

actual fun platformCreateTempDir(prefix: String): String =
    Files.createTempDirectory(prefix).toAbsolutePath().toString()

actual fun platformCreateParentDirectories(path: String): String {
    val parent = Path.of(path).toAbsolutePath().parent
        ?: throw IllegalArgumentException("Path has no parent: $path")
    return Files.createDirectories(parent).toString()
}

actual fun platformTraverse(root: String, breadthFirst: Boolean): List<String> {
    val rootPath = Path.of(root)
    val result = ArrayList<String>()
    if (!breadthFirst) {
        Files.walk(rootPath).use { stream ->
            val iterator = stream.iterator()
            while (iterator.hasNext()) result.add(iterator.next().toString())
        }
        return result
    }
    val queue = ArrayDeque<Path>()
    queue.add(rootPath)
    while (queue.isNotEmpty()) {
        val path = queue.removeFirst()
        result.add(path.toString())
        if (Files.isDirectory(path)) {
            Files.newDirectoryStream(path).use { children -> children.forEach(queue::addLast) }
        }
    }
    return result
}

actual fun platformIsDirectory(path: String): Boolean = Files.isDirectory(Path.of(path))
