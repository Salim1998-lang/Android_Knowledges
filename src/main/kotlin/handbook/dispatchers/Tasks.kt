package handbook.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
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
import kotlin.random.Random

/**
 * Тема 5 «Диспетчеры и CoroutineContext» — 20 задач.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.dispatchers.*"`.
 * Эталон — в [handbook.dispatchers.solutions.DispatchersSolutions].
 *
 * Идентичность диспетчера проверяют так: `coroutineContext[ContinuationInterceptor] === Dispatchers.IO`
 * (диспетчеры — синглтоны). Подсказка по импортам: `Dispatchers`, `withContext`, `coroutineScope`,
 * `launch`, `async`, `awaitAll`, `CoroutineName`, `Job`, `kotlin.coroutines.ContinuationInterceptor`.
 */
object DispatchersTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Верни имя текущей корутины (или null). Спойлер: coroutineContext[CoroutineName]?.name. */
    suspend fun currentName(): String? = coroutineContext[CoroutineName]?.name

    /** Л2. Выполни block на Dispatchers.IO и верни результат. Спойлер: withContext(Dispatchers.IO){ block() }. */
    suspend fun <T> onIO(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    /** Л3. Выполни block на Dispatchers.Default и верни результат. Спойлер: withContext(Default){ block() }. */
    suspend fun <T> onDefault(block: suspend () -> T): T = withContext(Dispatchers.Default) { block() }

    /**
     * Л4. Переключись на `dispatcher` и проверь, что текущий перехватчик — это он.
     * Спойлер: withContext(dispatcher){ coroutineContext[ContinuationInterceptor] === dispatcher }.
     */
    suspend fun runsOn(dispatcher: CoroutineDispatcher): Boolean =
        withContext(dispatcher) { kotlin.coroutines.coroutineContext[ContinuationInterceptor] === dispatcher }

    /** Л5. Собери контекст «IO + имя name». Спойлер: Dispatchers.IO + CoroutineName(name). */
    fun ioWithName(name: String): CoroutineContext = Dispatchers.IO + CoroutineName("$name")

    /**
     * Л6. Выполни блок под именем name и верни это имя изнутри.
     * Спойлер: withContext(CoroutineName(name)){ coroutineContext[CoroutineName]?.name }.
     */
    suspend fun nameUnder(name: String): String? =
        withContext(CoroutineName("$name")) { coroutineContext[CoroutineName]?.name }

    /**
     * Л7. Объедини два контекста; при конфликте ключей должен победить ПРАВЫЙ.
     * Спойлер: a + b (правый операнд переопределяет левый для того же ключа).
     */
    fun rightWins(a: CoroutineContext, b: CoroutineContext): CoroutineContext = a + b

    /** Л8. Верни контекст без элемента CoroutineName. Спойлер: ctx.minusKey(CoroutineName). */
    fun withoutName(ctx: CoroutineContext): CoroutineContext = ctx.minusKey(CoroutineName)

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Под именем parent запусти дочернюю корутину БЕЗ явного имени и верни имя, которое она видит.
     * Требования (проверяет тест): ребёнок НАСЛЕДУЕТ имя → результат == parent.
     *
     * Спойлер: withContext(CoroutineName(parent)){ var r; coroutineScope{ launch { r = coroutineContext[CoroutineName]?.name } }; r }.
     */
    suspend fun childInheritsName(parent: String): String? =
        withContext(CoroutineName("$parent")) {
            var child: String? = null
            coroutineScope {
                launch {
                    child = coroutineContext[CoroutineName]?.name
                }
            }
            child
        }

    /**
     * С10. Под именем parent запусти ребёнка с именем child (переопредели) и верни имя, которое он видит.
     * Требования (проверяет тест): результат == child.
     *
     * Спойлер: ... launch(CoroutineName(child)) { r = coroutineContext[CoroutineName]?.name } ...
     */
    suspend fun childOverridesName(parent: String, child: String): String? = withContext(CoroutineName(parent)) {
        var child1: String? = null
        coroutineScope {
            launch(CoroutineName(child)) {
                child1 = coroutineContext[CoroutineName]?.name
            }
        }
        child1
    }

    /**
     * С11. Находясь на IO, переключись внутрь на Default. Верни пару (былиНаIO, внутриНаDefault).
     * Требования (проверяет тест): (true, true).
     *
     * Спойлер: withContext(IO){ val a = interceptor===IO; val b = withContext(Default){ interceptor===Default }; a to b }.
     */
    suspend fun switchIOtoDefault(): Pair<Boolean, Boolean> = withContext(IO) {
        val a = ContinuationInterceptor === IO.key
        val b = withContext(Dispatchers.Default) {
            ContinuationInterceptor === Dispatchers.Default.key
        }
        a to b
    }

    /**
     * С12. Запусти `tasks` корутин на Default.limitedParallelism(limit) и верни МАКСИМАЛЬНОЕ число
     *      одновременно выполнявшихся (замер через счётчик активных).
     * Требования (проверяет тест): при tasks >= limit результат == limit (лимит реально ограничивает).
     * ВАЖНО: чтобы замер был честным, «работа» не должна приостанавливаться — держи поток занятым
     *        (busy-spin), иначе слот освобождается и активных станет больше лимита.
     *
     * Спойлер: val d = Dispatchers.Default.limitedParallelism(limit); active/max — AtomicInteger;
     *          launch { val c=active.incrementAndGet(); max.updateAndGet{ maxOf(it,c) }; busySpin(15); active.decrementAndGet() }.
     */
    suspend fun observedConcurrency(limit: Int, tasks: Int): Int {
        val active = AtomicInteger(0); val max = AtomicInteger(0)
        withContext(Default.limitedParallelism(limit)) {
            coroutineScope {
                repeat(tasks) {
                    launch {
                        val c = active.incrementAndGet()
                        max.updateAndGet { maxOf(c, it) }
                        busySpin(15)
                        active.decrementAndGet()
                    }
                }
            }
        }
        return max.get()
    }

    private fun busySpin(millis: Long) {
        val end = System.nanoTime() + millis * 1_000_000
        @Suppress("ControlFlowWithEmptyBody")
        while (System.nanoTime() < end) { /* держим поток занятым, не приостанавливаясь */ }
    }

    /**
     * С13. Выполни блок на «IO + имя name»; верни пару (имяВидимоеИзнутри, наIO).
     * Требования (проверяет тест): (name, true).
     *
     * Спойлер: withContext(Dispatchers.IO + CoroutineName(name)){ (coroutineContext[CoroutineName]?.name) to (interceptor===IO) }.
     */
    suspend fun runWithNameOnIO(name: String): Pair<String?, Boolean> = withContext(IO + CoroutineName(name)) {
        Pair(coroutineContext[CoroutineName]?.name, coroutineContext[CoroutineName]?.name != null)
    }

    /**
     * С14. Параллельно примени f к каждому элементу items на IO, сохранив порядок.
     * Требования (проверяет тест): результат == items.map(f), по элементу на async.
     *
     * Спойлер: withContext(Dispatchers.IO){ items.map { async { f(it) } }.awaitAll() }.
     */
    suspend fun <T, R> mapOnIO(items: List<T>, f: suspend (T) -> R): List<R> = withContext(IO) {
        items.map { async { f(it) } }.awaitAll()
    }

    /**
     * С15. Под именем name запусти async и верни имя, которое видит его тело.
     * Требования (проверяет тест): async НАСЛЕДУЕТ имя → результат == name.
     *
     * Спойлер: withContext(CoroutineName(name)){ async { coroutineContext[CoroutineName]?.name }.await() }.
     */
    suspend fun namePropagatesToAsync(name: String): String? = withContext(CoroutineName(name)) {
        async { coroutineContext[CoroutineName]?.name }.await()
    }

// ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Просуммируй numbers параллельно на Default.limitedParallelism(parallelism).
     * Требования (проверяет тест): результат == numbers.sum(); работа идёт на ограниченном пуле.
     *
     * Спойлер: withContext(Dispatchers.Default.limitedParallelism(parallelism)){ numbers.map { async { it } }.awaitAll().sum() }.
     */
    suspend fun boundedParallelSum(numbers: List<Int>, parallelism: Int): Int = withContext(Dispatchers.Default.limitedParallelism(parallelism)) {
        numbers.map { async {  it } }.awaitAll().sum()
    }

    /**
     * СЛ17. Round-trip: на Default → внутрь на IO → обратно (уже на Default). Верни [наDefault, наIO, сноваНаDefault].
     * Требования (проверяет тест): [true, true, true].
     *
     * Спойлер: withContext(Default){ val a=interceptor===Default; val b=withContext(IO){interceptor===IO}; val c=interceptor===Default; listOf(a,b,c) }.
     */
    suspend fun dispatcherRoundTrip(): List<Boolean> = withContext(Default) {
        val a = coroutineContext[ContinuationInterceptor] === Default
        val b = withContext(IO) { coroutineContext[ContinuationInterceptor] === IO };
        val c = coroutineContext[ContinuationInterceptor] === Default
        listOf(a, b, c)
    }

    /**
     * СЛ18. Запусти ребёнка и проверь, что его Job — это ОТДЕЛЬНЫЙ Job, являющийся дочерним для текущего.
     * Требования (проверяет тест): true (childJob !== parentJob И childJob входит в parentJob.children).
     *
     * Спойлер: coroutineScope { val p=coroutineContext[Job]!!; var ok=false; launch { val c=coroutineContext[Job]!!; ok = c!==p && p.children.contains(c) }.join(); ok }.
     */
    suspend fun childJobIsChildOfParent(): Boolean = coroutineScope {
        val p = coroutineContext[Job]!!
        var b = false
        launch {
            b = (p !== coroutineContext[Job]!! && p.children.contains(coroutineContext[Job]))
        }.join()
        b
    }

    /**
     * СЛ19. Конвейер: «загрузи» на IO (input*2), затем «обработай» на Default (+1).
     *      Верни Triple(загрузкаБылаНаIO, обработкаБылаНаDefault, результат).
     * Требования (проверяет тест): для input=10 → (true, true, 21).
     *
     * Спойлер: val (io,data)=withContext(IO){ (interceptor===IO) to (input*2) }; val (def,res)=withContext(Default){ (interceptor===Default) to (data+1) }; Triple(io,def,res).
     */
    suspend fun fetchThenProcess(input: Int): Triple<Boolean, Boolean, Int> = withContext(IO) {
        var i = input * 2
        val io = coroutineContext[ContinuationInterceptor]!! === IO
        var d = false
        withContext(Default) {
            i += 1
            d = coroutineContext[ContinuationInterceptor]!! === Default
        }
        Triple(first = io, d, i)
    }

    /**
     * СЛ20. Многоступенчатый pipeline с лимитом: для каждого input параллельно (не больше 4 разом)
     *      «загрузи» на IO (x*2), затем на Default прибавь 1. Верни результаты В ПОРЯДКЕ входа.
     * Требования (проверяет тест): [1,2,3] → [3,5,7]; порядок сохранён (awaitAll).
     *
     * Спойлер: withContext(Dispatchers.Default.limitedParallelism(4)){ inputs.map { x -> async { val f=withContext(IO){ x*2 }; f+1 } }.awaitAll() }.
     */
    suspend fun stagedPipeline(inputs: List<Int>): List<Int> = withContext(Dispatchers.Default.limitedParallelism(4)) {
        inputs.map { x -> async { val f = withContext(IO) { x * 2}; f +1 } }.awaitAll()
    }
}
