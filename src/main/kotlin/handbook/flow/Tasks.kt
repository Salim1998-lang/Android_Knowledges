package handbook.flow

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Отписка от колбэк-источника (как ListenerRegistration/Disposable). */
fun interface Subscription {
    fun cancel()
}

/**
 * Учебный колбэк-API (имитация слушателя): значения приходят «извне» через [emit], конец — [complete].
 * Используется в задаче про `callbackFlow` (СЛ21). После отписки [isSubscribed] == false — так тест
 * проверяет, что `awaitClose { }` реально освободил ресурс.
 */
class IntEmitter {
    private var onEach: ((Int) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null

    fun subscribe(onEach: (Int) -> Unit, onComplete: () -> Unit): Subscription {
        this.onEach = onEach
        this.onComplete = onComplete
        return Subscription {
            this.onEach = null
            this.onComplete = null
        }
    }

    fun emit(value: Int) {
        onEach?.invoke(value)
    }

    fun complete() {
        onComplete?.invoke()
    }

    fun isSubscribed(): Boolean = onEach != null
}

/**
 * Тема 6 «Flow» — 22 задачи (20 базовых + 2 на мост с колбэк-API).
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.flow.*"`.
 * Эталон — в [handbook.flow.solutions.FlowSolutions].
 *
 * Подсказка по импортам: `flow`, `flowOf`, `asFlow`, `map`, `filter`, `take`, `drop`, `count`,
 * `toList`, `scan`, `runningReduce`, `transform`, `distinctUntilChanged`, `zip`, `withIndex`,
 * `fold`, `catch`, `retry`, `flatMapConcat`, `callbackFlow`, `channelFlow`, `awaitClose`.
 */
object FlowTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Flow чисел от 1 до n. */
    fun rangeFlow(n: Int): Flow<Int> = (1..n).asFlow()

    /** Л2. Оставить чётные и возвести в квадрат. */
    fun evenSquares(source: Flow<Int>): Flow<Int> = source.filter { it % 2 == 0 }.map { it * it }

    /** Л3. Поток из элементов списка. */
    fun <T> fromList(list: List<T>): Flow<T> = list.asFlow()

    /** Л4. Длины строк. */
    fun mapLength(source: Flow<String>): Flow<Int> = source.map { it.length }

    /** Л5. Первые n элементов. */
    fun <T> takeN(source: Flow<T>, n: Int): Flow<T> = source.take(n)

    /** Л6. Пропустить первые n элементов. */
    fun <T> dropN(source: Flow<T>, n: Int): Flow<T> = source.drop(n)

    /** Л7. Число элементов в потоке (терминальный count). */
    suspend fun <T> countItems(source: Flow<T>): Int = source.count()

    /** Л8. Собрать в отсортированный список. */
    suspend fun toListSorted(source: Flow<Int>): List<Int> = source.toList().sorted()

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Нарастающие суммы: [1,2,3] → [1,3,6].
     * Требования (проверяет тест):
     *  • i-й элемент = сумма первых (i+1) входных;
     *  • длина выхода = длине входа (без «нулевого» начального элемента).
     *
     * Спойлер: scan(0){ acc, x -> acc + x }.drop(1).
     */
    fun runningTotal(source: Flow<Int>): Flow<Int> =
        source.scan(0) { accumulator, value -> accumulator + value }.drop(1)

    /**
     * С10. Нарастающий максимум: [3,1,4,1,5] → [3,3,4,4,5].
     * Требования (проверяет тест):
     *  • i-й элемент = максимум первых (i+1) входных;
     *  • длина выхода = длине входа.
     *
     * Спойлер: runningReduce { acc, x -> maxOf(acc, x) }.
     */
    fun runningMax(source: Flow<Int>): Flow<Int> =
        source.runningReduce { accumulator, value -> maxOf(accumulator, value) }

    /**
     * С11. Продублируй каждый элемент: [a,b] → [a,a,b,b].
     * Требования (проверяет тест):
     *  • каждый входной элемент излучается дважды подряд, в порядке.
     *
     * Спойлер: transform { emit(it); emit(it) }.
     */
    fun <T> duplicateEach(source: Flow<T>): Flow<T> = source.transform { emit(it); emit(it) }

    /**
     * С12. Убери ПОДРЯД идущие дубликаты: [1,1,2,2,1] → [1,2,1] (не-соседние повторы остаются).
     * Требования (проверяет тест):
     *  • элемент пропускается, только если равен непосредственно предыдущему выданному.
     *
     * Спойлер: distinctUntilChanged().
     */
    fun <T> dedupAdjacent(source: Flow<T>): Flow<T> = source.distinctUntilChanged()

    /**
     * С13. Попарная сумма двух потоков: выход[i] = a[i] + b[i].
     * Требования (проверяет тест):
     *  • элементы берутся попарно; длина = min(len a, len b).
     *
     * Спойлер: a.zip(b) { x, y -> x + y }.
     */
    fun zipSum(a: Flow<Int>, b: Flow<Int>): Flow<Int> = a.zip(b) { a, b -> a + b }

