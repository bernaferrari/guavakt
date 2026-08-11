package dev.guavakt.io

import java.nio.file.Files as JvmFiles
import java.nio.file.Path
import java.util.ArrayDeque

/**
 * JVM-only `java.nio.file.Path` conveniences for [MoreFiles].
 *
 * Common code should use the [MoreFiles] overloads that take an injected Okio filesystem instead.
 */
fun MoreFiles.FileTraverser.breadthFirst(root: Path): List<Path> {
    val result = ArrayList<Path>()
    val queue = ArrayDeque<Path>()
    queue.add(root)
    while (queue.isNotEmpty()) {
        val path = queue.removeFirst()
        result.add(path)
        if (JvmFiles.isDirectory(path)) {
            JvmFiles.newDirectoryStream(path).use { children -> children.forEach(queue::addLast) }
        }
    }
    return result
}

fun MoreFiles.FileTraverser.depthFirstPreOrder(root: Path): List<Path> =
    JvmFiles.walk(root).use { stream ->
        buildList {
            val iterator = stream.iterator()
            while (iterator.hasNext()) add(iterator.next())
        }
    }

/** Creates missing parents and returns [path], matching the common Okio overload. */
fun MoreFiles.createParentDirectories(path: Path): Path {
    path.toAbsolutePath().parent?.let(JvmFiles::createDirectories)
    return path
}

fun MoreFiles.isDirectory(path: Path): Boolean = JvmFiles.isDirectory(path)
