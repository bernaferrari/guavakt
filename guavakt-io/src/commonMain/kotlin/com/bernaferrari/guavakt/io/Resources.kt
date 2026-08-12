package com.bernaferrari.guavakt.io

/**
 * Guava Resources — classpath resource helpers.
 * KMP: registry-based resources (no ClassLoader); apps register named resources.
 */
object Resources {
    private val registry = LinkedHashMap<String, ByteArray>()

    fun register(name: String, bytes: ByteArray) {
        registry[name] = bytes
    }

    fun getResource(resourceName: String): ByteSource {
        val bytes = registry[resourceName]
            ?: throw IllegalArgumentException("resource $resourceName not found")
        return ByteSource.wrap(bytes)
    }

    fun toByteArray(resourceName: String): ByteArray = getResource(resourceName).read()

    fun toString(resourceName: String, charsetName: String = "UTF-8"): String {
        val bytes = toByteArray(resourceName)
        return bytes.decodeToString() // UTF-8 KMP default
    }

    fun readLines(resourceName: String): List<String> =
        CharSource.wrap(toString(resourceName)).readLines()
}
