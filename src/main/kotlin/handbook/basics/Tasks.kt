package handbook.basics

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

/**
 * Тема 1 «Основы» — 23 задачи.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.basics.*"`.
 * Эталон — в [handbook.basics.solutions.BasicsSolutions].
 *
 * Подсказка по импортам: `kotlinx.coroutines.delay`, `launch`, `coroutineScope`, `CoroutineStart`.
 */
object BasicsTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Приостановиться на 100 мс и вернуть переданное значение. */
    suspend fun delayedValue(value: Int): Int = TODO()

    /** Л2. Подождать 50 мс и вернуть "Привет, <name>!". */
    suspend fun greet(name: String): String = TODO()

    /** Л3. Подождать 10 мс и вернуть сумму a + b. */
    suspend fun sumAfterDelay(a: Int, b: Int): Int = TODO()

    /** Л4. Вызвать a() и b() ПОСЛЕДОВАТЕЛЬНО и вернуть их сумму. */
    suspend fun sequentialSum(a: suspend () -> Int, b: suspend () -> Int): Int = TODO()

    /** Л5. Подождать 10 мс и вернуть удвоенное значение. */
    suspend fun doubled(x: Int): Int = TODO()

    /** Л6. Вернуть список из [times] копий [value]; перед каждым добавлением — delay(5). */
    suspend fun repeatValue(value: String, times: Int): List<String> = TODO()

    /** Л7. Обратный отсчёт: вернуть [n, n-1, ..., 1], каждый шаг с delay(5). */
    suspend fun countdown(n: Int): List<Int> = TODO()

    /** Л8. Запустить дочернюю корутину, которая вызовет [action]; дождаться её (join). */
    suspend fun runInChild(action: () -> Unit) { TODO() }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Запусти n дочерних корутин: корутина i ждёт (n-i)*10 мс, затем добавляет свой индекс.
     *     Верни индексы в порядке ЗАВЕРШЕНИЯ.
     * Требования (проверяет тест):
     *  • корутины параллельны (общее время = максимума задержек, а не суммы);
     *  • порядок = порядок завершения → [n-1, ..., 0].
     *
     * Спойлер: n раз launch { delay((n-i)*10); result += i }; читать список после coroutineScope.
     */
    suspend fun collectInLaunchOrder(n: Int): List<Int> = TODO()

    /**
     * С10. Запусти n корутин, каждая вызывает onEach(i). Дождись всех, верни n.
     * Требования (проверяет тест):
     *  • onEach вызван для каждого i от 0 до n-1;
     *  • функция не возвращается, пока все корутины не отработали.
     *
     * Спойлер: coroutineScope { repeat(n) { launch { onEach(it) } } }; n.
     */
    suspend fun launchN(n: Int, onEach: (Int) -> Unit): Int = TODO()

    /**
     * С11. Применяй ops к start ПОСЛЕДОВАТЕЛЬНО (каждый к результату предыдущего), верни итог.
     * Требования (проверяет тест):
     *  • ops применяются по цепочке в порядке списка (fold).
     *
     * Спойлер: var acc = start; for (op in ops) acc = op(acc).
     */
    suspend fun sequentialChain(start: Int, ops: List<suspend (Int) -> Int>): Int = TODO()

    /**
     * С12. Просуммируй values, делая delay(5) перед каждым прибавлением. Верни сумму.
     * Требования (проверяет тест):
     *  • обработка последовательна (общее время = N × delay(5));
     *  • результат = сумма всех values.
     *
     * Спойлер: for (v in values) { delay(5); sum += v }.
     */
    suspend fun delayedSum(values: List<Int>): Int = TODO()

    /**
     * С13. Построй приветствия для всех имён ПОСЛЕДОВАТЕЛЬНО, используя собственный greet.
     * Требования (проверяет тест):
     *  • обработка последовательна (время = N × задержки greet);
     *  • порядок результатов = порядок имён.
     *
     * Спойлер: for (name in names) result += greet(name)  (свой greet, не из solutions).
     */
    suspend fun buildGreetings(names: List<String>): List<String> = TODO()

    /**
     * С14. Запусти n корутин, каждая делает delay(1) и counter++. Дождись всех и верни counter.
     * Требования (проверяет тест):
     *  • корутины параллельны (общее время = 1, а не n);
     *  • counter == n.
     * Тонкость: counter читай ПОСЛЕ выхода из coroutineScope (внутри блока сразу после launch он ещё 0).
     *
     * Спойлер: coroutineScope { repeat(n){ launch { delay(1); counter++ } } }; вернуть counter после.
     */
    suspend fun counterWithLaunches(n: Int): Int = TODO()

    /**
     * С15. Запусти по корутине на каждую задержку; корутина добавляет свой индекс после своей задержки.
     *      Верни индексы в порядке ЗАВЕРШЕНИЯ.
     * Требования (проверяет тест):
     *  • корутины параллельны (время = максимума задержек);
     *  • порядок = порядок завершения (по возрастанию delays[i]).
     * (Общий список пишется из параллельных launch — безопасно только на однопоточном тест-диспетчере.)
     *
     * Спойлер: coroutineScope { for i launch { delay(delays[i]); result += i } }.
     */
    suspend fun orderedByDelay(delays: List<Long>): List<Int> = TODO()

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Выполни block; при исключении повторяй, всего не более times попыток, между попытками delay(delayMs).
     * Требования (проверяет тест):
     *  • успех на любой попытке → вернуть результат сразу, без лишних пауз;
     *  • K неудач → ровно K пауз delay(delayMs) (между попытками, не после последней);
     *  • все попытки провалились → пробросить ПОСЛЕДНЕЕ исключение.
     *
     * Спойлер: repeat(times){ try return block() catch { last=e; if(attempt<times-1) delay(delayMs) } }; throw last.
     */
    suspend fun <T> retry(times: Int, delayMs: Long = 20, block: suspend () -> T): T = TODO()

    /**
     * СЛ17. Опрашивай produce() с delay(5) между попытками, пока не вернёт target. Верни число попыток
     *       (включая успешную).
     * Требования (проверяет тест):
     *  • число пауз delay(5) = число неудачных попыток;
     *  • возвращённое число = все попытки, включая успешную.
     *
     * Спойлер: var n=1; while (produce() != target) { delay(5); n++ }; n.
     */
    suspend fun pollUntil(target: Int, produce: suspend () -> Int): Int = TODO()

    /**
     * СЛ18. Факториал n через РЕКУРСИЮ с приостановкой (на каждом шаге delay(1)).
     * Требования (проверяет тест):
     *  • factorial(0) = factorial(1) = 1 без задержки;
     *  • для n≥2 время = (n-1) × delay(1); результат = n!.
     *
     * Спойлер: if (n <= 1) 1 else { delay(1); n * factorial(n-1) }.
     */
    suspend fun factorial(n: Int): Long = TODO()

    /**
     * СЛ19. Создай ЛЕНИВУЮ корутину, которая вызовет onStart. Если trigger — запусти её и верни true;
     *       иначе отмени незапущенную (onStart не должен вызваться) и верни false.
     * Требования (проверяет тест):
     *  • trigger=true → onStart вызван, вернуть true;
     *  • trigger=false → onStart НЕ вызван, вернуть false.
     *
     * Спойлер: launch(start = CoroutineStart.LAZY){ onStart() }; trigger ? job.join() : job.cancel().
     */
    suspend fun lazyStart(trigger: Boolean, onStart: () -> Unit): Boolean = TODO()

    /**
     * СЛ20. n шагов: на каждом delay(1) и sum += step. Верни итог.
     * Требования (проверяет тест):
     *  • время = n × delay(1);
     *  • результат = n × step.
     *
     * Спойлер: repeat(n) { delay(1); sum += step }.
     */
    suspend fun accumulate(n: Int, step: Int): Int = TODO()

    // ═══════════════════════════ Дополнительные (21–23) ═══════════════════════════

    /**
     * Д21. `yield` и чередование. Запустить [n] корутин ПО ПОРЯДКУ. Каждая корутина i:
     * добавляет i в общий список, вызывает `yield()`, снова добавляет i. Из-за yield первый
     * проход всех корутин идёт ДО второго → [0,1,..,n-1, 0,1,..,n-1]. Без yield было бы
     * [0,0,1,1,...]. Вернуть итоговый список. (Тест-диспетчер однопоточный — гонок нет.)
     */
    suspend fun roundRobin(n: Int): List<Int> = TODO()

    /**
     * Д22. Состояния Job. Создать ленивую корутину (`CoroutineStart.LAZY`, внутри delay).
     * Снять флаги (isActive, isCompleted, isCancelled) в двух точках:
     *  - New (до старта),
     *  - Cancelled (после `cancelAndJoin` незапущенной).
     * Вернуть Pair(флагиNew, флагиCancelled). Ловушка: в Cancelled `isCompleted` тоже true.
     */
    suspend fun lazyJobFlags(): Pair<Triple<Boolean, Boolean, Boolean>, Triple<Boolean, Boolean, Boolean>> =
        TODO()

    /**
     * Д23. `launch` лишь ставит корутину в очередь. В общий log по порядку:
     *  1) `launch { log.add("child") }`
     *  2) log.add("parent-before")
     *  3) `yield()` — suspend-точка, здесь ребёнок и отработает
     *  4) log.add("parent-after")
     * Вернуть log. Ожидается [parent-before, child, parent-after]: до suspend-точки ребёнок не бежит.
     */
    suspend fun queueBeforeSuspend(): List<String> = TODO()
}
