package dev.guavakt.reflect

/**
 * Guava TypeVisitor — visits type structure (class, parameterized, array, variable, wildcard).
 * KMP uses string descriptors.
 */
abstract class TypeVisitor {
    private val visited = LinkedHashSet<String>()

    fun visit(vararg types: String) {
        for (type in types) {
            if (!visited.add(type)) continue
            try {
                when {
                    type.endsWith("[]") -> visitArray(type)
                    type.contains("<") -> visitParameterized(type)
                    type.startsWith("?") -> visitWildcard(type)
                    type.length == 1 && type[0].isUpperCase() -> visitVariable(type)
                    else -> visitClass(type)
                }
            } catch (e: Exception) {
                visited.remove(type)
                throw e
            }
        }
    }

    protected open fun visitClass(t: String) {}
    protected open fun visitParameterized(t: String) {}
    protected open fun visitArray(t: String) {}
    protected open fun visitVariable(t: String) {}
    protected open fun visitWildcard(t: String) {}
}
