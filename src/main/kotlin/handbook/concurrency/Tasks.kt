package handbook.concurrency

import handbook.concurrency.solutions.ConcurrencySolutions.Get
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Общий изменяемый держатель — «ящик» для задач про read-modify-write. */
class IntBox(var value: Int = 0)

/**
 * Тема 9 «Разделяемое изменяемое состояние (Mutex)» — 20 задач.
 * Реализуй функции (замени `TODO()`). Проверка: `./gradlew test --tests "handbook.concurrency.*"`.
 * Эталон — в [handbook.concurrency.solutions.ConcurrencySolutions].
 *
 * Многие задачи запускают реально параллельные корутины на [kotlinx.coroutines.Dispatchers.Default]
 * — именно там проявляются гонки. Правильная реализация даёт ВЕРНЫЙ результат при любом
 * планировании. Подсказка по импортам: `sync.Mutex`, `sync.withLock`, `Dispatchers.Default`,
 * `withContext`, `coroutineScope`, `launch`, `async`, `awaitAll`, `channels.Channel`,
 * `java.util.concurrent.atomic.AtomicInteger`/`AtomicReference`.
 */
object ConcurrencyTasks {

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Выполни block под замком и верни его результат (withLock возвращает значение). */
    suspend fun <T> guard(mutex: Mutex, block: () -> T): T = mutex.withLock { block() }