    /**
     * С14. Преобразуй поток в пары (индекс, значение), индекс с 0.
     * Требования (проверяет тест):
     *  • выход = [(0,v0), (1,v1), ...] в порядке.
     *
     * Спойлер: withIndex().map { it.index to it.value }  (или счётчик в transform).
     */
    fun <T> indexedPairs(source: Flow<T>): Flow<Pair<Int, T>> = source.withIndex().map { it.index to it.value }

    /**
     * С15. Терминальная операция: сумма всех элементов потока.
     * Требования (проверяет тест):
     *  • suspend-функция, собирает поток и возвращает сумму (пустой → 0).
     *
     * Спойлер: fold(0) { acc, x -> acc + x }.
     */
    suspend fun foldSum(source: Flow<Int>): Int = source.fold(0) { a, b -> a + b }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Группируй элементы по size: [1,2,3,4,5], size=2 → [[1,2],[3,4],[5]]. Пустой → пустой.
     * Требования (проверяет тест):
     *  • полные группы по size, последняя может быть неполной;
     *  • остаток излучается по завершении upstream.
     *
     * Спойлер: flow { буфер; на каждый — add, при заполнении emit и очисти; после collect — emit остатка }.
     */
    fun <T> chunked(source: Flow<T>, size: Int): Flow<List<T>> {
        require(size >= 1)
        return flow {
            val buffer = mutableListOf<T>()
            source.collect { value ->
                buffer.add(value)
                if (buffer.size == size) {
                    emit(buffer.toList())
                    buffer.clear()
                }
            }
            if (buffer.isNotEmpty()) {
                emit(buffer.toList())
            }
        }
    }

    /**
     * СЛ17. Если upstream упал — вместо ошибки излучи fallback и заверши поток нормально.
     * Требования (проверяет тест):
     *  • без ошибки — исходные элементы;
     *  • при ошибке upstream — уже излучённые элементы + fallback, без выброса наружу.
     *
     * Спойлер: catch { emit(fallback) }.
     */
    fun <T> withFallback(source: Flow<T>, fallback: T): Flow<T> =
        source.catch { emit(fallback) }

    /**
     * СЛ18. При ошибке upstream перезапускай его сначала, до retries раз.
     * Требования (проверяет тест):
     *  • успех — как есть;
     *  • ошибка → повтор всего upstream (не более retries раз), затем пробросить, если так и падает.
     *
     * Спойлер: retry(retries).
     */
    fun <T> retryUpstream(source: Flow<T>, retries: Long): Flow<T> = source.retry(retries)

    /**
     * СЛ19. Разверни каждое число n в n копий n: [2,3] → [2,2,3,3,3].
     * Требования (проверяет тест):
     *  • для входного n — ровно n элементов со значением n;
     *  • порядок сохраняется (сначала все копии первого, затем второго).
     *
     * Спойлер: flatMapConcat { n -> flow { repeat(n) { emit(n) } } }.
     */
    fun expand(source: Flow<Int>): Flow<Int> = source.flatMapConcat { n -> flow { repeat(n) { emit(n) } } }

    /**
     * СЛ20. Разбей поток на батчи по size и излучи СУММУ каждого батча (последний может быть неполным).
     * Требования (проверяет тест):
     *  • выход[i] = сумма i-го батча из size элементов;
     *  • остаток (< size) тоже даёт свою сумму.
     *
     * Спойлер: chunked(size), затем map { it.sum() } — или буфер во flow { }.
     */
    fun batchSums(source: Flow<Int>, size: Int): Flow<Int> = source.chunked(size = size).map { it.sum() }

    // ═══════════════════════════ Мост с колбэк-API (21–22) ═══════════════════════════

    /**
     * СЛ21. Оберни колбэк-источник [IntEmitter] в холодный Flow через `callbackFlow`.
     * Требования (проверяет тест):
     *  • на каждый emitter.emit(v) — элемент v в потоке (используй trySend);
     *  • emitter.complete() завершает поток (close);
     *  • при отмене/завершении сбора подписка снимается — `awaitClose { subscription.cancel() }`
     *    (после сбора emitter.isSubscribed() == false).
     *
     * Спойлер: callbackFlow { val sub = emitter.subscribe(onEach = { trySend(it) }, onComplete = { close() }); awaitClose { sub.cancel() } }.
     */
    fun emitterFlow(emitter: IntEmitter): Flow<Int> = callbackFlow {
        val subscription = emitter.subscribe(onEach = { trySend(it) }, onComplete = { close() })
        awaitClose { subscription.cancel() }
    }

    /**
     * СЛ22. Слей несколько Flow в один КОНКУРЕНТНО через `channelFlow` (обычный `flow { }` не умеет
     *       эмитить из нескольких корутин — нужен channelFlow + send).
     * Требования (проверяет тест):
     *  • в выходе присутствуют все элементы всех источников;
     *  • источники собираются параллельно (каждый в своём launch).
     *
     * Спойлер: channelFlow { sources.forEach { src -> launch { src.collect { send(it) } } } }.
     */
    fun mergeConcurrently(sources: List<Flow<Int>>): Flow<Int> = channelFlow {
        sources.forEach { source ->
            launch { source.collect { send(it) } }
        }
    }
}
