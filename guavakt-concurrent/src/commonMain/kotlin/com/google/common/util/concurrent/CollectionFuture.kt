package dev.guavakt.util.concurrent

open class CollectionFuture<V>(
    futures: Collection<ListenableFuture<out V>>,
) : AbstractFuture<List<V>>() {
    private val inner = AggregateFuture.create(futures, allMustSucceed = true)
    init {
        setAsync(inner)
    }
}
