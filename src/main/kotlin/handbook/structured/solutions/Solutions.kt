package handbook.structured.solutions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Эталонные решения темы 2. Подсмотри, если застрял с [handbook.structured.StructuredTasks]. */
object StructuredSolutions {

    suspend fun <A, B> awaitBoth(a: suspend () -> A, b: suspend () -> B): Pair<A, B> = coroutineScope {
        val da = async { a() }
        val db = async { b() }
        da.await() to db.await()
    }

    suspend fun parallelSum(a: suspend () -> Int, b: suspend () -> Int): Int = coroutineScope {
        val da = async { a() }; val db = async { b() }
        da.await() + db.await()
    }

    suspend fun parallelProduct(a: suspend () -> Int, b: suspend () -> Int): Int = coroutineScope {
        val da = async { a() }; val db = async { b() }
        da.await() * db.await()
    }

    suspend fun <A, B, R> combineAsync(a: suspend () -> A, b: suspend () -> B, combine: (A, B) -> R): R =
        coroutineScope {
            val da = async { a() }; val db = async { b() }
            combine(da.await(), db.await())
        }

    suspend fun parallelMax(a: suspend () -> Int, b: suspend () -> Int): Int = coroutineScope {
        val da = async { a() }; val db = async { b() }
        maxOf(da.await(), db.await())
    }

    suspend fun runAllParallel(blocks: List<suspend () -> Unit>) {
        coroutineScope { blocks.map { block -> async { block() } }.awaitAll() }
    }

    suspend fun asyncDouble(block: suspend () -> Int): Int = coroutineScope {
        val d = async { block() }
        d.await() * 2
    }

    suspend fun <A, B, C> tripleParallel(
        a: suspend () -> A, b: suspend () -> B, c: suspend () -> C,
    ): Triple<A, B, C> = coroutineScope {
        val da = async { a() }; val db = async { b() }; val dc = async { c() }
        Triple(da.await(), db.await(), dc.await())
    }

    suspend fun <T> loadAll(ids: List<Int>, load: suspend (Int) -> T): List<T> = coroutineScope {
        ids.map { id -> async { load(id) } }.awaitAll()
    }

    suspend fun parallelSumList(blocks: List<suspend () -> Int>): Int = coroutineScope {
        blocks.map { block -> async { block() } }.awaitAll().sum()
    }

    suspend fun <T, R> parallelMap(items: List<T>, transform: suspend (T) -> R): List<R> =
        coroutineScope { items.map { item -> async { transform(item) } }.awaitAll() }

    suspend fun sumHalvesInParallel(list: List<Int>): Int = coroutineScope {
        val mid = list.size / 2
        val left = async { list.subList(0, mid).sum() }
        val right = async { list.subList(mid, list.size).sum() }
        left.await() + right.await()
    }

    suspend fun <T> parallelFilter(items: List<T>, predicate: suspend (T) -> Boolean): List<T> =
        coroutineScope {
            items.map { item -> async { item to predicate(item) } }
                .awaitAll().filter { it.second }.map { it.first }
        }

    suspend fun <T> parallelCount(items: List<T>, predicate: suspend (T) -> Boolean): Int =
        coroutineScope {
            items.map { item -> async { if (predicate(item)) 1 else 0 } }.awaitAll().sum()
        }

    suspend fun awaitAllPreservesOrder(delays: List<Long>): List<Int> = coroutineScope {
        delays.mapIndexed { i, d -> async { delay(d); i } }.awaitAll()
    }

    suspend fun <T, R> mapConcurrent(items: List<T>, concurrency: Int, transform: suspend (T) -> R): List<R> =
        coroutineScope {
            require(concurrency >= 1) { "concurrency must be >= 1" }
            val gate = Semaphore(concurrency)
            items.map { item -> async { gate.withPermit { transform(item) } } }.awaitAll()
        }

    suspend fun <T, R> chunkedParallel(items: List<T>, chunkSize: Int, transform: suspend (T) -> R): List<R> {
        require(chunkSize >= 1)
        val result = mutableListOf<R>()
        for (chunk in items.chunked(chunkSize)) {
            val done = coroutineScope { chunk.map { item -> async { transform(item) } }.awaitAll() }
            result.addAll(done)
        }
        return result
    }

    suspend fun nestedParallelSum(matrix: List<List<Int>>): Int = coroutineScope {
        matrix.map { row ->
            async {
                var s = 0
                for (x in row) { delay(1); s += x }
                s
            }
        }.awaitAll().sum()
    }

    suspend fun <T, R> parallelMapCatching(items: List<T>, transform: suspend (T) -> R): List<Result<R>> =
        supervisorScope {
            items.map { item -> async { transform(item) } }.map { deferred ->
                try {
                    Result.success(deferred.await())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }
        }

    suspend fun windowedSums(values: List<Int>, window: Int): List<Int> = coroutineScope {
        require(window >= 1)
        values.windowed(window, step = 1, partialWindows = false)
            .map { w -> async { w.sum() } }.awaitAll()
    }

    suspend fun <T> runLazySequentially(blocks: List<suspend () -> T>): List<T> = coroutineScope {
        val deferreds = blocks.map { b -> async(start = CoroutineStart.LAZY) { b() } }
        // await() будит LAZY-корутину и ждёт её ДО того, как тронем следующую → строго по очереди.
        deferreds.map { it.await() }
    }

    suspend fun <A, B> bothOn(
        ctx: CoroutineContext,
        a: suspend () -> A,
        b: suspend () -> B,
    ): Pair<A, B> {
        val ra = withContext(ctx) { a() }   // один блок
        val rb = withContext(ctx) { b() }   // затем второй — последовательно, не параллельно
        return ra to rb
    }

    suspend fun runStructured(blocks: List<suspend () -> Unit>) {
        coroutineScope {
            blocks.forEach { b -> launch { b() } }
        }   // барьер coroutineScope дождётся всех детей — детей не отвязывали
    }
}
