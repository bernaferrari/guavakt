package dev.guavakt.io

import okio.FileSystem
import okio.Path

/**
 * Guava MoreFiles — NIO Path utilities.
 * KMP: path-string oriented helpers (no java.nio.file.Path).
 */
object MoreFiles {
    fun getFileExtension(path: String): String {
        val name = path.substringAfterLast('/').substringAfterLast('\\')
        val dot = name.lastIndexOf('.')
        return if (dot < 0) "" else name.substring(dot + 1)
    }

    fun getNameWithoutExtension(path: String): String {
        val name = path.substringAfterLast('/').substringAfterLast('\\')
        val dot = name.lastIndexOf('.')
        return if (dot < 0) name else name.substring(0, dot)
    }

    fun fileTraverser(): FileTraverser = FileTraverser

    object FileTraverser {
        /** JVM filesystem traversal; unsupported targets throw instead of returning fake data. */
        fun breadthFirst(root: String): List<String> = platformTraverse(root, breadthFirst = true)
        fun depthFirstPreOrder(root: String): List<String> = platformTraverse(root, breadthFirst = false)

        fun breadthFirst(fileSystem: FileSystem, root: Path): List<Path> {
            val result = ArrayList<Path>()
            val queue = ArrayDeque<Path>()
            queue.addLast(root)
            while (queue.isNotEmpty()) {
                val path = queue.removeFirst()
                result.add(path)
                if (fileSystem.metadataOrNull(path)?.isDirectory == true) {
                    for (child in fileSystem.list(path)) queue.addLast(child)
                }
            }
            return result
        }

        fun depthFirstPreOrder(fileSystem: FileSystem, root: Path): List<Path> {
            val result = ArrayList<Path>()
            fun visit(path: Path) {
                result.add(path)
                if (fileSystem.metadataOrNull(path)?.isDirectory == true) {
                    for (child in fileSystem.list(path)) visit(child)
                }
            }
            visit(root)
            return result
        }
    }

    /** Creates every missing parent directory on JVM; unsupported on targets without filesystem access. */
    fun createParentDirectories(path: String): String = platformCreateParentDirectories(path)

    fun createParentDirectories(fileSystem: FileSystem, path: Path): Path {
        path.parent?.let(fileSystem::createDirectories)
        return path
    }

    fun isDirectory(path: String): Boolean = platformIsDirectory(path)

    fun isDirectory(fileSystem: FileSystem, path: Path): Boolean =
        fileSystem.metadataOrNull(path)?.isDirectory == true
}
