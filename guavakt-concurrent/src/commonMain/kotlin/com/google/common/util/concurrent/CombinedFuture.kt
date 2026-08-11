package dev.guavakt.util.concurrent

class CombinedFuture<V>(
    futures: Collection<ListenableFuture<out V>>,
) : AbstractFuture<List<V>>() {
    init {
        setAsync(AggregateFuture.create(futures, allMustSucceed = true))
    }
}
