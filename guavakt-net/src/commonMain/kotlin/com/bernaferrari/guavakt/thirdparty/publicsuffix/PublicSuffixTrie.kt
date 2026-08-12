package com.bernaferrari.guavakt.thirdparty.publicsuffix

/**
 * Guava PublicSuffixTrie — faithful port including [ChunksCharSequence] addressing (chunkShift=13).
 */
class PublicSuffixTrie(
    trieDataChunks: List<String>,
    stringPoolChunks: List<String>,
    chunkShift: Int,
) {
    private val trieData = ChunksCharSequence(trieDataChunks, chunkShift)
    private val stringPool = ChunksCharSequence(stringPoolChunks, chunkShift)

    fun findSuffixIndex(labels: List<String>): Int = findSuffixIndex(labels, null)

    fun findSuffixIndex(labels: List<String>, desiredType: PublicSuffixType?): Int {
        val partsSize = labels.size
        var nodeIndex = 0
        var bestResult = -1
        for (i in partsSize - 1 downTo 0) {
            val firstChild = trieData.charAt(nodeIndex * NODE_SIZE + 1).code
            val numChildren = trieData.charAt(nodeIndex * NODE_SIZE + 2).code and CHILDREN_MASK
            nodeIndex = findChild(firstChild, numChildren, labels[i])
            if (nodeIndex == -1) break
            val metadata = trieData.charAt(nodeIndex * NODE_SIZE + 2).code
            if (isExcludedMatch(metadata)) bestResult = i + 1
            val exactType = getExactMatchType(metadata)
            if (matchesType(desiredType, exactType)) bestResult = i
            val wildcardType = getWildcardMatchType(metadata)
            if (i > 0 && matchesType(desiredType, wildcardType)) bestResult = i - 1
        }
        return bestResult
    }

    fun getPublicSuffix(domain: String): String? = getPublicSuffix(domain, null)

    fun getPublicSuffix(domain: String, desiredType: PublicSuffixType?): String? {
        val labels = domain.lowercase().split('.').filter { it.isNotEmpty() }
        val idx = findSuffixIndex(labels, desiredType)
        if (idx < 0) return null
        return labels.subList(idx, labels.size).joinToString(".")
    }

    fun getRegistrySuffix(domain: String): String? =
        getPublicSuffix(domain, PublicSuffixType.REGISTRY)

    /**
     * Type of the matched public suffix (REGISTRY vs PRIVATE), or null if none.
     * Walks the trie the same way as [findSuffixIndex] and records the last matching type.
     */
    fun getPublicSuffixType(domain: String): PublicSuffixType? {
        val labels = domain.lowercase().split('.').filter { it.isNotEmpty() }
        val partsSize = labels.size
        var nodeIndex = 0
        var bestType: PublicSuffixType? = null
        var bestResult = -1
        for (i in partsSize - 1 downTo 0) {
            val firstChild = trieData.charAt(nodeIndex * NODE_SIZE + 1).code
            val numChildren = trieData.charAt(nodeIndex * NODE_SIZE + 2).code and CHILDREN_MASK
            nodeIndex = findChild(firstChild, numChildren, labels[i])
            if (nodeIndex == -1) break
            val metadata = trieData.charAt(nodeIndex * NODE_SIZE + 2).code
            if (isExcludedMatch(metadata)) {
                bestResult = i + 1
                // exclusion nodes are not typed public suffixes; keep prior type
            }
            val exactType = getExactMatchType(metadata)
            if (exactType != null) {
                bestResult = i
                bestType = exactType
            }
            val wildcardType = getWildcardMatchType(metadata)
            if (i > 0 && wildcardType != null) {
                bestResult = i - 1
                bestType = wildcardType
            }
        }
        return if (bestResult < 0) null else bestType
    }

    fun isPublicSuffix(domain: String): Boolean = getPublicSuffix(domain) == domain.lowercase().trimEnd('.')

    fun isRegistrySuffix(domain: String): Boolean {
        val rs = getRegistrySuffix(domain) ?: return false
        return rs == domain.lowercase().trimEnd('.')
    }

    private fun matchesType(desired: PublicSuffixType?, actual: PublicSuffixType?): Boolean =
        actual != null && (desired == null || desired == actual)

    private fun isExcludedMatch(metadata: Int): Boolean =
        ((metadata ushr EXCLUSION_SHIFT) and EXCLUSION_MASK) != 0

    private fun getExactMatchType(metadata: Int): PublicSuffixType? =
        getType((metadata ushr EXACT_SHIFT) and TYPE_MASK)

    private fun getWildcardMatchType(metadata: Int): PublicSuffixType? =
        getType((metadata ushr WILDCARD_SHIFT) and TYPE_MASK)

    private fun getType(typeBits: Int): PublicSuffixType? = when (typeBits) {
        1 -> PublicSuffixType.REGISTRY
        2 -> PublicSuffixType.PRIVATE
        else -> null
    }

    private fun findChild(firstChild: Int, numChildren: Int, label: String): Int {
        var low = firstChild
        var high = firstChild + numChildren - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val labelOffset = trieData.charAt(mid * NODE_SIZE).code
            val cmp = compareLabel(label, labelOffset)
            when {
                cmp < 0 -> high = mid - 1
                cmp > 0 -> low = mid + 1
                else -> return mid
            }
        }
        return -1
    }

    private fun compareLabel(label: String, offset: Int): Int {
        val labelLen = label.length
        val nodeLabelLen = stringPool.charAt(offset).code
        val min = minOf(labelLen, nodeLabelLen)
        for (i in 0 until min) {
            val c1 = label[i]
            val c2 = stringPool.charAt(offset + 1 + i)
            if (c1 != c2) return c1 - c2
        }
        return labelLen - nodeLabelLen
    }

    /** Guava ChunksCharSequence — charAt uses chunkShift addressing (not naive concat). */
    private class ChunksCharSequence(
        private val chunks: List<String>,
        private val chunkShift: Int,
    ) {
        private val chunkMask = (1 shl chunkShift) - 1
        private val length: Int = chunks.sumOf { it.length }

        fun charAt(index: Int): Char {
            val chunk = chunks[index ushr chunkShift]
            return chunk[index and chunkMask]
        }

        fun length(): Int = length
    }

    companion object {
        const val NODE_SIZE = 3
        const val CHILDREN_MASK = 0x7FF
        const val EXCLUSION_MASK = 0x1
        const val TYPE_MASK = 0x3
        const val EXCLUSION_SHIFT = 11
        const val WILDCARD_SHIFT = 12
        const val EXACT_SHIFT = 14
    }
}
