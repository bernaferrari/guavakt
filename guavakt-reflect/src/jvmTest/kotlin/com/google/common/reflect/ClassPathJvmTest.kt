package dev.guavakt.reflect

import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClassPathJvmTest {
    @Test
    fun fromScansTheRequestedLoaderDirectoryForClassesAndResources() {
        val root = Files.createTempDirectory("guavakt-classpath")
        try {
            val classFile = root.resolve("sample/Visible.class")
            Files.createDirectories(classFile.parent)
            Files.write(classFile, byteArrayOf())
            val resource = root.resolve("sample/config.properties")
            Files.write(resource, byteArrayOf())

            URLClassLoader(arrayOf(root.toUri().toURL()), null).use { loader ->
                val classPath = ClassPath.from(loader)
                assertTrue(classPath.getAllClasses().any { it.className == "sample.Visible" })
                assertTrue(classPath.getResources().any { it.resourceName == "sample/config.properties" })
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun initializePropagatesStaticInitializationFailure() {
        assertFailsWith<ExceptionInInitializerError> {
            Reflection.initialize(BrokenInitialization::class)
        }
    }

    private class BrokenInitialization {
        companion object {
            init {
                error("expected initialization failure")
            }
        }
    }
}
