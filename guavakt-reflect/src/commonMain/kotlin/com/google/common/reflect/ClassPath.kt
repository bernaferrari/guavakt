package dev.guavakt.reflect

/**
 * Guava ClassPath — discovers classes/resources.
 * Common: explicit [registerClass] / [registerResource] registry (portable).
 * JVM: [from] recognizes a `ClassLoader` marker and scans it; [fromPlatformClassLoader] scans
 * the current context loader. Other targets return the portable registry only.
 */
class ClassPath private constructor(
    private val resources: Set<ResourceInfo>,
) {
    fun getResources(): Set<ResourceInfo> = resources
    fun getAllClasses(): Set<ClassInfo> = resources.filterIsInstance<ClassInfo>().toSet()
    fun getTopLevelClasses(): Set<ClassInfo> =
        getAllClasses().filter { !it.className.contains('$') }.toSet()
    fun getTopLevelClasses(packageName: String): Set<ClassInfo> =
        getTopLevelClasses().filter { it.packageName == packageName }.toSet()
    fun getTopLevelClassesRecursive(packageName: String): Set<ClassInfo> =
        getTopLevelClasses().filter {
            it.packageName == packageName || it.packageName.startsWith("$packageName.")
        }.toSet()

    open class ResourceInfo internal constructor(val resourceName: String) {
        override fun toString(): String = resourceName
        override fun equals(other: Any?): Boolean =
            other is ResourceInfo && resourceName == other.resourceName
        override fun hashCode(): Int = resourceName.hashCode()
    }

    class ClassInfo internal constructor(
        resourceName: String,
        val className: String,
    ) : ResourceInfo(resourceName) {
        val packageName: String
            get() {
                val dot = className.lastIndexOf('.')
                return if (dot < 0) "" else className.substring(0, dot)
            }
        val simpleName: String
            get() {
                val dollar = className.lastIndexOf('$')
                val dot = className.lastIndexOf('.')
                return className.substring(maxOf(dollar, dot) + 1)
            }
        override fun toString(): String = className
    }

    companion object {
        private val registered = LinkedHashSet<ResourceInfo>()

        fun registerClass(className: String) {
            val resource = className.replace('.', '/') + ".class"
            registered.add(ClassInfo(resource, className))
        }

        fun registerResource(resourceName: String) {
            registered.add(ResourceInfo(resourceName))
        }

        /**
         * Discovers resources from [classLoaderMarker] on JVM when it is a `ClassLoader`.
         * Other targets, and non-loader markers, return the portable explicit registry.
         */
        fun from(classLoaderMarker: Any? = null): ClassPath {
            val scanned = platformScanClassPath(classLoaderMarker)
            return ClassPath((registered + scanned).toSet())
        }

        /** JVM: scan class loader URLs; other targets: same as [from]. */
        fun fromPlatformClassLoader(): ClassPath = from(null)
    }
}

internal expect fun platformScanClassPath(classLoaderMarker: Any?): Set<ClassPath.ResourceInfo>
