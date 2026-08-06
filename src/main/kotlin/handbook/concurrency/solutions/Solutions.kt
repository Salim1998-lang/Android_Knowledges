package handbook.concurrency.solutions

import handbook.concurrency.IntBox
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Эталонные решения темы 9. Подсмотри, если застрял с [handbook.concurrency.ConcurrencyTasks]. */
object ConcurrencySolutions {

    // ── Лёгкие ──

    suspend fun <T> guard(mutex: Mutex, block: () -> T): T = mutex.withLock { block() }

    suspend fun mutexIncrements(times: Int): Int {
        val mutex = Mutex()
        var counter = 0
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) { launch { mutex.withLock { counter++ } } }
            }
        }
        return counter
    }

    suspend fun atomicIncrements(times: Int): Int {
        val counter = AtomicInteger(0)
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) { launch { counter.incrementAndGet() } }
            }
        }
        return counter.get()
    }

    suspend fun safeAppendAll(values: List<Int>): List<Int> {
        val mutex = Mutex()
        val list = mutableListOf<Int>()
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (v in values) launch { mutex.withLock { list.add(v) } }
            }
        }
        return list
    }

    suspend fun sumConcurrently(numbers: List<Int>): Int {
        val mutex = Mutex()
        var sum = 0
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (x in numbers) launch { mutex.withLock { sum += x } }
            }
        }
        return sum
    }

    fun atomicAddAndGet(counter: AtomicInteger, delta: Int): Int = counter.addAndGet(delta)

    fun setIfZero(counter: AtomicInteger, newValue: Int): Boolean = counter.compareAndSet(0, newValue)

    suspend fun mutexDecrements(start: Int, times: Int): Int {
        val mutex = Mutex()
        var counter = start
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) { launch { mutex.withLock { counter-- } } }
            }
        }
        return counter
    }

    // ── Средние ──

    suspend fun concurrentReadModifyWrite(box: IntBox, times: Int): Int {
        val mutex = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) { launch { mutex.withLock { box.value = box.value + 1 } } }
            }
        }
        return box.value
    }

    suspend fun confinedCounterFine(times: Int): Int {
        val ctx = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            var counter = 0
            withContext(Dispatchers.Default) {
                coroutineScope {
                    repeat(times) { launch { withContext(ctx) { counter++ } } }
                }
            }
            return counter
        } finally {
            ctx.close()
        }
    }

    suspend fun confinedCounterCoarse(times: Int): Int {
        val ctx = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            var counter = 0
            withContext(ctx) {
                coroutineScope {
                    repeat(times) { launch { counter++ } }
                }
            }
            return counter
        } finally {
            ctx.close()
        }
    }

    suspend fun wordFrequencies(words: List<String>): Map<String, Int> {
        val mutex = Mutex()
        val freq = mutableMapOf<String, Int>()
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (w in words) launch { mutex.withLock { freq[w] = (freq[w] ?: 0) + 1 } }
            }
        }
        return freq
    }

    suspend fun atomicMax(candidates: List<Int>): Int {
        require(candidates.isNotEmpty())
        val max = AtomicInteger(Int.MIN_VALUE)
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (x in candidates) launch {
                    while (true) {
                        val cur = max.get()
                        if (x <= cur) break
                        if (max.compareAndSet(cur, x)) break
                    }
                }
            }
        }
        return max.get()
    }

    suspend fun perKeyCounts(keys: List<String>): Map<String, Int> {
        val counters = keys.distinct().associateWith { AtomicInteger(0) }
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (k in keys) launch { counters.getValue(k).incrementAndGet() }
            }
        }
        return counters.mapValues { it.value.get() }
    }

    suspend fun applyTransfers(balances: IntArray, transfers: List<Triple<Int, Int, Int>>): IntArray {
        val mutex = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                for ((from, to, amount) in transfers) launch {
                    mutex.withLock {
                        balances[from] -= amount
                        balances[to] += amount
                    }
                }
            }
        }
        return balances
    }

    // ── Сложные ──

    private sealed interface Msg
    private data class Inc(val by: Int) : Msg
    private data class Get(val reply: CompletableDeferred<Int>) : Msg

    suspend fun counterActorFinalValue(times: Int): Int = coroutineScope {
        val ch = Channel<Msg>()
        val owner = launch {
            var state = 0
            for (msg in ch) when (msg) {
                is Inc -> state += msg.by
                is Get -> msg.reply.complete(state)
            }
        }
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(times) { launch { ch.send(Inc(1)) } }
            }
        }
        val reply = CompletableDeferred<Int>()
        ch.send(Get(reply))
        val result = reply.await()
        ch.close()
        owner.join()
        result
    }

    suspend fun stripedIncrement(keys: List<String>, perKey: Int): Map<String, Int> {
        val distinct = keys.distinct()
        val locks = distinct.associateWith { Mutex() }
        val counts = distinct.associateWith { IntBox() }
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (key in distinct) repeat(perKey) {
                    launch { locks.getValue(key).withLock { counts.getValue(key).value++ } }
                }
            }
        }
        return counts.mapValues { it.value.value }
    }

    suspend fun runInitOnce(callers: Int, initializer: () -> Int): Int {
        val mutex = Mutex()
        var value: Int? = null
        var initCount = 0
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(callers) {
                    launch {
                        if (value == null) {
                            mutex.withLock {
                                if (value == null) {          // double-check под замком
                                    initCount++
                                    value = initializer()
                                }
                            }
                        }
                    }
                }
            }
        }
        return initCount
    }

    private class Node<T>(val value: T, val next: Node<T>?)

    suspend fun treiberStackRoundTrip(values: List<Int>): List<Int> {
        val top = AtomicReference<Node<Int>?>(null)

        fun push(v: Int) {
            while (true) {
                val old = top.get()
                if (top.compareAndSet(old, Node(v, old))) return
            }
        }

        fun pop(): Int? {
            while (true) {
                val old = top.get() ?: return null
                if (top.compareAndSet(old, old.next)) return old.value
            }
        }

        // Конкурентный push всех значений.
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (v in values) launch { push(v) }
            }
        }
        // Конкурентный pop; собираем извлечённое в общий список под Mutex.
        val out = mutableListOf<Int>()
        val outMutex = Mutex()
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(values.size) {
                    launch {
                        val x = pop()
                        if (x != null) outMutex.withLock { out.add(x) }
                    }
                }
            }
        }
        return out
    }

    suspend fun parallelSumLocalAgg(numbers: List<Int>, workers: Int): Int {
        require(workers >= 1)
        val mutex = Mutex()
        var total = 0
        val chunkSize = (numbers.size + workers - 1) / workers.coerceAtLeast(1)
        val chunks = if (numbers.isEmpty()) emptyList() else numbers.chunked(chunkSize.coerceAtLeast(1))
        withContext(Dispatchers.Default) {
            coroutineScope {
                for (chunk in chunks) launch {
                    var local = 0
                    for (x in chunk) local += x
                    mutex.withLock { total += local }
                }
            }
        }
        return total
    }
}
