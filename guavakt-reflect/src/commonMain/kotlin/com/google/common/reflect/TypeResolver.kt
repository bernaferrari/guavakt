package dev.guavakt.reflect

/**
 * Guava TypeResolver — resolves type variables in a context of mappings.
 * KMP simplified: string-keyed type variable map for API shape.
 */
class TypeResolver {
    private val map = LinkedHashMap<String, String>()

    fun where(formal: String, actual: String): TypeResolver {
        val copy = TypeResolver()
        copy.map.putAll(map)
        copy.map[formal] = actual
        return copy
    }

    fun resolveType(typeVarName: String): String = map[typeVarName] ?: typeVarName

    fun resolveAll(): Map<String, String> = map.toMap()
}
