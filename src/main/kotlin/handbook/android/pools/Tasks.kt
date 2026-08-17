package handbook.android.pools

/**
 * Тема 3 «Пулы потоков в Android» — 20 задач.
 *
 * Про то, как ПРАВИЛЬНО настроить пул под Android: sizing (CPU- vs IO-bound), `ThreadFactory`
 * (осмысленные имена + приоритет фонового потока), и — главное — механику `ThreadPoolExecutor`:
 * связку core / max / очередь / rejection-политика, где скрыт классический подвох с безграничной
 * очередью. Учим на НАСТОЯЩЕМ `java.util.concurrent` (тот же, что внутри `AsyncTask`,
 * `Dispatchers.IO`, `WorkManager`). Различие с [handbook.java.concurrenthigh]: там — API
 * `ExecutorService`/`Future`, здесь — КАК сконфигурировать пул и почему.
 *
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.android.pools.*"`.
 * Эталон — в [handbook.android.pools.solutions.PoolsSolutions].
 *
 * Детерминизм: занимай ровно N потоков блокирующими задачами [Gate.task], дожидайся [Gate.awaitStarted],
 * читай состояние пула (`poolSize`, отказ), затем [Gate.release]. После работы — `shutdown()` +
 * `awaitTermination(...)`.
 */
object PoolsTasks {

    // ═══════════════════════════ Лёгкие (1–8): sizing и ThreadFactory ═══════════════════════════

    /**
     * Л1. Sizing для CPU-bound работы: потоков должно быть примерно `число_ядер + 1` (+1 «подстраховка»
     *     на случай редких промахов кэша/страничных ошибок). Верни [cores] + 1.
     *
     * Мораль: для чистого CPU больше потоков, чем ядер, лишь добавляют переключений контекста.
     */
    fun cpuBoundPoolSize(cores: Int): Int = TODO()

    /**
     * Л2. Sizing для IO-bound работы (формула Брайана Гётца): `N = ядра * (1 + wait/compute)`, где
     *     wait — время ожидания (сеть/диск), compute — время счёта. Верни округлённое число потоков
     *     по [cores], [waitMs], [computeMs].
     *
     * Мораль: чем больше поток «ждёт», тем больше потоков имеет смысл — они простаивают, не грузя CPU.
     * Спойлер: Math.round(cores * (1.0 + waitMs.toDouble()/computeMs)).toInt().
     */
    fun ioBoundPoolSize(cores: Int, waitMs: Long, computeMs: Long): Int = TODO()

    /**
     * Л3. `ThreadFactory`, дающая потокам осмысленные имена "prefix-1", "prefix-2", … Верни имена
     *     первых [count] созданных потоков.
     *
     * Мораль: именуй потоки пула — это разница между читаемым и нечитаемым ANR-трейсом/профайлером.
     * Спойлер: AtomicInteger n; ThreadFactory { r -> Thread(r, "$prefix-${n.incrementAndGet()}") }.
     */
    fun threadNames(prefix: String, count: Int): List<String> = TODO()

    /**
     * Л4. `ThreadFactory`, выставляющая приоритет потока в [priority] (аналог
     *     `android.os.Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND)`). Верни приоритет
     *     созданного потока.
     *
     * Мораль: фоновые пулы должны идти с ПОНИЖЕННЫМ приоритетом, чтобы не отбирать CPU у UI-потока.
     */
    fun factoryPriority(priority: Int): Int = TODO()

    /**
     * Л5. `ThreadFactory`, помечающая потоки как daemon = [daemon]. Верни `isDaemon` созданного потока.
     *
     * Мораль: daemon-потоки не мешают JVM/процессу завершиться; пул-потоки часто делают daemon,
     *         чтобы забытый `shutdown()` не держал процесс.
     */
    fun daemonFlag(daemon: Boolean): Boolean = TODO()

    /**
     * Л6. Базовое использование: на `Executors.newFixedThreadPool(4)` выполни [taskCount] задач,
     *     каждая инкрементит счётчик. Заверши пул (`shutdown` + `awaitTermination`) и верни счётчик.
     */
    fun fixedPoolRunsAll(taskCount: Int): Int = TODO()

    /**
     * Л7. Submit `Callable`, возвращающий `x*2`, на single-thread пул; верни `future.get()`.
     *
     * Мораль: `submit` даёт `Future` — мост «фоновая задача → её результат».
     */
    fun futureValue(x: Int): Int = TODO()

    /**
     * Л8. `invokeAll` на списке `Callable { x*x }` для [inputs]; верни результаты В ПОРЯДКЕ входа.
     *
     * Мораль: `invokeAll` блокирует до готовности всех и возвращает `Future` в порядке задач.
     */
    fun invokeAllInOrder(inputs: List<Int>): List<Int> = TODO()

    // ═══════════════════════════ Средние (9–15): механика ThreadPoolExecutor ═══════════════════════════

    /**
     * С9. `ThreadPoolExecutor(core=3, max=5, LinkedBlockingQueue())`. Займи 3 задачами, дождись
     *     старта и верни `poolSize` (== 3).
     *
     * Мораль: пул поднимает потоки под нагрузку до `core`.
     */
    fun activeCoreThreads(): Int = TODO()

