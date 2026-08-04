package handbook.cancellation.solutions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

/** Эталонные решения темы 3. Подсмотри, если застрял с [handbook.cancellation.CancellationTasks]. */
object CancellationSolutions {

    suspend fun <T> orNull(timeoutMs: Long, block: suspend () -> T): T? =
        withTimeoutOrNull(timeoutMs) { block() }

    suspend fun <T> strict(timeoutMs: Long, block: suspend () -> T): T =
        withTimeout(timeoutMs) { block() }

    suspend fun <T> withFallback(timeoutMs: Long, fallback: T, block: suspend () -> T): T =
        withTimeoutOrNull(timeoutMs) { block() } ?: fallback

    suspend fun isTimedOut(timeoutMs: Long, block: suspend () -> Unit): Boolean =
        withTimeoutOrNull(timeoutMs) { block() } == null

    suspend fun <T> delayThenValue(ms: Long, value: T): T {
        delay(ms)
        return value
    }

    suspend fun sumCooperatively(values: List<Int>): Int {
        var sum = 0
        for (v in values) {
            currentCoroutineContext().ensureActive()
            delay(1)
            sum += v
        }
        return sum
    }

    suspend fun yieldingList(n: Int): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until n) {
            result.add(i)
            yield()
        }
        return result
    }

    suspend fun runUntilCancelled(tick: () -> Unit) {
        while (currentCoroutineContext().isActive) {
            tick()
            delay(10)
        }
    }

    suspend fun <T> countCompletedBeforeTimeout(
        timeoutMs: Long, items: List<T>, stepMs: Long, work: (T) -> Unit,
    ): Int {
        var done = 0
        withTimeoutOrNull(timeoutMs) {
            for (item in items) { delay(stepMs); work(item); done++ }
        }
        return done
    }

    suspend fun <T, R> mapUntilTimeout(
        timeoutMs: Long, items: List<T>, stepMs: Long, transform: (T) -> R,
    ): List<R> {
        val out = mutableListOf<R>()
        withTimeoutOrNull(timeoutMs) {
            for (item in items) { delay(stepMs); out.add(transform(item)) }
        }
        return out
    }

    suspend fun <T> loadAllWithDeadline(timeoutMs: Long, ids: List<Int>, load: suspend (Int) -> T): List<T>? =
        withTimeoutOrNull(timeoutMs) {
            coroutineScope { ids.map { id -> async { load(id) } }.awaitAll() }
        }

    suspend fun <T> cooperativeIndexOf(items: List<T>, predicate: suspend (T) -> Boolean): Int? {
        for ((i, item) in items.withIndex()) {
            currentCoroutineContext().ensureActive()
            if (predicate(item)) return i
        }
        return null
    }

    suspend fun runChildThenCancel(runMs: Long, tickEvery: Long): Int {
        var ticks = 0
        coroutineScope {
            val job = launch {
                while (true) { ticks++; delay(tickEvery) }
            }
            delay(runMs)
            job.cancel()
            job.join()
        }
        return ticks
    }

    suspend fun sumWithDeadline(timeoutMs: Long, values: List<Int>, stepMs: Long): Int {
        var sum = 0
        withTimeoutOrNull(timeoutMs) {
            for (v in values) { delay(stepMs); sum += v }
        }
        return sum
    }

    suspend fun <T> firstOrSecond(firstMs: Long, first: suspend () -> T, second: suspend () -> T): T =
        withTimeoutOrNull(firstMs) { first() } ?: second()

    suspend fun <T> runWithGuaranteedCleanup(block: suspend () -> T, cleanup: suspend () -> Unit): T {
        try {
            return block()
        } finally {
            withContext(NonCancellable) { cleanup() }
        }
    }

    suspend fun <T> retryWithTimeout(times: Int, perAttemptMs: Long, delayMs: Long, block: suspend () -> T): T {
        require(times >= 1)
        var last: Throwable? = null
        repeat(times) { attempt ->
            try {
                return withTimeout(perAttemptMs) { block() }
            } catch (e: TimeoutCancellationException) {
                last = e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                last = e
            }
            if (attempt < times - 1) delay(delayMs)
        }
        throw last!!
    }

    suspend fun <T> raceFirst(a: suspend () -> T, b: suspend () -> T): T = coroutineScope {
        val da = async { a() }
        val db = async { b() }
        select {
            da.onAwait { db.cancel(); it }
            db.onAwait { da.cancel(); it }
        }
    }

    suspend fun <T> withTimeoutCleanup(timeoutMs: Long, block: suspend () -> T, onCancel: suspend () -> Unit): T? =
        try {
            withTimeout(timeoutMs) { block() }
        } catch (e: TimeoutCancellationException) {
            withContext(NonCancellable) { onCancel() }
            null
        }

    suspend fun <T> parallelWithDeadline(timeoutMs: Long, blocks: List<suspend () -> T>): List<T>? =
        withTimeoutOrNull(timeoutMs) {
            coroutineScope { blocks.map { block -> async { block() } }.awaitAll() }
        }
}