    /**
     * Л2. Запусти `times` корутин на Dispatchers.Default; каждая увеличивает общий счётчик на 1
     *     ПОД Mutex. Верни итог (== times).
     * Спойлер: val m = Mutex(); var c = 0; withContext(Default){ coroutineScope{ repeat(times){ launch{ m.withLock{ c++ } } } } }; c.
     */
    suspend fun mutexIncrements(times: Int): Int {
        val m = Mutex()
        var c = 0
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) {
                    launch { m.withLock { c++ } }
                }
            }
        }
        return c
    }

    /**
     * Л3. То же, но через AtomicInteger вместо Mutex.
     * Спойлер: val c = AtomicInteger(0); ... launch { c.incrementAndGet() }; c.get().
     */
    suspend fun atomicIncrements(times: Int): Int {
        val c = AtomicInteger(0)
        val m = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) {
                    launch {
                        m.withLock { c.updateAndGet { it + 1 } }
                    }
                }
            }
        }
        return c.get()
    }

    /**
     * Л4. Конкурентно (по корутине на элемент) добавь каждый элемент values в общий список ПОД Mutex.
     *     Верни получившийся список (порядок не важен — тест сверяет множество и размер).
     */
    suspend fun safeAppendAll(values: List<Int>): List<Int> {
        val mutex = Mutex()
        val resultList = mutableListOf<Int>()
        coroutineScope {
            values.forEach { value ->
                launch(Dispatchers.Default) {
                    mutex.withLock { resultList.add(value) }
                }
            }
        }
        return resultList
    }

    /**
     * Л5. Конкурентно просуммируй числа: по корутине на элемент, прибавление к общей сумме ПОД Mutex.
     */
    suspend fun sumConcurrently(numbers: List<Int>): Int {
        val sum = AtomicInteger(0)
        withContext(Dispatchers.Default) {
            coroutineScope {
                numbers.forEach { value ->
                    launch {
                        sum.updateAndGet { it + value }
                    }
                }
            }
        }
        return sum.get()
    }

    /** Л6. Атомарно прибавь delta и верни новое значение. Спойлер: counter.addAndGet(delta). */
    fun atomicAddAndGet(counter: AtomicInteger, delta: Int): Int = counter.addAndGet(delta)

    /**
     * Л7. Установи newValue, только если счётчик сейчас равен 0; верни, удалось ли.
     * Спойлер: counter.compareAndSet(0, newValue).
     */
    fun setIfZero(counter: AtomicInteger, newValue: Int): Boolean = counter.compareAndSet(0, newValue)

    /**
     * Л8. Запусти `times` корутин; каждая уменьшает счётчик (старт = start) на 1 ПОД Mutex. Верни итог.
     * Спойлер: как Л2, но c-- ; итог == start - times.
     */
    suspend fun mutexDecrements(start: Int, times: Int): Int {
        val m = Mutex()
        var c = start
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) {
                    launch {
                        m.withLock { c-- }
                    }
                }
            }
        }
        return c
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Конкурентно выполни `times` раз read-modify-write над box: box.value = box.value + 1.
     *     ВСЯ тройка (прочитать-прибавить-записать) должна быть под ОДНИМ Mutex. Верни box.value.
     * Требования (проверяет тест): итог == times (без потерянных приращений).
     *
     * Спойлер: val m = Mutex(); withContext(Default){ coroutineScope{ repeat(times){ launch{ m.withLock{ box.value = box.value + 1 } } } } }; box.value.
     */
    suspend fun concurrentReadModifyWrite(box: IntBox, times: Int): Int {
        val m = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) {
                    launch {
                        m.withLock { box.value += 1 }
                    }
                }
            }
        }
        return box.value
    }

    /**
     * С10. ТОНКОЕ замыкание на поток: каждый инкремент гони на один single-thread диспетчер.
     * Требования (проверяет тест): итог == times; синхронизация — только через замыкание (без Mutex/атомика).
     *
     * Спойлер: val ctx = Executors.newSingleThreadExecutor().asCoroutineDispatcher(); try{ ... launch{ withContext(ctx){ c++ } } } finally { ctx.close() }.
     */
    suspend fun confinedCounterFine(times: Int): Int {
        val ctx = Dispatchers.Default.limitedParallelism(1)
        try {
            var count = 0
            withContext(Dispatchers.Default) {
                coroutineScope {
                    repeat(times) {
                        launch {
                            withContext(ctx) {
                                count++
                            }
                        }
                    }
                }
            }
            return count
        } finally {
            ctx.cancel()
        }
    }

    /**
     * С11. ГРУБОЕ замыкание: весь блок с инкрементами выполни на одном single-thread диспетчере.
     * Требования (проверяет тест): итог == times; один переход контекста на весь блок, не на каждый ++.
     *
     * Спойлер: withContext(ctx){ coroutineScope{ repeat(times){ launch{ c++ } } } }.
     */
    suspend fun confinedCounterCoarse(times: Int): Int {
        val ctx = Dispatchers.Default.limitedParallelism(1)
        try {
            var count = 0
            withContext(ctx) {
                coroutineScope {
                    repeat(times) {
                        launch {
                            count++
                        }
                    }
                }
            }
            return count
        } finally {
            ctx.cancel()
        }
    }

    /**
     * С12. Конкурентно посчитай частоты слов (по корутине на слово), общий MutableMap под Mutex.
     * Требования (проверяет тест): map[слово] == число вхождений; сумма значений == words.size.
     *
     * Спойлер: m.withLock { map[w] = (map[w] ?: 0) + 1 } — RMW над map целиком под замком.
     */
    suspend fun wordFrequencies(words: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val m = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (word in words) {
                    launch {
                        m.withLock { map[word] = map.getOrDefault(word, 0) + 1 }
                    }
                }
            }
        }
        return map
    }

    /**
     * С13. Конкурентно найди максимум candidates, обновляя общий AtomicInteger CAS-циклом.
     * Требования (проверяет тест): итог == candidates.max(); обновление — неблокирующее (compareAndSet в цикле).
     *
     * Спойлер: launch на элемент; while(true){ val cur=a.get(); if(x<=cur) break; if(a.compareAndSet(cur,x)) break }.
     */
    suspend fun atomicMax(candidates: List<Int>): Int {
        val a = AtomicInteger(0)
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (candidate in candidates) {
                    launch {
                        while (true) {
                            val cur = a.get()
                            if (candidate <= cur) {
                                break
                            }
                            if (a.compareAndSet(cur, candidate)) {
                                break
                            }
                        }
                    }
                }
            }
        }
        return a.get()
    }

    /**
     * С14. Per-key атомики: на каждый ключ — свой AtomicInteger (предзаполнен нулями). Конкурентно
     *     инкрементируй счётчик соответствующего ключа для КАЖДОГО элемента keys. Верни срез Map<ключ, счётчик>.
     * Требования (проверяет тест): значение ключа == число его вхождений; общий Mutex НЕ нужен (атомик на ключ).
     *
     * Спойлер: val counters = keys.distinct().associateWith { AtomicInteger(0) }; launch на элемент { counters[k]!!.incrementAndGet() }.
     */
    suspend fun perKeyCounts(keys: List<String>): Map<String, Int> {
        val counters = keys.distinct().associateWith { AtomicInteger(0) }
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (key in keys) {
                    launch {
                        counters.getValue(key).incrementAndGet()
                    }
                }
            }
        }
        return counters.mapValues { it.value.get() }
    }

    /**
     * С15. Конкурентно применить переводы к balances: каждый transfer (from, to, amount) списывает
     *     amount со счёта from и зачисляет на to. Вся пара изменений — ПОД одним Mutex (составной инвариант).
     *     Верни итоговые балансы.
     * Требования (проверяет тест): сумма балансов сохраняется; конкретные значения детерминированы.
     *
     * Спойлер: m.withLock { balances[from]-=amount; balances[to]+=amount } — обе строки в одной критической секции.
     */
    suspend fun applyTransfers(balances: IntArray, transfers: List<Triple<Int, Int, Int>>): IntArray {
        val m = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                for ((from, to, amount) in transfers) {
                    launch {
                        m.withLock {
                            balances[from] -= amount
                            balances[to] += amount
                        }
                    }
                }
            }
        }
        return balances
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Actor-паттерн: одна корутина владеет счётчиком и принимает команды через Channel; `times`
     *      корутин шлют ей команду «увеличить». В конце запроси значение и верни его.
     * Требования (проверяет тест): итог == times; состояние трогает ТОЛЬКО корутина-владелец.
     *
     * Спойлер: Channel<Msg>; launch { var s=0; for(m in ch) when(m){ Inc->s++; is Get->m.reply.complete(s) } };
     *          отправить times × Inc (дождаться), затем Get с CompletableDeferred; закрыть канал.
     */

    private sealed interface Msg
    private data class Inc(val value: Int) : Msg
    private data class Get(val reply: CompletableDeferred<Int>) : Msg

    suspend fun counterActorFinalValue(times: Int): Int = coroutineScope {
        val ch = Channel<Msg>()
        val owner = launch {
            var state = 0
            for (msg in ch) {
                when (msg) {
                    is Inc -> state += msg.value
                    is Get -> msg.reply.complete(state)
                }
            }
        }
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) {
                    launch {
                        ch.send(Inc(1))
                    }
                }
            }
        }
        val reply = CompletableDeferred<Int>()
        ch.send(Get(reply))
        val result = reply.await()
        ch.close()
        owner.join()
        result
    }

    /**
     * СЛ17. Lock striping: на каждый ключ — свой Mutex. Для каждого ключа выполни perKey конкурентных
     *      инкрементов его счётчика (RMW под mutex ЭТОГО ключа). Верни Map<ключ, счётчик>.
     * Требования (проверяет тест): каждое значение == perKey; разные ключи не мешают друг другу.
     *
     * Спойлер: val locks = keys.associateWith { Mutex() }; val counts = keys.associateWith { IntBox() };
     *          launch на (key, повтор) { locks[key]!!.withLock { counts[key]!!.value++ } }.
     */
    suspend fun stripedIncrement(keys: List<String>, perKey: Int): Map<String, Int> =
        TODO()

    /**
     * СЛ18. Ленивая инициализация «ровно один раз»: `callers` корутин конкурентно требуют значение,
     *      но тяжёлый initializer должен выполниться РОВНО ОДИН раз (double-checked под Mutex).
     *      Верни, сколько раз initializer реально вызвался (должно быть 1).
     * Требования (проверяет тест): результат == 1 при любом числе конкурентных вызовов.
     *
     * Спойлер: var value: Int? = null; val m = Mutex(); каждый вызывающий:
     *          if(value==null) m.withLock { if(value==null){ initCount++; value=initializer() } }.
     */
    suspend fun runInitOnce(callers: Int, initializer: () -> Int): Int =
        TODO()

    /**
     * СЛ19. Lock-free стек Трайбера на AtomicReference+CAS: конкурентно затолкни все values (push),
     *      затем конкурентно вытолкни столько же (pop). Верни список извлечённых значений.
     * Требования (проверяет тест): множество извлечённых == множеству values, размер совпадает.
     *
     * Спойлер: узел Node(value, next); top: AtomicReference<Node?>;
     *          push: loop{ old=top.get(); n=Node(v,old); if(top.compareAndSet(old,n)) return };
     *          pop:  loop{ old=top.get() ?: return null; if(top.compareAndSet(old,old.next)) return old.value }.
     */
    suspend fun treiberStackRoundTrip(values: List<Int>): List<Int> =
        TODO()

    /**
     * СЛ20. Параллельная свёртка с ЛОКАЛЬНОЙ агрегацией: раздели numbers на `workers` частей; каждый
     *      воркер копит свою сумму БЕЗ синхронизации и вливает её в общий total ОДИН раз под Mutex.
     * Требования (проверяет тест): total == numbers.sum(); блокировка — раз на воркера, не на элемент.
     *
     * Спойлер: chunks = numbers.chunked(...); var total=0; m=Mutex();
     *          coroutineScope{ chunks.forEach{ c-> launch{ var local=0; for(x in c) local+=x; m.withLock{ total+=local } } } }.
     */
    suspend fun parallelSumLocalAgg(numbers: List<Int>, workers: Int): Int =
        TODO()
}
