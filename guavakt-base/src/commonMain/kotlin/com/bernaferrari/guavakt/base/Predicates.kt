package com.bernaferrari.guavakt.base

object Predicates {
    fun <T> alwaysTrue(): Predicate<T> = Predicate { true }
    fun <T> alwaysFalse(): Predicate<T> = Predicate { false }
    fun <T> isNull(): Predicate<T?> = Predicate { it == null }
    fun <T> notNull(): Predicate<T?> = Predicate { it != null }
    fun <T> not(predicate: Predicate<T>): Predicate<T> = Predicate { !predicate.apply(it) }
    fun <T> and(a: Predicate<T>, b: Predicate<T>): Predicate<T> =
        Predicate { a.apply(it) && b.apply(it) }
    fun <T> or(a: Predicate<T>, b: Predicate<T>): Predicate<T> =
        Predicate { a.apply(it) || b.apply(it) }
    fun <T> equalTo(target: T): Predicate<T> = Predicate { it == target }
    fun <T> `in`(target: Collection<T>): Predicate<T> = Predicate { it in target }
    fun <A, B> compose(predicate: Predicate<B>, function: Function<A, B>): Predicate<A> =
        Predicate { predicate.apply(function.apply(it)) }
}
