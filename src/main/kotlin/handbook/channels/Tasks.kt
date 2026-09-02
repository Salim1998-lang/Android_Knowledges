package handbook.channels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Тема 8 «Каналы» — 20 задач.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.channels.*"`.
 * Эталон — в [handbook.channels.solutions.ChannelsSolutions].
 *
 * Функции-производители — расширения CoroutineScope (нужны для `produce`). В тестах их вызывают
 * так: `with(ChannelsTasks) { drain(produceNumbers(5)) }` внутри `runTest { }`.
 * Подсказка по импортам: `channels.produce`, `channels.consumeEach`, `launch`, `async`,
 * `awaitAll`, `coroutineScope`, `selects.select`.
 */
object ChannelsTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Канал чисел 1..n (produce, закрывается сам). */
    fun CoroutineScope.produceNumbers(n: Int): ReceiveChannel<Int> = produce {
        for (n in 1..n) {
            send(n)
        }
    }

    /** Л2. Прочитать все элементы в список (consumeEach), сохранив порядок. */
    suspend fun <T> drain(channel: ReceiveChannel<T>): List<T> {
        val list = ArrayList<T>()
        channel.consumeEach { list += it }
        return list
    }

    /** Л3. Канал чисел from..to. */
    fun CoroutineScope.produceRange(from: Int, to: Int): ReceiveChannel<Int> = produce { for (n in from..to) send(n) }

    /** Л4. Канал из элементов списка. */
    fun <T> CoroutineScope.produceFrom(list: List<T>): ReceiveChannel<T> = produce { for (n in list) send(n) }

    /** Л5. Принять первые два значения (receive дважды). */
    suspend fun <T> firstTwo(channel: ReceiveChannel<T>): Pair<T, T> {
        return Pair(channel.receive(), channel.receive())
    }

    /** Л6. Сумма всех чисел канала. */
    suspend fun sumChannel(channel: ReceiveChannel<Int>): Int = channel.toList().sum()

    /** Л7. Число элементов в канале. */
    suspend fun <T> countChannel(channel: ReceiveChannel<T>): Int = channel.toList().count()

    /** Л8. Канал чётных: 2,4,...,2n. */
    fun CoroutineScope.produceEvens(n: Int): ReceiveChannel<Int> =
        produce {
            for (i in 1..n)
                send(2 * i)
        }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Звено пайплайна: выдай канал квадратов элементов входного канала.
     * Требования (проверяет тест):
     *  • на каждый вход x — выход x*x, в том же порядке;
     *  • выходной канал закрывается, когда закрылся входной.
     *
     * Спойлер: produce { for (x in input) send(x * x) }.
     */
    fun CoroutineScope.squares(input: ReceiveChannel<Int>): ReceiveChannel<Int> =
        produce { for (x in input) send(x * x) }

    /**
     * С10. Звено-map: выдай канал с transform(элемент) для каждого элемента входного.
     * Требования (проверяет тест):
     *  • порядок сохраняется, каждый элемент преобразован;
     *  • выход закрывается вслед за входом.
     *
     */
    fun <T, R> CoroutineScope.mapChannel(input: ReceiveChannel<T>, transform: (T) -> R): ReceiveChannel<R> =
        produce { for (n in input) send(transform(n)) }

    /**
     * С11. Звено-filter: выдай канал только тех элементов входного, что удовлетворяют предикату.
     * Требования (проверяет тест):
     *  • порядок сохраняется, непрошедшие отбрасываются;
     *  • выход закрывается вслед за входом.
     *
     * Спойлер: produce { for (x in input) if (predicate(x)) send(x) }.
     */
    fun <T> CoroutineScope.filterChannel(input: ReceiveChannel<T>, predicate: (T) -> Boolean): ReceiveChannel<T> =
        produce { for (n in input) if (predicate(n)) send(n) }

    /**
     * С12. Fan-in: слей два канала в один (все элементы обоих; порядок между источниками может чередоваться).
     * Требования (проверяет тест):
     *  • в выходе присутствуют все элементы a и b;
     *  • выход закрывается, когда оба источника исчерпаны.
     *
     * Спойлер: produce { launch { for (x in a) send(x) }; launch { for (x in b) send(x) } } (дождаться обоих).
     */
    fun <T> CoroutineScope.merge(a: ReceiveChannel<T>, b: ReceiveChannel<T>): ReceiveChannel<T> = produce {
        launch { launch { for (x in a) send(x) }; launch { for (x in b) send(x) } }
    }

    /**
     * С13. Звено take: пропусти первые n элементов входного канала, затем завершись.
     * Требования (проверяет тест):
     *  • ровно n элементов (или меньше, если вход короче);
     *  • после n апстрим ОТМЕНЯЕТСЯ (иначе он повиснет, продолжая слать).
     *
     * Спойлер: produce { for (x in input) { send(x); if (++cnt == n) break } }; затем input.cancel().
     */
    fun <T> CoroutineScope.takeChannel(input: ReceiveChannel<T>, n: Int): ReceiveChannel<T> {
        var count = 0
        return produce {
            for (x in input) {
                send(x)
                if (++count == n) break
            }
            input.cancel()
        }
    }

    /**
     * С14. Прими одно значение из канала; если канал уже закрыт — верни null (без исключения).
     * Требования (проверяет тест):
     *  • открытый канал → следующее значение;
     *  • закрытый → null, а не бросить ClosedReceiveChannelException.
     *
     * Спойлер: channel.receiveCatching().getOrNull().
     */
    suspend fun <T> receiveCatchingOrNull(channel: ReceiveChannel<T>): T? = channel.receiveCatching().getOrNull()

    /**
     * С15. Zip: попарно комбинируй элементы a и b, пока не закроется ЛЮБОЙ из каналов.
     * Требования (проверяет тест):
     *  • i-й выход = combine(a[i], b[i]);
     *  • длина = min(len a, len b); закрытие любого завершает выход.
     *
     * Спойлер: produce { while(true){ receiveCatching из a и b; если любой закрыт — break; send(combine) } }.
     */
    fun <A, B, R> CoroutineScope.zipChannels(
        a: ReceiveChannel<A>,
        b: ReceiveChannel<B>,
        combine: (A, B) -> R,
    ): ReceiveChannel<R> {
        return produce {
            while (true) {
                val a = a.receiveCatching().getOrNull() ?: break
                val b = b.receiveCatching().getOrNull() ?: break
                send(combine(a, b))
            }
        }
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Fan-out: раздай числа канала numbers между workers воркерами (каждый элемент — одному воркеру);
     *       каждый суммирует своё, итог = сумма всех воркеров.
     * Требования (проверяет тест):
     *  • каждый элемент обработан ровно одним воркером (несколько воркеров тянут из ОДНОГО канала);
     *  • итог = полная сумма канала.
     *
     * Спойлер: coroutineScope { List(workers){ async { var s=0; for (x in numbers) s+=x; s } }.awaitAll().sum() }.
     */
    suspend fun fanOutSum(numbers: ReceiveChannel<Int>, workers: Int): Int = coroutineScope {
        List(workers) {
            async {
                var s = 0
                for (x in numbers) {
                    s += x
                }
                s
            }
        }.awaitAll().sum()
    }

    /**
     * СЛ17. Многоступенчатый конвейер: числа 1..n → возвести в квадрат → прибавить 1. Верни выходной канал.
     * Требования (проверяет тест):
     *  • результат = [1²+1, 2²+1, ..., n²+1] в порядке;
     *  • ступени соединены каналами (каждая — отдельное звено).
     *
     * Спойлер: produce(1..n) → squares-звено → map(+1)-звено; верни последний канал.
     */
    fun CoroutineScope.pipelineSquaredPlusOne(n: Int): ReceiveChannel<Int> {
        val nums = produceNumbers(n)
        val sq = squares(nums)
        return mapChannel(sq) { it + 1 }
    }

    /**
     * СЛ18. Fan-in: слей МНОГО каналов (sources) в один.
     * Требования (проверяет тест):
     *  • выход содержит все элементы всех источников;
     *  • выход закрывается, когда исчерпаны все источники.
     *
     * Спойлер: produce { sources.forEach { src -> launch { for (x in src) send(x) } } } (дождаться всех).
     */
    fun <T> CoroutineScope.fanInMerge(sources: List<ReceiveChannel<T>>): ReceiveChannel<T> =
        produce {
            for (src in sources) launch { for (x in src) send(x) }
        }

    /**
     * СЛ19. Раздай числа канала между workers воркерами; верни список ЧАСТИЧНЫХ сумм (по одной на воркера).
     * Требования (проверяет тест):
     *  • каждый элемент попадает ровно к одному воркеру;
     *  • список из workers сумм, их общая сумма = полной сумме канала.
     *
     * Спойлер: coroutineScope { List(workers){ async { var s=0; for (x in numbers) s+=x; s } }.awaitAll() }.
     */
    suspend fun distributeAndSum(numbers: ReceiveChannel<Int>, workers: Int): List<Int> =
        coroutineScope { List(workers) { async { var s = 0; for (x in numbers) s += x; s } }.awaitAll() }

    /**
     * СЛ20. Верни ПЕРВОЕ доступное значение из a или b — что придёт раньше.
     * Требования (проверяет тест):
     *  • если в одном канале значение есть, а другой молчит — вернуть из первого, не блокируясь на втором.
     *
     * Спойлер: select { a.onReceive { it }; b.onReceive { it } }.
     */
    suspend fun <T> selectFirst(a: ReceiveChannel<T>, b: ReceiveChannel<T>): T =
        select { a.onReceive { it }; b.onReceive { it } }
}
