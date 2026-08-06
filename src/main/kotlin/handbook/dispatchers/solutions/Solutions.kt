package handbook.dispatchers.solutions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Эталонные решения темы 5. Подсмотри, если застрял с [handbook.dispatchers.DispatchersTasks]. */
object DispatchersSolutions {

    // ── Лёгкие ──

    suspend fun currentName(): String? = coroutineContext[CoroutineName]?.name

    suspend fun <T> onIO(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    suspend fun <T> onDefault(block: suspend () -> T): T = withContext(Dispatchers.Default) { block() }

    suspend fun runsOn(dispatcher: CoroutineDispatcher): Boolean =
        withContext(dispatcher) { coroutineContext[ContinuationInterceptor] === dispatcher }

    fun ioWithName(name: String): CoroutineContext = Dispatchers.IO + CoroutineName(name)

    suspend fun nameUnder(name: String): String? =
        withContext(CoroutineName(name)) { coroutineContext[CoroutineName]?.name }

    fun rightWins(a: CoroutineContext, b: CoroutineContext): CoroutineContext = a + b

    fun withoutName(ctx: CoroutineContext): CoroutineContext = ctx.minusKey(CoroutineName)

    // ── Средние ──

    suspend fun childInheritsName(parent: String): String? = withContext(CoroutineName(parent)) {
        var seen: String? = null
        coroutineScope { launch { seen = coroutineContext[CoroutineName]?.name } }
        seen
    }

    suspend fun childOverridesName(parent: String, child: String): String? = withContext(CoroutineName(parent)) {
        var seen: String? = null
        coroutineScope { launch(CoroutineName(child)) { seen = coroutineContext[CoroutineName]?.name } }
        seen
    }

    suspend fun switchIOtoDefault(): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        val outerIsIO = coroutineContext[ContinuationInterceptor] === Dispatchers.IO
        val innerIsDefault = withContext(Dispatchers.Default) {
            coroutineContext[ContinuationInterceptor] === Dispatchers.Default
        }
        outerIsIO to innerIsDefault
    }

    private fun busySpin(millis: Long) {
        val end = System.nanoTime() + millis * 1_000_000
        @Suppress("ControlFlowWithEmptyBody")
        while (System.nanoTime() < end) { /* держим поток занятым, не приостанавливаясь */ }
    }

    suspend fun observedConcurrency(limit: Int, tasks: Int): Int {
        val dispatcher = Dispatchers.Default.limitedParallelism(limit)
        val active = AtomicInteger(0)
        val max = AtomicInteger(0)
        withContext(dispatcher) {
            coroutineScope {
                repeat(tasks) {
                    launch {
                        val cur = active.incrementAndGet()
                        max.updateAndGet { m -> maxOf(m, cur) }
                        busySpin(15)
                        active.decrementAndGet()
                    }
                }
            }
        }
        return max.get()
    }

    suspend fun runWithNameOnIO(name: String): Pair<String?, Boolean> =
        withContext(Dispatchers.IO + CoroutineName(name)) {
            coroutineContext[CoroutineName]?.name to (coroutineContext[ContinuationInterceptor] === Dispatchers.IO)
        }

    suspend fun <T, R> mapOnIO(items: List<T>, f: suspend (T) -> R): List<R> = withContext(Dispatchers.IO) {
        items.map { async { f(it) } }.awaitAll()
    }

    suspend fun namePropagatesToAsync(name: String): String? = withContext(CoroutineName(name)) {
        async { coroutineContext[CoroutineName]?.name }.await()
    }

    // ── Сложные ──

    suspend fun boundedParallelSum(numbers: List<Int>, parallelism: Int): Int =
        withContext(Dispatchers.Default.limitedParallelism(parallelism.coerceAtLeast(1))) {
            numbers.map { async { it } }.awaitAll().sum()
        }

    suspend fun dispatcherRoundTrip(): List<Boolean> = withContext(Dispatchers.Default) {
        val onDefault1 = coroutineContext[ContinuationInterceptor] === Dispatchers.Default
        val onIO = withContext(Dispatchers.IO) { coroutineContext[ContinuationInterceptor] === Dispatchers.IO }
        val onDefault2 = coroutineContext[ContinuationInterceptor] === Dispatchers.Default
        listOf(onDefault1, onIO, onDefault2)
    }

    suspend fun childJobIsChildOfParent(): Boolean = coroutineScope {
        val parentJob = coroutineContext[Job]!!
        var ok = false
        launch {
            val childJob = coroutineContext[Job]!!
            ok = childJob !== parentJob && parentJob.children.contains(childJob)
        }.join()
        ok
    }

    suspend fun fetchThenProcess(input: Int): Triple<Boolean, Boolean, Int> {
        val (fetchOnIO, data) = withContext(Dispatchers.IO) {
            (coroutineContext[ContinuationInterceptor] === Dispatchers.IO) to (input * 2)
        }
        val (processOnDefault, result) = withContext(Dispatchers.Default) {
            (coroutineContext[ContinuationInterceptor] === Dispatchers.Default) to (data + 1)
        }
        return Triple(fetchOnIO, processOnDefault, result)
    }

    suspend fun stagedPipeline(inputs: List<Int>): List<Int> =
        withContext(Dispatchers.Default.limitedParallelism(4)) {
            inputs.map { x ->
                async {
                    val fetched = withContext(Dispatchers.IO) { x * 2 }   // «сеть» на IO
                    fetched + 1                                           // «обработка» на Default
                }
            }.awaitAll()
        }
}
