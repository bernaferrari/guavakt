package com.bernaferrari.guavakt.io

/** Guava TempFileCreator — KMP cannot create real temp files in commonMain; returns path names. */
internal object TempFileCreator {
    private var counter = 0
    fun createTempFile(prefix: String, suffix: String): String =
        "${prefix}${counter++}${if (suffix.startsWith(".")) suffix else ".$suffix"}"
    fun createTempDir(prefix: String): String = "${prefix}${counter++}/"
}
