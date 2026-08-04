package handbook.exceptions.solutions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/** Эталонные решения темы 4. Подсмотри, если застрял с [handbook.exceptions.ExceptionsTasks]. */
object ExceptionsSolutions {

    suspend fun <T> coRunCatching(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }

    suspend fun <T> successOrNull(block: suspend () -> T): T? = coRunCatching(block).getOrNull()

    suspend fun <T> getOrDefault(default: T, block: suspend () -> T): T =
        coRunCatching(block).getOrDefault(default)

    suspend fun <T> recoverWith(block: suspend () -> T, recover: (Throwable) -> T): T =
        coRunCatching(block).getOrElse(recover)

    suspend fun isFailure(block: suspend () -> Unit): Boolean = coRunCatching(block).isFailure

    suspend fun <T, R> catchingTransform(input: T, transform: suspend (T) -> R): Result<R> =
        coRunCatching { transform(input) }

    suspend fun firstSuccessful(blocks: List<suspend () -> Int>): Int? {
        for (block in blocks) {
            val r = coRunCatching { block() }
            if (r.isSuccess) return r.getOrThrow()
        }
        return null
    }

    suspend fun countFailures(blocks: List<suspend () -> Unit>): Int {
        var count = 0
        for (block in blocks) if (coRunCatching { block() }.isFailure) count++
        return count
    }

    suspend fun <T> loadAllIndependently(loaders: List<suspend () -> T>): List<Result<T>> =
        supervisorScope {
            loaders.map { loader -> async { loader() } }.map { d -> coRunCatching { d.await() } }
        }

    suspend fun <T> partitionResults(loaders: List<suspend () -> T>): Pair<List<T>, List<Throwable>> {
        val results = loadAllIndependently(loaders)
        return results.mapNotNull { it.getOrNull() } to results.mapNotNull { it.exceptionOrNull() }
    }

    suspend fun sumSuccesses(loaders: List<suspend () -> Int>): Int =
        loadAllIndependently(loaders).mapNotNull { it.getOrNull() }.sum()

    suspend fun <T> recoverEach(loaders: List<suspend () -> T>, recover: (Throwable) -> T): List<T> =
        loadAllIndependently(loaders).map { it.getOrElse(recover) }

    suspend fun <T> failureMessages(loaders: List<suspend () -> T>): List<String> =
        loadAllIndependently(loaders).mapNotNull { it.exceptionOrNull()?.message }

    suspend fun <T> successCount(loaders: List<suspend () -> T>): Int =
        loadAllIndependently(loaders).count { it.isSuccess }

    suspend fun <T> firstSuccessfulResult(loaders: List<suspend () -> T>): T? = supervisorScope {
        val deferreds = loaders.map { loader -> async { loader() } }
        for (d in deferreds) {
            val r = coRunCatching { d.await() }
            if (r.isSuccess) return@supervisorScope r.getOrThrow()
        }
        null
    }

    suspend fun <T> awaitAllOrCancel(blocks: List<suspend () -> T>): List<T> = coroutineScope {
        blocks.map { block -> async { block() } }.awaitAll()
    }

    suspend fun <T> retryResult(times: Int, delayMs: Long, block: suspend () -> T): Result<T> {
        require(times >= 1)
        var last: Throwable? = null
        repeat(times) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                last = e
                if (attempt < times - 1) delay(delayMs)
            }
        }
        return Result.failure(last!!)
    }

    suspend fun <T> firstSuccessOf(blocks: List<suspend () -> T>): T = supervisorScope {
        val deferreds = blocks.map { block -> async { block() } }
        var last: Throwable? = null
        for (d in deferreds) {
            try {
                return@supervisorScope d.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                last = e
            }
        }
        throw last ?: IllegalArgumentException("no blocks")
    }

    suspend fun sumOrFirstError(blocks: List<suspend () -> Int>): Result<Int> =
        coRunCatching {
            coroutineScope { blocks.map { block -> async { block() } }.awaitAll().sum() }
        }

    suspend fun countUncaught(blocks: List<suspend () -> Unit>): Int = coroutineScope {
        val caught = AtomicInteger(0)
        val handler = CoroutineExceptionHandler { _, _ -> caught.incrementAndGet() }
        val scope = CoroutineScope(coroutineContext + SupervisorJob() + handler)
        blocks.forEach { block -> scope.launch { block() } }
        scope.coroutineContext[Job]!!.children.forEach { it.join() }
        caught.get()
    }

    suspend fun collectSuppressed(
        work: suspend () -> Unit,
        close: () -> Unit,
    ): Pair<String?, List<String?>> {
        val resource = AutoCloseable { close() }
        return try {
            resource.use { work() }
            null to emptyList()
        } catch (e: Throwable) {
            e.message to e.suppressed.map { it.message }
        }
    }
}
