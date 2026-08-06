package handbook.testing

import kotlinx.coroutines.test.TestScope

/**
 * Тема 10 «Тестирование корутин» — 20 задач.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.testing.*"`.
 * Эталон — в [handbook.testing.solutions.TestingSolutions].
 *
 * Задачи — расширения [TestScope] (виртуальное время, планировщик). В тестах вызываются так:
 * `runTest { with(TestingTasks) { elapsed { delay(500) } } }`. Функция [runOnMain] — исключение:
 * она обычная suspend-функция «под тестом», а подмену Main делает сам тест (`setMain`/`resetMain`).
 * Подсказка по импортам: `test.currentTime`, `test.advanceTimeBy`, `test.runCurrent`,
 * `test.advanceUntilIdle`, `test.StandardTestDispatcher`, `test.UnconfinedTestDispatcher`,
 * `delay`, `launch`, `async`, `awaitAll`, `coroutineScope`, `withTimeout`, `withTimeoutOrNull`,
 * `Dispatchers.Main`, `flow`.
 */
object TestingTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Верни, сколько ВИРТУАЛЬНОГО времени заняло выполнение block. Спойлер: t0=currentTime; block(); currentTime-t0. */
    suspend fun TestScope.elapsed(block: suspend () -> Unit): Long =
        TODO()

    /**
     * Л2. Выполни delay(a), затем delay(b) ПОСЛЕДОВАТЕЛЬНО; верни итоговое виртуальное время.
     * Требования (проверяет тест): результат == a + b.
     * Спойлер: delay(a); delay(b); currentTime.
     */
    suspend fun TestScope.sequentialDelays(a: Long, b: Long): Long =
        TODO()

    /**
     * Л3. Запусти delay(a) и delay(b) ПАРАЛЛЕЛЬНО (в разных корутинах); верни итоговое время.
     * Требования (проверяет тест): результат == max(a, b) (не сумма!).
     * Спойлер: coroutineScope { launch { delay(a) }; launch { delay(b) } }; currentTime.
     */
    suspend fun TestScope.concurrentDelays(a: Long, b: Long): Long =
        TODO()

    /** Л4. Продвинь виртуальные часы на ms и верни currentTime. Спойлер: advanceTimeBy(ms); currentTime. */
    fun TestScope.advanceAndTime(ms: Long): Long =
        TODO()

    /**
     * Л5. Запусти корутину, которая после delay(100) кладёт 42 в результат; верни результат ПОСЛЕ
     *     того, как вся отложенная работа выполнена.
     * Требования (проверяет тест): результат == 42 (advanceUntilIdle довёл дело до конца).
     * Спойлер: var x=0; launch { delay(100); x=42 }; advanceUntilIdle(); x.
     */
    fun TestScope.pendingUntilIdle(): Int =
        TODO()

    /**
     * Л6. Запусти две корутины: одну без задержки (ставит a=1), другую с delay(100) (ставит b=1).
     *     Выполни ТОЛЬКО текущие задачи (без проматывания времени) и верни (a, b).
     * Требования (проверяет тест): (1, 0) — runCurrent выполнил немедленную, но не отложенную.
     * Спойлер: var a=0,b=0; launch { a=1 }; launch { delay(100); b=1 }; runCurrent(); a to b.
     */
    fun TestScope.runCurrentImmediate(): Pair<Int, Int> =
        TODO()

    /**
     * Л7. Запусти корутины с delay(100) (a=1) и delay(300) (b=1). Продвинь время на 150 и верни (a, b).
     * Требования (проверяет тест): (1, 0) — сработала только задержка 100.
     * Спойлер: ... advanceTimeBy(150); a to b.
     */
    fun TestScope.advancePartial(): Pair<Int, Int> =
        TODO()

    /** Л8. Подожди ms виртуально и верни value. Спойлер: delay(ms); value. */
    suspend fun TestScope.delayedValue(ms: Long, value: Int): Int =
        TODO()

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. StandardTestDispatcher ЛЕНИВЫЙ: запусти launch на нём и верни (исполнилосьДоRunCurrent, послеRunCurrent).
     * Требования (проверяет тест): (false, true).
     * Спойлер: val d=StandardTestDispatcher(testScheduler); var ran=false; launch(d){ran=true}; val b=ran; runCurrent(); b to ran.
     */
    fun TestScope.standardIsLazy(): Pair<Boolean, Boolean> =
        TODO()

    /**
     * С10. UnconfinedTestDispatcher ЖАДНЫЙ: запусти launch на нём; верни, исполнилось ли СРАЗУ (до проматывания).
     * Требования (проверяет тест): true.
     * Спойлер: val d=UnconfinedTestDispatcher(testScheduler); var ran=false; launch(d){ran=true}; ran.
     */
    fun TestScope.unconfinedIsEager(): Boolean =
        TODO()

    /**
     * С11. Запусти три корутины с delay(300)->3, delay(100)->1, delay(200)->2, дописывающие в список.
     *     Дай всему отработать; верни список в порядке СРАБАТЫВАНИЯ.
     * Требования (проверяет тест): [1, 2, 3] (по возрастанию задержки, не запуска).
     * Спойлер: launch{delay(300);add(3)}; launch{delay(100);add(1)}; launch{delay(200);add(2)}; advanceUntilIdle(); list.
     */
    fun TestScope.orderedByDelay(): List<Int> =
        TODO()

    /**
     * С12. Запусти корутину, которая трижды делает delay(100) и после каждого пишет currentTime.
     *     Дай отработать; верни список отметок времени.
     * Требования (проверяет тест): [100, 200, 300].
     * Спойлер: launch { repeat(3){ delay(100); times.add(currentTime) } }; advanceUntilIdle(); times.
     */
    fun TestScope.currentTimeAtEachStep(): List<Long> =
        TODO()

    /**
     * С13. withTimeout(1000) вокруг работы delay(500)->7. Верни результат.
     * Требования (проверяет тест): 7 (успел; тест не ждёт реально).
     * Спойлер: withTimeout(1000){ delay(500); 7 }.
     */
    suspend fun TestScope.timeoutSucceeds(): Int =
        TODO()

    /**
     * С14. withTimeoutOrNull(1000) вокруг работы delay(2000). Верни, случился ли тайм-аут (результат == null).
     * Требования (проверяет тест): true.
     * Спойлер: withTimeoutOrNull(1000){ delay(2000); 1 } == null.
     */
    suspend fun TestScope.timeoutFails(): Boolean =
        TODO()

    /**
     * С15. Собери Flow, эмитящий 1,2,3 с delay(100) перед каждым. Верни (список, затраченноеВиртуальноеВремя).
     * Требования (проверяет тест): ([1,2,3], 300).
     * Спойлер: val f=flow{ for(i in 1..3){ delay(100); emit(i) } }; t0=currentTime; val l=f.toList(); l to (currentTime-t0).
     */
    suspend fun TestScope.collectWithVirtualTime(): Pair<List<Int>, Long> =
        TODO()

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Обычная suspend-функция «под тестом»: выполни работу на Dispatchers.Main и верни метку.
     *      Подмену Main (`setMain`/`resetMain`) делает САМ ТЕСТ — здесь просто используй Main.
     * Требования (проверяет тест): "on-main".
     * Спойлер: withContext(Dispatchers.Main){ "on-main" }.
     */
    suspend fun runOnMain(): String =
        TODO()

    /**
     * СЛ17. Retry с экспоненциальной задержкой: операция бросает первые (failTimes) раз, затем успех.
     *      Между попытками — delay(100 * 2^(n-1)) (100, 200, 400, ...). Верни (числоПопыток, виртуальноеВремя).
     * Требования (проверяет тест): при failTimes=3 → попыток 4, время = 100+200+400 = 700.
     * Спойлер: t0=currentTime; var attempt=0; while(true){ attempt++; try{ op(attempt); break } catch(e){ delay(100 * (1 shl (attempt-1))) } }; attempt to (currentTime-t0).
     */
    suspend fun TestScope.retryWithBackoff(failTimes: Int): Pair<Int, Long> =
        TODO()

    /**
     * СЛ18. backgroundScope: запусти БЕСКОНЕЧНЫЙ тикер (каждые period добавляет ++i в список). Затем
     *      `steps` раз продвинь время на period и верни накопленный список.
     * Требования (проверяет тест): [1, 2, ..., steps]; тест не виснет (тикер в backgroundScope).
     * Спойлер: backgroundScope.launch { while(true){ delay(period); ticks.add(++i) } }; repeat(steps){ advanceTimeBy(period); runCurrent() }; ticks.
     */
    fun TestScope.backgroundTicker(period: Long, steps: Int): List<Int> =
        TODO()

    /**
     * СЛ19. Три async с delay(a)->a, delay(b)->b, delay(c)->c параллельно. Верни (списокРезультатов, времяВыполнения).
     * Требования (проверяет тест): для (100,200,300) → ([100,200,300], 300) — время == max.
     * Спойлер: t0=currentTime; val r=listOf(a,b,c).map{ async{ delay(it); it } }.awaitAll(); r to (currentTime-t0).
     */
    suspend fun TestScope.parallelAwaitAll(a: Long, b: Long, c: Long): Pair<List<Long>, Long> =
        TODO()

    /**
     * СЛ20. Одни и те же задержки (100,200,300) выполни ПОСЛЕДОВАТЕЛЬНО, потом ПАРАЛЛЕЛЬНО.
     *      Верни (времяПоследовательно, времяПараллельно).
     * Требования (проверяет тест): (600, 300).
     * Спойлер: t0; delay(100);delay(200);delay(300); seq=currentTime-t0; t1; coroutineScope{ launch{delay(100)};launch{delay(200)};launch{delay(300)} }; par=currentTime-t1; seq to par.
     */
    suspend fun TestScope.sequentialVsParallel(): Pair<Long, Long> =
        TODO()
}
