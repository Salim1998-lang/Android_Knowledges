package handbook.cancellation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Тема 3 «Отмена и таймауты» — 20 задач.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.cancellation.*"`.
 * Эталон — в [handbook.cancellation.solutions.CancellationSolutions].
 *
 * Подсказка по импортам: `withTimeout`, `withTimeoutOrNull`, `TimeoutCancellationException`,
 * `NonCancellable`, `withContext`, `delay`, `yield`, `ensureActive`, `isActive`,
 * `currentCoroutineContext`, `coroutineScope`, `launch`, `async`, `awaitAll`,
 * `selects.select`, `CancellationException`.
 */
object CancellationTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Выполнить block с таймаутом; не успел → null. */
    suspend fun <T> orNull(timeoutMs: Long, block: suspend () -> T): T? = withTimeoutOrNull(timeoutMs.milliseconds) {
        block()
    }

    /** Л2. Выполнить block с таймаутом; не успел → TimeoutCancellationException. */
    suspend fun <T> strict(timeoutMs: Long, block: suspend () -> T): T = withTimeout(timeoutMs) { block() }

    /** Л3. Выполнить block с таймаутом; не успел → вернуть fallback. */
    suspend fun <T> withFallback(timeoutMs: Long, fallback: T, block: suspend () -> T): T =
        withTimeoutOrNull(timeoutMs) { block() } ?: fallback

    /** Л4. true, если block НЕ уложился в таймаут. */
    suspend fun isTimedOut(timeoutMs: Long, block: suspend () -> Unit): Boolean =
        withTimeoutOrNull(timeoutMs) { block() } == null

    /** Л5. Подождать ms (отменяемо) и вернуть value. */
    suspend fun <T> delayThenValue(ms: Long, value: T): T {
        delay(ms)
        return value
    }

    /** Л6. Кооперативная сумма: перед каждым элементом ensureActive(), затем delay(1) и +=. */
    suspend fun sumCooperatively(values: List<Int>): Int = coroutineScope {
        var sum = 0
        values.forEach {
            ensureActive()
            delay(1)
            sum += it
        }
        sum
    }

    /** Л7. Построить [0..n-1], уступая поток через yield() между элементами. */
    suspend fun yieldingList(n: Int): List<Int> = coroutineScope {
        val result = mutableListOf<Int>()
        for (i in 0..<n) {
            yield()
            result += i
        }
        result
    }

    /** Л8. Крутить, пока корутина активна: tick(); delay(10). Отмена завершает цикл. */
    suspend fun runUntilCancelled(tick: () -> Unit) {
        while (currentCoroutineContext().isActive) {
            tick()
            delay(10)
        }
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Сколько элементов успели ПОЛНОСТЬЮ обработать до дедлайна timeoutMs
     *     (обработка одного = delay(stepMs) + work(item)).
     * Требования (проверяет тест):
     *  • по дедлайну обработка прекращается — оставшиеся элементы не ждём;
     *  • вернуть точное число завершённых (прерванный на дедлайне не считается).
     *
     * Спойлер: withTimeoutOrNull + счётчик, объявленный ВНЕ таймаут-блока (иначе потеряешь его при отмене).
     */
    suspend fun <T> countCompletedBeforeTimeout(
        timeoutMs: Long,
        items: List<T>,
        stepMs: Long,
        work: (T) -> Unit,
    ): Int = coroutineScope {
        var count = 0
        withTimeoutOrNull(timeoutMs.milliseconds) {
            for (item in items) {
                delay(stepMs)
                work(item)
                count++
            }
        }
        count
    }

    /**
     * С10. Применяй transform к элементам по очереди (каждый занимает delay(stepMs)); верни список
     *      результатов тех, кто успел до дедлайна timeoutMs.
     * Требования (проверяет тест):
     *  • по дедлайну сбор прекращается;
     *  • вернуть ЧАСТИЧНЫЙ список успевших в исходном порядке (не null).
     *
     * Спойлер: withTimeoutOrNull, аккумулятор ВНЕ таймаут-блока.
     */
    suspend fun <T, R> mapUntilTimeout(
        timeoutMs: Long,
        items: List<T>,
        stepMs: Long,
        transform: (T) -> R,
    ): List<R> {
        val result = mutableListOf<R>()
        withTimeoutOrNull(timeoutMs.milliseconds) {
            for (item in items) {
                delay(stepMs)
                result.add(transform(item))
            }
        }
        return result
    }

    /**
     * С11. Загрузи все id и верни результаты в порядке ids — но под ОБЩИМ дедлайном timeoutMs.
     * Требования (проверяет тест):
     *  • загрузки идут параллельно (общее время = самой долгой, не суммы);
     *  • успели все → список; не успел хоть один → null (семантика «всё или ничего»);
     *  • по дедлайну незавершённые загрузки отменяются (без утечек).
     *
     * Спойлер: withTimeoutOrNull { ids.map { async { load(it) } }.awaitAll() }.
     */
    suspend fun <T> loadAllWithDeadline(
        timeoutMs: Long,
        ids: List<Int>,
        load: suspend (Int) -> T,
    ): List<T>? =
        withTimeoutOrNull(timeoutMs.milliseconds) {
            ids.map { async { load(it) } }.awaitAll()
        }

    /**
     * С12. Индекс первого элемента, для которого predicate вернёт true, иначе null.
     * Требования (проверяет тест):
     *  • при отмене корутины поиск прерывается на СЛЕДУЮЩЕМ шаге, даже если predicate не суспендится
     *    (длинный скан обязан быть отменяемым).
     *
     * Спойлер: проверяй отмену на каждом шаге (ensureActive()/isActive), ДО вызова predicate.
     */
    suspend fun <T> cooperativeIndexOf(items: List<T>, predicate: suspend (T) -> Boolean): Int? = coroutineScope {
        items.forEachIndexed { index, t ->
            ensureActive()
            if (predicate(t)) {
                return@coroutineScope index
            }
        }
        null
    }

    /**
     * С13. Запусти дочернюю корутину, которая тикает каждые tickEvery мс; спустя runMs останови её
     *      и верни, сколько тиков она успела.
     * Требования (проверяет тест):
     *  • общее время = runMs (не ждём лишнего после отмены);
     *  • возвращённое число тиков соответствует прожитому времени;
     *  • к возврату ребёнок фактически завершён (не «висит»).
     *
     * Спойлер: launch тикающий цикл; delay(runMs); job.cancelAndJoin() (именно ...AndJoin — дождаться).
     */
    suspend fun runChildThenCancel(runMs: Long, tickEvery: Long): Int = coroutineScope {
        var tick = 0
        val job = launch {
            while (isActive) {
                tick++
                delay(tickEvery)
            }
        }
        delay(runMs)
        job.cancelAndJoin()
        tick
    }

    /**
     * С14. Суммируй values по очереди (перед каждым — delay(stepMs)); верни сумму тех, кто успел
     *      до дедлайна timeoutMs.
     * Требования (проверяет тест):
     *  • по дедлайну суммирование прекращается;
     *  • вернуть ЧАСТИЧНУЮ сумму успевших (не бросать исключение).
     *
     * Спойлер: withTimeoutOrNull, сумма-аккумулятор ВНЕ таймаут-блока.
     */
    suspend fun sumWithDeadline(timeoutMs: Long, values: List<Int>, stepMs: Long): Int {
        var sum = 0
        withTimeoutOrNull(timeoutMs.milliseconds) {
            for (value in values) {
                delay(stepMs)
                sum += value
            }
        }
        return sum
    }

    /**
     * С15. Верни результат first(), если он уложился в firstMs; иначе — результат second().
     * Требования (проверяет тест):
     *  • first успел в дедлайн → его результат;
     *  • first не успел → дождаться second() и вернуть его результат (не null, не исключение).
     *
     * Спойлер: withTimeoutOrNull(firstMs) { first() } ?: second().
     */
    suspend fun <T> firstOrSecond(firstMs: Long, first: suspend () -> T, second: suspend () -> T): T =
        withTimeoutOrNull(firstMs.milliseconds) { first() } ?: second()

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Выполни block, верни его результат, и ГАРАНТИРОВАННО выполни cleanup в конце —
     *       даже если корутину отменили.
     * Требования (проверяет тест):
     *  • cleanup выполняется и при успехе, и при отмене;
     *  • cleanup доигрывает ПОЛНОСТЬЮ, даже если внутри есть suspend-вызовы;
     *  • отмену наружу не проглатывать.
     *
     * Спойлер: try/finally + withContext(NonCancellable) вокруг cleanup.
     */
    suspend fun <T> runWithGuaranteedCleanup(block: suspend () -> T, cleanup: suspend () -> Unit): T {
        return try {
            block()
        } finally {
            withContext(NonCancellable) {
                cleanup()
            }
        }
    }

    /**
     * СЛ17. Верни результат block. До times попыток, у каждой свой дедлайн perAttemptMs.
     *       Попытка провалилась (дедлайн ИЛИ ошибка) → следующая, с паузой delayMs между ними.
     * Требования (проверяет тест):
     *  • успех на любой попытке → вернуть сразу;
     *  • все провалились → пробросить последнюю ошибку;
     *  • ВНЕШНЮЮ отмену (не таймаут попытки!) не глотать — пробрасывать.
     *
     * Спойлер: withTimeout на попытку; отличай TimeoutCancellationException (ретраить)
     *          от «чужой» CancellationException (пробросить). Порядок catch важен.
     */
    suspend fun <T> retryWithTimeout(
        times: Int,
        perAttemptMs: Long,
        delayMs: Long,
        block: suspend () -> T,
    ): T {
        var t: Throwable? = null
        repeat(times) { attempt ->
            t = try {
                return withTimeout(perAttemptMs) { block() }
            } catch (e: TimeoutCancellationException) {
                e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e
            }
            if (attempt < times - 1) {
                delay(delayMs)
            }
        }
        throw t!!
    }

    /**
     * СЛ18. Верни результат того из a()/b(), кто завершится ПЕРВЫМ.
     * Требования (проверяет тест):
     *  • общее время = время победителя, не проигравшего (проигравший не должен доработать);
     *  • к возврату не осталось работающих корутин (без утечек).
     *
     * Спойлер: coroutineScope + два async + select { onAwait }; в выбранной ветке отмени проигравшего.
     */
    suspend fun <T> raceFirst(a: suspend () -> T, b: suspend () -> T): T = coroutineScope {
        val aA = async { a() }
        val bB = async { b() }
        select {
            aA.onAwait { bB.cancel(); it }
            bB.onAwait { aA.cancel(); it }
        }
    }

    /**
     * СЛ19. Выполни block под дедлайном timeoutMs. Успел → верни результат. Сработал таймаут →
     *       выполни onCancel() ровно один раз и верни null.
     * Требования (проверяет тест):
     *  • при успехе onCancel НЕ вызывается;
     *  • при таймауте onCancel вызывается ровно один раз и доигрывает полностью (даже с suspend внутри);
     *  • при таймауте результат = null.
     *
     * Спойлер: try/catch (TimeoutCancellationException) вокруг withTimeout; onCancel под withContext(NonCancellable).
     */
    suspend fun <T> withTimeoutCleanup(
        timeoutMs: Long,
        block: suspend () -> T,
        onCancel: suspend () -> Unit,
    ): T? =
        try {
            withTimeout(timeoutMs.milliseconds) { block() }
        } catch (e: TimeoutCancellationException) {
            withContext(NonCancellable) { onCancel() }
            null
        }

    /**
     * СЛ20. Запусти все blocks под ОБЩИМ дедлайном timeoutMs.
     * Требования (проверяет тест):
     *  • blocks идут параллельно (общее время = самого долгого);
     *  • успели все → список результатов; не успел хоть один → null;
     *  • по дедлайну незавершённые отменяются (без утечек).
     *
     * Спойлер: withTimeoutOrNull { blocks.map { async { it() } }.awaitAll() }.
     */
    suspend fun <T> parallelWithDeadline(timeoutMs: Long, blocks: List<suspend () -> T>): List<T>? {
        return withTimeoutOrNull(timeoutMs.milliseconds) {
            blocks.map { async { it() } }.awaitAll()
        }
    }
}
