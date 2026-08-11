package dev.guavakt.util.concurrent

/** Guava Runnables — empty runnable constant. */
object Runnables {
    val EMPTY_RUNNABLE: () -> Unit = {}
    fun doNothing(): () -> Unit = EMPTY_RUNNABLE
}
