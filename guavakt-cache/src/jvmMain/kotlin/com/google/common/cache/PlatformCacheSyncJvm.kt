package dev.guavakt.cache

internal actual inline fun <T> platformCacheSync(lock: Any, block: () -> T): T = synchronized(lock, block)
