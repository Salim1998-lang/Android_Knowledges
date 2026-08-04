package handbook.exceptions

import handbook.exceptions.solutions.ExceptionsSolutions
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.cancellation.CancellationException

/**
 * Тема 4 «Обработка исключений» — 21 задача.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.exceptions.*"`.
 * Эталон — в [handbook.exceptions.solutions.ExceptionsSolutions].
 *
 * Подсказка по импортам: `CancellationException`, `async`, `awaitAll`, `coroutineScope`,
 * `supervisorScope`, `launch`, `delay`, `CoroutineExceptionHandler`, `CoroutineScope`,
 * `SupervisorJob`, `Job`.
 */
object ExceptionsTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Корутино-безопасный runCatching: успех→success; Cancellation→throw; прочее→failure. */
    suspend fun <T> coRunCatching(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /** Л2. Результат block или null при ошибке (Cancellation пробрасывать). */
    suspend fun <T> successOrNull(block: suspend () -> T): T? = coRunCatching(block).getOrNull()

    /** Л3. Результат block или [default] при ошибке. */
    suspend fun <T> getOrDefault(default: T, block: suspend () -> T): T = coRunCatching(block).getOrDefault(default)

    /** Л4. Результат block; при ошибке вернуть recover(e). */
    suspend fun <T> recoverWith(block: suspend () -> T, recover: (Throwable) -> T): T =
        coRunCatching(block).getOrElse(recover)

    /** Л5. true, если block бросил (не Cancellation). */
    suspend fun isFailure(block: suspend () -> Unit): Boolean = coRunCatching(block).isFailure

    /** Л6. Применить transform к input, обернув в Result (ошибка transform → failure). */
    suspend fun <T, R> catchingTransform(input: T, transform: suspend (T) -> R): Result<R> =
        coRunCatching { transform(input) }

    /** Л7. Первый блок, не бросивший исключение (по порядку); все упали → null. */
    suspend fun firstSuccessful(blocks: List<suspend () -> Int>): Int? {
        for (block in blocks) {
            val result = coRunCatching(block)
            if (result.isSuccess) {
                return result.getOrThrow()
            }
        }
        return null
    }

    /** Л8. Сколько блоков бросили исключение. */
    suspend fun countFailures(blocks: List<suspend () -> Unit>): Int {
        var count = 0
        for (block in blocks) {
            val result = coRunCatching(block)
            if (result.isFailure) {
                count++
            }
        }
        return count
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Выполни все loaders ПАРАЛЛЕЛЬНО и НЕЗАВИСИМО; верни List<Result> в порядке loaders.
     * Требования (проверяет тест):
     *  • loaders идут параллельно;
     *  • падение одного не срывает остальных (изоляция);
     *  • успех → Result.success, ошибка → Result.failure; порядок = порядок loaders.
     *
     * Спойлер: supervisorScope + async, вокруг await заворачивай в Result (Cancellation пробрасывать).
     */
    suspend fun <T> loadAllIndependently(loaders: List<suspend () -> T>): List<Result<T>> {
        return supervisorScope { loaders.map { async { coRunCatching { it() } } } }.awaitAll()
    }

    /**
     * С10. Выполни loaders независимо и раздели итоги на (успехи, ошибки).
     * Требования (проверяет тест):
     *  • падение одного не срывает остальных;
     *  • вернуть Pair(список значений успешных, список исключений упавших).
     *
     * Спойлер: как С9, затем partition по success/failure.
     */
    suspend fun <T> partitionResults(loaders: List<suspend () -> T>): Pair<List<T>, List<Throwable>> = supervisorScope {
        val loadeds = loadAllIndependently(loaders)
        loadeds.mapNotNull { it.getOrNull() } to loadeds.mapNotNull { it.exceptionOrNull() }
    }

    /**
     * С11. Сумма значений только успешных loaders (упавшие игнорируются).
     * Требования (проверяет тест):
     *  • ошибки отдельных loaders не срывают подсчёт;
     *  • вернуть сумму успешно посчитанных.
     *
     * Спойлер: независимо собери Result'ы, просуммируй успешные.
     */
    suspend fun sumSuccesses(loaders: List<suspend () -> Int>): Int = supervisorScope {
        val result = loadAllIndependently(loaders)
        result.mapNotNull { it.getOrNull() }.sum()
    }

    /**
     * С12. Для каждого loader верни его результат, а при ошибке — recover(e). Порядок сохраняется.
     * Требования (проверяет тест):
     *  • успешный loader → его значение; упавший → recover(его исключение);
     *  • порядок = порядок loaders; падение одного не срывает соседей.
     *
     * Спойлер: независимый запуск; на каждый await — try/catch с recover (Cancellation пробрасывать).
     */
    suspend fun <T> recoverEach(loaders: List<suspend () -> T>, recover: (Throwable) -> T): List<T> = supervisorScope {
        loadAllIndependently(loaders).map { it.getOrElse(recover) }
    }

    /**
     * С13. Собери message всех УПАВШИХ loaders, в порядке loaders.
     * Требования (проверяет тест):
     *  • успешные пропускаются;
     *  • для упавших — их message, в исходном порядке.
     *
     * Спойлер: независимые Result'ы → у failure возьми exceptionOrNull()?.message.
     */
    suspend fun <T> failureMessages(loaders: List<suspend () -> T>): List<String> = supervisorScope {
        loadAllIndependently(loaders).mapNotNull { it.exceptionOrNull()?.message }
    }

    /**
     * С14. Сколько loaders завершились успешно.
     * Требования (проверяет тест):
     *  • падение одних не мешает другим;
     *  • вернуть число успешных.
     *
     * Спойлер: независимые Result'ы → count { it.isSuccess }.
     */
    suspend fun <T> successCount(loaders: List<suspend () -> T>): Int = supervisorScope {
        loadAllIndependently(loaders).count { it.isSuccess }
    }

    /**
     * С15. Запусти loaders параллельно и независимо; верни результат ПЕРВОГО успешного ПО ПОРЯДКУ, иначе null.
     * Требования (проверяет тест):
     *  • «первый по порядку», а не «первый по времени» (позиция в loaders, не кто раньше финишировал);
     *  • все упали → null; падения не срывают друг друга.
     *
     * Спойлер: supervisor + async всех, затем по порядку ищи первый Result.success.
     */
    suspend fun <T> firstSuccessfulResult(loaders: List<suspend () -> T>): T? = supervisorScope {
        val deferreds = loaders.map { async { it() } }
        for (deferred in deferreds) {
            val res = coRunCatching { deferred.await() }
            if (res.isSuccess) {
                return@supervisorScope res.getOrThrow()
            }
        }
        null
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Выполни все blocks параллельно; верни их результаты. FAIL-FAST: ошибка любого отменяет
     *       остальных и всплывает наружу.
     * Требования (проверяет тест):
     *  • все успешны → список результатов в порядке blocks;
     *  • любой упал → остальные отменяются, исключение пробрасывается (не Result);
     *  • без утечек (отменённые сворачиваются).
     *
     * Спойлер: coroutineScope + async + awaitAll (fail-fast встроен).
     */
    suspend fun <T> awaitAllOrCancel(blocks: List<suspend () -> T>): List<T> =
        TODO()

    /**
     * СЛ17. Ретрай, возвращающий Result. До times попыток, между ними delay(delayMs).
     * Требования (проверяет тест):
     *  • успех на любой попытке → Result.success сразу;
     *  • все провалились → Result.failure с ПОСЛЕДНИМ исключением;
     *  • собственную отмену не глотать.
     *
     * Спойлер: цикл попыток в try/catch; успех → success; иначе запомни ошибку и повтори; Cancellation -> throw.
     */
    suspend fun <T> retryResult(times: Int, delayMs: Long, block: suspend () -> T): Result<T> =
        TODO()

    /**
     * СЛ18. Запусти blocks параллельно и независимо; верни первый успех ПО ПОРЯДКУ. Все упали → брось последнее.
     * Требования (проверяет тест):
     *  • «первый по порядку» (позиция в blocks), падения соседей не срывают;
     *  • все упали → пробросить последнее исключение (не null).
     *
     * Спойлер: supervisor + async всех; по порядку верни первый успешный await; иначе throw последней ошибки.
     */
    suspend fun <T> firstSuccessOf(blocks: List<suspend () -> T>): T =
        TODO()

    /**
     * СЛ19. Fail-fast сумма: все blocks успели → Result.success(сумма); любой упал → Result.failure(первое исключение).
     * Требования (проверяет тест):
     *  • при падении одного остальные отменяются (не ждём их);
     *  • ошибка возвращается как Result.failure, наружу не бросается.
     *
     * Спойлер: runCatching { coroutineScope { async-все; awaitAll().sum() } } (fail-fast даёт первое исключение).
     */
    suspend fun sumOrFirstError(blocks: List<suspend () -> Int>): Result<Int> =
        TODO()

    /**
     * СЛ20. Запусти blocks как независимых детей и верни, сколько из них выбросили НЕОБРАБОТАННОЕ исключение.
     * Требования (проверяет тест):
     *  • падение одного не отменяет остальных (супервизор-семантика);
     *  • необработанные исключения перехватываются, а не роняют всё; вернуть их число.
     *
     * Спойлер: scope с SupervisorJob + CoroutineExceptionHandler(считает ошибки); launch каждого; дождаться всех.
     */
    suspend fun countUncaught(blocks: List<suspend () -> Unit>): Int =
        TODO()

    // ═══════════════════════════ Дополнительные (21) ═══════════════════════════

    /**
     * Д21. suppressed-исключения. Выполни work() с ресурсом, который в конце закрывается через close().
     *      Если И work(), И close() бросают — ОСНОВНЫМ становится исключение work(), а исключение
     *      close() СОХРАНЯЕТСЯ рядом как suppressed (механизм try-with-resources). Верни
     *      Pair(message основного, список message подавленных). Ничего не бросило → (null, []).
     * Требования (проверяет тест):
     *  • work бросил, close бросил → primary = work.message, suppressed = [close.message];
     *  • только close бросил → primary = close.message, suppressed = [] (нечего подавлять);
     *  • ничего не бросило → (null, []).
     *
     * Спойлер: AutoCloseable { close() }.use { work() }; в catch — e.message to e.suppressed.map { it.message }.
     */
    suspend fun collectSuppressed(
        work: suspend () -> Unit,
        close: () -> Unit,
    ): Pair<String?, List<String?>> = TODO()
}
