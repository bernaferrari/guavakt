package dev.guavakt.reflect

/** Guava Types — type manipulation utilities (string descriptors on KMP). */
object Types {
    fun newArrayType(componentType: String): String = "$componentType[]"

    fun newParameterizedType(rawType: String, vararg arguments: String): String =
        rawType + arguments.joinToString(prefix = "<", postfix = ">", separator = ", ")

    fun subtypeOf(upperBound: String): String = "? extends $upperBound"
    fun supertypeOf(lowerBound: String): String = "? super $lowerBound"

    fun getComponentType(arrayType: String): String? =
        if (arrayType.endsWith("[]")) arrayType.removeSuffix("[]") else null
}
