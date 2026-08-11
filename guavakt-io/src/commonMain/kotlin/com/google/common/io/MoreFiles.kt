package dev.guavakt.io

import okio.FileSystem
import okio.Path

/**
 * Kotlin-first filesystem helpers backed by an explicitly supplied Okio [FileSystem].
 *
 * The name helpers are pure string utilities. Storage operations deliberately require an Okio
 * [FileSystem] and [Path] on every target.
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
        /** Breadth-first traversal through the supplied filesystem. */
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

        /** Depth-first pre-order traversal through the supplied filesystem. */
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

    /** Creates every missing parent directory through the supplied filesystem. */
    fun createParentDirectories(fileSystem: FileSystem, path: Path): Path {
        path.parent?.let(fileSystem::createDirectories)
        return path
    }

    fun isDirectory(fileSystem: FileSystem, path: Path): Boolean =
        fileSystem.metadataOrNull(path)?.isDirectory == true
}
