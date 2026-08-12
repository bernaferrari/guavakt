package com.bernaferrari.guavakt.collect

class HashMultiset<E> private constructor() : AbstractMapBasedMultiset<E>(HashMap()) {
    companion object {
        fun <E> create(): HashMultiset<E> = HashMultiset()
        fun <E> create(elements: Iterable<E>): HashMultiset<E> =
            create<E>().also { for (element in elements) it.add(element) }
    }
}
