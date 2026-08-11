package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible
abstract class Ordering<T> : Comparator<T> {
    abstract override fun compare(left: T, right: T): Int

    fun <S : T> reverse(): Ordering<S> = ReverseOrdering(this)

    fun <F> onResultOf(function: (F) -> T): Ordering<F> = object : Ordering<F>() {
        override fun compare(left: F, right: F): Int =
            this@Ordering.compare(function(left), function(right))
    }

    fun nullsFirst(): Ordering<T?> = object : Ordering<T?>() {
        override fun compare(left: T?, right: T?): Int = when {
            left === right -> 0
            left == null -> -1
            right == null -> 1
            else -> this@Ordering.compare(left, right)
        }
    }

    fun nullsLast(): Ordering<T?> = object : Ordering<T?>() {
        override fun compare(left: T?, right: T?): Int = when {
            left === right -> 0
            left == null -> 1
            right == null -> -1
            else -> this@Ordering.compare(left, right)
        }
    }

    fun <E : T> leastOf(iterable: Iterable<E>, k: Int): List<E> {
        Preconditions.checkArgument(k >= 0)
        return iterable.sortedWith(this).take(k)
    }

    fun <E : T> greatestOf(iterable: Iterable<E>, k: Int): List<E> {
        Preconditions.checkArgument(k >= 0)
        return iterable.sortedWith(this.reversed()).take(k)
    }

    fun <E : T> sortedCopy(elements: Iterable<E>): List<E> = elements.sortedWith(this)

    fun <E : T> min(a: E, b: E): E = if (compare(a, b) <= 0) a else b
    fun <E : T> max(a: E, b: E): E = if (compare(a, b) >= 0) a else b

    companion object {
        fun <C : Comparable<C>> natural(): Ordering<C> = NaturalOrdering as Ordering<C>
        fun <T> from(comparator: Comparator<T>): Ordering<T> =
            if (comparator is Ordering) comparator else ComparatorOrdering(comparator)
        fun <T> explicit(list: List<T>): Ordering<T> = ExplicitOrdering(list)
        fun usingToString(): Ordering<Any> = object : Ordering<Any>() {
            override fun compare(left: Any, right: Any): Int = left.toString().compareTo(right.toString())
        }
        fun <T> allEqual(): Ordering<T> = object : Ordering<T>() {
            override fun compare(left: T, right: T): Int = 0
        }
    }
}