    /**
     * С10. ⚠️ ГЛАВНЫЙ ПОДВОХ. `ThreadPoolExecutor(core=2, max=8, LinkedBlockingQueue())` — очередь
     *      БЕЗГРАНИЧНА. Отправь 10 блокирующих задач, дождись старта, верни `poolSize`.
     *
     * Мораль: с безграничной очередью пул НИКОГДА не растёт выше `core` (== 2) — до `max=8` дело не
     *         дойдёт, лишние задачи просто копятся в очереди. Именно так делает `newFixedThreadPool`.
     */
    fun unboundedQueueCapsAtCore(): Int = TODO()

    /**
     * С11. `ThreadPoolExecutor(core=2, max=4, ArrayBlockingQueue(2))`. Отправь 6 блокирующих задач,
     *      дождись старта, верни `poolSize` (== 4).
     *
     * Мораль: новые потоки (сверх core, до max) поднимаются, ТОЛЬКО когда очередь ЗАПОЛНЕНА.
     *         Порядок: core → очередь → до max → отказ.
     */
    fun boundedQueueReachesMax(): Int = TODO()

    /**
     * С12. Тот же пул (core=2, max=4, очередь 2 → ёмкость 6, политика по умолчанию AbortPolicy).
     *      Насыть 6 задачами, затем отправь 7-ю. Верни true, если поймал `RejectedExecutionException`.
     *
     * Мораль: когда заняты и все потоки, и очередь — новая задача отвергается (по умолчанию — с исключением).
     */
    fun rejectsWhenSaturated(): Boolean = TODO()

    /**
     * С13. `CallerRunsPolicy`. Пул core=1, max=1, очередь 1. Займи поток, заполни очередь, затем
     *      отправь задачу, которая запишет `Thread.currentThread()`. Верни true, если она выполнилась
     *      на ВЫЗЫВАЮЩЕМ потоке.
     *
     * Мораль: `CallerRunsPolicy` при переполнении исполняет задачу на потоке отправителя — это даёт
     *         естественный backpressure (продюсер притормаживается, ничего не теряется).
     */
    fun callerRunsOnCaller(): Boolean = TODO()

    /**
     * С14. `DiscardPolicy`. Пул core=1, max=1, очередь 1. Отправь 3 задачи (каждая инкрементит
     *      счётчик), заверши, верни счётчик.
     *
     * Мораль: `DiscardPolicy` молча ВЫБРАСЫВАЕТ перелив → выполнится только 2 из 3. Опасно: потеря без следа.
     */
    fun discardDropsOverflow(): Int = TODO()

    /**
     * С15. `shutdownNow`. Пул core=1, max=1, безграничная очередь. Займи поток, поставь 3 задачи в
     *      очередь, вызови `shutdownNow()` и верни размер возвращённого им списка (== 3).
     *
     * Мораль: `shutdown()` — «не принимать новые, доработать текущее»; `shutdownNow()` — прервать
     *         текущее и ВЕРНУТЬ незапущенные задачи (их можно перепланировать).
     */
    fun shutdownNowReturnsPending(): Int = TODO()

    // ═══════════════════════════ Сложные (16–20): паттерны ═══════════════════════════

    /**
     * СЛ16. Прогрев пула. Создай `ThreadPoolExecutor(core, max=core*2, …)`, вызови
     *       `prestartAllCoreThreads()` и верни `poolSize` ДО отправки задач (== [core]).
     *
     * Мораль: по умолчанию потоки создаются лениво; прогрев убирает задержку первого запроса
     *         (латентность «холодного» пула).
     */
    fun prestartCoreThreads(core: Int): Int = TODO()

    /**
     * СЛ17. single-thread пул сериализует задачи. Отправь [n] задач, добавляющих свой индекс; верни
     *       собранный список (== 0..n-1).
     *
     * Мораль: `newSingleThreadExecutor` = confinement + порядок (см. тему 2 про `HandlerThread`).
     */
    fun singleThreadPreservesOrder(n: Int): List<Int> = TODO()

    /**
     * СЛ18. Рецепт backpressure без потерь: `ThreadPoolExecutor(2, 2, ArrayBlockingQueue(2),
     *       CallerRunsPolicy)`. Отправь [n] задач (каждая инкрементит счётчик), заверши, верни счётчик.
     *
     * Мораль: ограниченная очередь + `CallerRunsPolicy` = продюсер притормаживается сам, НИ ОДНА
     *         задача не отвергнута и не потеряна (== [n]). Это дефолтный «безопасный» пул.
     */
    fun callerRunsNoLoss(n: Int): Int = TODO()

    /**
     * СЛ19. Изоляция пулов. Заведи два пула: `io` (fixed 2) и `cpu` (fixed 2). Полностью займи `io`
     *       блокирующими задачами, затем посчитай `42` на `cpu` и верни true, если получил результат.
     *
     * Мораль: РАЗНЫЕ пулы под IO и CPU изолируют блокировки — забитый IO-пул не морит голодом
     *         CPU-работу. Ровно поэтому у корутин `Dispatchers.IO` и `Dispatchers.Default` раздельны.
     */
    fun poolIsolation(): Boolean = TODO()

    /**
     * СЛ20. Измерь реальный параллелизм. На `newFixedThreadPool(k)` запусти [k] задач, которые через
     *       `CyclicBarrier(k)` оказываются активны ОДНОВРЕМЕННО; фиксируй максимум одновременно
     *       активных. Верни его (== [k]).
     *
     * Мораль: размер пула = жёсткий потолок параллелизма. Больше [k] одновременно не выполнится,
     *         сколько задач ни кидай.
     */
    fun observedParallelism(k: Int): Int = TODO()
}
