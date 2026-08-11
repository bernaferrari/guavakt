package dev.guavakt.reflect

/**
 * Guava Parameter — invokable parameter metadata (name/type/annotations).
 */
class Parameter internal constructor(
    val name: String?,
    val typeName: String,
    val position: Int,
    val annotations: List<Annotation> = emptyList(),
) {
    fun isAnnotationPresent(annotationClass: kotlin.reflect.KClass<out Annotation>): Boolean =
        annotations.any { annotationClass.isInstance(it) }

    inline fun <reified A : Annotation> getAnnotation(): A? =
        annotations.filterIsInstance<A>().firstOrNull()

    override fun toString(): String = typeName + (name?.let { " $it" } ?: "")
}
