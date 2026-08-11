package dev.guavakt.reflect

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

internal actual fun platformScanClassPath(classLoaderMarker: Any?): Set<ClassPath.ResourceInfo> {
    val out = LinkedHashSet<ClassPath.ResourceInfo>()
    val classLoader = classLoaderMarker as? ClassLoader
        ?: Thread.currentThread().contextClassLoader
        ?: ClassLoader.getSystemClassLoader()
    val urls = LinkedHashSet<URL>()
    var current: ClassLoader? = classLoader
    while (current is URLClassLoader) {
        urls.addAll(current.urLs)
        current = current.parent
    }
    // Since Java 9 the application class loader is no longer necessarily a URLClassLoader.
    // Its class path remains the portable way to discover the ordinary application locations.
    System.getProperty("java.class.path", "")
        .split(File.pathSeparator)
        .filter(String::isNotEmpty)
        .forEach { entry -> urls += File(entry).toURI().toURL() }

    for (url in urls) {
        if (url.protocol != "file") continue
        val path = File(url.toURI())
        if (path.isDirectory) {
            path.walkTopDown().filter(File::isFile).forEach { file ->
                val resourceName = path.toPath().relativize(file.toPath()).toString()
                    .replace(File.separatorChar, '/')
                if (resourceName.endsWith(".class") && !resourceName.endsWith("module-info.class")) {
                    val rel = resourceName
                    val className = rel.removeSuffix(".class").replace('/', '.')
                    out.add(ClassPath.ClassInfo(rel, className))
                } else {
                    out.add(ClassPath.ResourceInfo(resourceName))
                }
            }
        } else if (path.isFile && path.name.endsWith(".jar")) {
            JarFile(path).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.endsWith(".class") && !entry.name.endsWith("module-info.class")) {
                        val className = entry.name.removeSuffix(".class").replace('/', '.')
                        out.add(ClassPath.ClassInfo(entry.name, className))
                    } else if (!entry.isDirectory) {
                        out.add(ClassPath.ResourceInfo(entry.name))
                    }
                }
            }
        }
    }
    return out
}
