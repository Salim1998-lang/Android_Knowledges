package handbook.hotflows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Тема 7 «Горячие потоки: StateFlow / SharedFlow» — 20 задач.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.hotflows.*"`.
 * Эталон — в [handbook.hotflows.solutions.HotFlowsSolutions].
 *
 * Задачи, которые превращают холодный Flow в горячий или собирают его, принимают `scope`
 * (в тестах — `backgroundScope`, чтобы runTest не повис на бесконечном горячем потоке).
 * Подсказка по импортам: `MutableStateFlow`, `MutableSharedFlow`, `asStateFlow`, `update`,
 * `first`, `take`, `toList`, `stateIn`, `shareIn`, `SharingStarted`, `combine`, `map`,
 * `runningReduce`, `channels.BufferOverflow`, `launch`, `awaitCancellation`.
 */
object HotFlowsTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Создай MutableStateFlow с начальным значением. Спойлер: MutableStateFlow(initial). */
    fun mutableIntState(initial: Int): MutableStateFlow<Int> = MutableStateFlow(initial)

    /** Л2. Верни текущее значение состояния. Спойлер: state.value. */
    fun currentValue(state: StateFlow<Int>): Int = state.value

    /** Л3. Запиши новое значение. Спойлер: state.value = newValue. */
    fun setValue(state: MutableStateFlow<Int>, newValue: Int) {
        state.value = newValue
    }

    /** Л4. Атомарно увеличь значение на 1 (без гонок). Спойлер: state.update { it + 1 }. */
    fun incrementState(state: MutableStateFlow<Int>) {
        state.update { it + 1 }
    }

    /** Л5. Отдай состояние наружу как read-only. Спойлер: state.asStateFlow(). */
    fun <T> asReadOnly(state: MutableStateFlow<T>): StateFlow<T> {
        return state.asStateFlow()
    }

    /**
     * Л6. Установи update, только если текущее значение == expect; верни, удалось ли.
     * Спойлер: state.compareAndSet(expect, update).
     */
    fun compareAndSetValue(state: MutableStateFlow<Int>, expect: Int, update: Int): Boolean =
        state.compareAndSet(expect, update)

    /** Л7. Создай MutableSharedFlow с заданным replay. Спойлер: MutableSharedFlow(replay = replay). */
    fun sharedWithReplay(replay: Int): MutableSharedFlow<Int> = MutableSharedFlow(replay = replay)

    /** Л8. Излучи по очереди все values в поток. Спойлер: for (v in values) sharedFlow.emit(v). */
    suspend fun emitEach(sharedFlow: MutableSharedFlow<Int>, values: List<Int>) {
        for (value in values) {
            sharedFlow.emit(value)
        }
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Создай SharedFlow с replay=2, помести в него все values (tryEmit) и верни replayCache.
     * Требования (проверяет тест): для [1,2,3,4] → [3,4] (последние два).
     */
    fun lastTwoReplayed(values: List<Int>): List<Int> {
        val sf = MutableSharedFlow<Int>(replay = 2)
        for (value in values) {
            sf.tryEmit(value)
        }
        return sf.replayCache
    }

    /**
     * С10. SharedFlow с replay=2 и onBufferOverflow=DROP_OLDEST: tryEmit никогда не блокирует и не
     *      теряет позицию — в replayCache остаются ПОСЛЕДНИЕ значения. Верни replayCache после всех values.
     * Требования (проверяет тест): для [1,2,3,4,5] → [4,5].
     * Спойлер: MutableSharedFlow(replay=2, onBufferOverflow=BufferOverflow.DROP_OLDEST); tryEmit все; replayCache.
     */
    fun dropOldestKeepsLatest(values: List<Int>): List<Int> {
        val sf = MutableSharedFlow<Int>(replay = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        for (value in values) {
            sf.tryEmit(value)
        }
        return sf.replayCache
    }

    /** С11. Верни число активных подписчиков потока. Спойлер: sharedFlow.subscriptionCount.value. */
    fun subscriberCount(sharedFlow: MutableSharedFlow<Int>): Int = sharedFlow.subscriptionCount.value

    /**
     * С12. Верни первое значение потока (для SharedFlow с replay>=1 — это закэшированное).
     * Требования (проверяет тест): не виснет, возвращает первое доступное.
     * Спойлер: flow.first().
     */
    suspend fun firstValue(flow: SharedFlow<Int>): Int = flow.first()

    /**
     * С13. Преврати холодный source в StateFlow (Eagerly, начальное initial), привязав к scope.
     * Требования (проверяет тест): после сбора source .value == последнему эмитнутому.
     * Спойлер: source.stateIn(scope, SharingStarted.Eagerly, initial).
     */
    fun stateFromFlow(scope: CoroutineScope, source: Flow<Int>, initial: Int): StateFlow<Int> = source.stateIn(
        scope,
        SharingStarted.Eagerly, initial
    )

    /**
     * С14. Преврати холодный source в SharedFlow (Eagerly, заданный replay), привязав к scope.
     * Требования (проверяет тест): replayCache содержит последние replay значений source.
     * Спойлер: source.shareIn(scope, SharingStarted.Eagerly, replay).
     */
    fun shareFromFlow(scope: CoroutineScope, source: Flow<Int>, replay: Int): SharedFlow<Int> =
        source.shareIn(scope, replay = replay, started = SharingStarted.Eagerly)

    /**
     * С15. Комбинируй два StateFlow в новый StateFlow их суммы (Eagerly, scope).
     * Требования (проверяет тест): .value == a.value + b.value и обновляется при изменении любого.
     * Спойлер: combine(a, b) { x, y -> x + y }.stateIn(scope, SharingStarted.Eagerly, a.value + b.value).
     */
    fun sumState(scope: CoroutineScope, a: StateFlow<Int>, b: StateFlow<Int>): StateFlow<Int> {
        return combine(a, b) { a, b -> a + b }.stateIn(scope, SharingStarted.Eagerly, a.value + b.value)
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Запусти в scope сбор source в sink (для наблюдения дедупликации StateFlow). Верни Job.
     * Требования (проверяет тест): при initial 0 и установке 0,1,1,2 (с прокруткой между) sink == [0,1,2]
     *      — повтор того же значения подписчика не будит.
     * Спойлер: scope.launch { source.collect { sink.add(it) } }.
     */
    fun mirror(scope: CoroutineScope, source: StateFlow<Int>, sink: MutableList<Int>): Job = scope.launch { source.collect { sink.add(it) } }

    /**
     * СЛ17. Накопительная сумма как StateFlow: source → бегущая сумма → StateFlow (Eagerly, initial 0).
     * Требования (проверяет тест): для source 1,2,3 → .value == 6 после сбора.
     * Спойлер: source.runningReduce { acc, x -> acc + x }.stateIn(scope, SharingStarted.Eagerly, 0).
     */
    fun runningTotalState(scope: CoroutineScope, source: Flow<Int>): StateFlow<Int> =
        source.runningReduce { accumulator, value -> accumulator + value }.stateIn(scope, SharingStarted.Eagerly, 0)

    /**
     * СЛ18. Возьми первые n значений SharedFlow и верни списком.
     * Требования (проверяет тест): для SharedFlow(replay=5) с [1..5] и n=3 → [1,2,3].
     * Спойлер: source.take(n).toList().
     */
    suspend fun takeN(source: SharedFlow<Int>, n: Int): List<Int> =
        source.take(n).toList()

    /**
     * СЛ19. Трансформируй StateFlow<Int> в StateFlow<R>, применяя f (Eagerly, scope).
     *      Начальное значение — f от текущего значения source.
     * Требования (проверяет тест): .value == f(source.value) и обновляется при изменении source.
     * Спойлер: source.map { f(it) }.stateIn(scope, SharingStarted.Eagerly, f(source.value)).
     */
    fun <R> mapState(scope: CoroutineScope, source: StateFlow<Int>, f: (Int) -> R): StateFlow<R> =
        source.map { f(it) }.stateIn(scope, SharingStarted.Eagerly, f(source.value))

    /**
     * СЛ20. Ленивое разделение: shareIn с SharingStarted.WhileSubscribed() — апстрим НЕ стартует,
     *      пока нет подписчиков, и включается с первым.
     * Требования (проверяет тест): без подписчиков source не собирается; после подписки — собирается.
     * Спойлер: source.shareIn(scope, SharingStarted.WhileSubscribed(), replay).
     */
    fun whileSubscribedShare(scope: CoroutineScope, source: Flow<Int>, replay: Int): SharedFlow<Int> =
        source.shareIn(scope, replay = replay, started = SharingStarted.WhileSubscribed())
}
