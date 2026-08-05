package handbook.flow.solutions

import handbook.flow.IntEmitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

/** Эталонные решения темы 5. Подсмотри, если застрял с [handbook.flow.FlowTasks]. */
object FlowSolutions {

    fun rangeFlow(n: Int): Flow<Int> = flow { for (i in 1..n) emit(i) }

    fun evenSquares(source: Flow<Int>): Flow<Int> = source.filter { it % 2 == 0 }.map { it * it }

    fun <T> fromList(list: List<T>): Flow<T> = list.asFlow()

    fun mapLength(source: Flow<String>): Flow<Int> = source.map { it.length }

    fun <T> takeN(source: Flow<T>, n: Int): Flow<T> = source.take(n)

    fun <T> dropN(source: Flow<T>, n: Int): Flow<T> = source.drop(n)

    suspend fun <T> countItems(source: Flow<T>): Int = source.count()

    suspend fun toListSorted(source: Flow<Int>): List<Int> = source.toList().sorted()

    fun runningTotal(source: Flow<Int>): Flow<Int> = source.scan(0) { acc, x -> acc + x }.drop(1)

    fun runningMax(source: Flow<Int>): Flow<Int> = source.runningReduce { acc, x -> maxOf(acc, x) }

    fun <T> duplicateEach(source: Flow<T>): Flow<T> = source.transform { emit(it); emit(it) }

    fun <T> dedupAdjacent(source: Flow<T>): Flow<T> = source.distinctUntilChanged()

    fun zipSum(a: Flow<Int>, b: Flow<Int>): Flow<Int> = a.zip(b) { x, y -> x + y }

    fun <T> indexedPairs(source: Flow<T>): Flow<Pair<Int, T>> =
        source.withIndex().map { it.index to it.value }

    suspend fun foldSum(source: Flow<Int>): Int = source.fold(0) { acc, x -> acc + x }

    fun <T> chunked(source: Flow<T>, size: Int): Flow<List<T>> {
        require(size >= 1)
        return flow {
            val buffer = ArrayList<T>(size)
            source.collect { value ->
                buffer.add(value)
                if (buffer.size == size) {
                    emit(buffer.toList())
                    buffer.clear()
                }
            }
            if (buffer.isNotEmpty()) emit(buffer.toList())
        }
    }

    fun <T> withFallback(source: Flow<T>, fallback: T): Flow<T> = source.catch { emit(fallback) }

    fun <T> retryUpstream(source: Flow<T>, retries: Long): Flow<T> = source.retry(retries)

    fun expand(source: Flow<Int>): Flow<Int> =
        source.flatMapConcat { n -> flow { repeat(n) { emit(n) } } }

    fun batchSums(source: Flow<Int>, size: Int): Flow<Int> = chunked(source, size).map { it.sum() }

    fun emitterFlow(emitter: IntEmitter): Flow<Int> = callbackFlow {
        val subscription = emitter.subscribe(
            onEach = { value -> trySend(value) },
            onComplete = { close() },
        )
        awaitClose { subscription.cancel() }
    }

    fun mergeConcurrently(sources: List<Flow<Int>>): Flow<Int> = channelFlow {
        for (src in sources) launch { src.collect { send(it) } }
    }
}
