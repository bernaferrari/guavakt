package dev.guavakt.escape

/**
 * Immutable, shareable lookup storage for array-backed escapers.
 *
 * Storage is proportional to the greatest mapped UTF-16 character. Prefer sharing one instance
 * when several escapers use a large or high-valued replacement map.
 */
class ArrayBasedEscaperMap private constructor(
    private val replacements: Array<CharArray?>,
) {
    internal fun replacementArray(): Array<CharArray?> = replacements

    companion object {
        fun create(replacements: Map<Char, String>): ArrayBasedEscaperMap {
            val maximum = replacements.keys.maxOfOrNull(Char::code)
                ?: return ArrayBasedEscaperMap(emptyArray())
            val table = arrayOfNulls<CharArray>(maximum + 1)
            replacements.forEach { (character, replacement) ->
                table[character.code] = replacement.toCharArray()
            }
            return ArrayBasedEscaperMap(table)
        }
    }
}
