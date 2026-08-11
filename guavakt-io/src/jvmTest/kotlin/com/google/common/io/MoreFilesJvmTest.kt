package dev.guavakt.io

import java.nio.file.Files
import java.nio.file.Path
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
            val breadth = MoreFiles.fileTraverser().breadthFirst(root.toString()).map { java.nio.file.Path.of(it) }
            val depth = MoreFiles.fileTraverser().depthFirstPreOrder(root.toString()).map { java.nio.file.Path.of(it) }
            assertEquals(setOf(root, child, child.resolve("file.txt")), breadth.toSet())
            assertEquals(breadth.toSet(), depth.toSet())
            assertTrue(MoreFiles.isDirectory(child.toString()))
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test fun createParentDirectoriesTouchesTheFilesystem() {
        val root = Files.createTempDirectory("guavakt-more-files-parent")
        try {
            val target = root.resolve("one/two/file.txt")
            val created = MoreFiles.createParentDirectories(target.toString())
            assertTrue(Files.isDirectory(Path.of(created)))
            assertTrue(Files.isDirectory(root.resolve("one/two")))
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
