package dev.guavakt.io

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoreFilesJvmTest {
    @Test fun traversalVisitsDescendantsAndDirectoryCheckUsesFilesystem() {
        val root = Files.createTempDirectory("guavakt-more-files")
        try {
            val child = Files.createDirectory(root.resolve("child"))
            child.resolve("file.txt").writeText("value")
            val breadth = MoreFiles.fileTraverser().breadthFirst(root)
            val depth = MoreFiles.fileTraverser().depthFirstPreOrder(root)
            assertEquals(setOf(root, child, child.resolve("file.txt")), breadth.toSet())
            assertEquals(breadth.toSet(), depth.toSet())
            assertTrue(MoreFiles.isDirectory(child))
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test fun createParentDirectoriesTouchesTheFilesystem() {
        val root = Files.createTempDirectory("guavakt-more-files-parent")
        try {
            val target = root.resolve("one/two/file.txt")
            val created = MoreFiles.createParentDirectories(target)
            assertTrue(Files.isDirectory(created.parent))
            assertTrue(Files.isDirectory(root.resolve("one/two")))
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
