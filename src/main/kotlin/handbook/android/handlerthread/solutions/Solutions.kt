package handbook.android.handlerthread.solutions

import handbook.android.handlerthread.LooperThread
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Эталонные решения темы 2. Подсмотри, если застрял с [handbook.android.handlerthread.HandlerThreadTasks]. */
object HandlerThreadSolutions {

    private fun <T> syncList(): MutableList<T> = Collections.synchronizedList(mutableListOf())

    // ── Лёгкие ──

    fun runAndCollect(items: List<String>): List<String> {
        val out = syncList<String>()
        val lt = LooperThread("worker").apply { start() }
        items.forEach { item -> lt.post { out.add(item) } }
        lt.quitSafely(); lt.join()
        return out.toList()
    }

    fun sameThreadForAll(count: Int): Boolean {
        val ids = syncList<Long>()
        val lt = LooperThread("worker").apply { start() }
        repeat(count) { lt.post { ids.add(Thread.currentThread().id) } }
        lt.quitSafely(); lt.join()
        return ids.toSet().size == 1
    }

    fun runsOffCallerThread(): Boolean {
        val callerId = Thread.currentThread().id
        val same = AtomicBoolean(true)
        val lt = LooperThread("worker").apply { start() }
        lt.post { same.set(Thread.currentThread().id == callerId) }
        lt.quitSafely(); lt.join()
        return !same.get()
    }

    fun postBeforeStartRejected(): Boolean {
        val lt = LooperThread("worker")
        val accepted = lt.post { }      // ещё не start() — Looper'а нет
        lt.start(); lt.quitSafely(); lt.join()
        return !accepted
    }

    fun postAfterQuitRejected(): Boolean {
        val lt = LooperThread("worker").apply { start() }
        lt.quitSafely(); lt.join()
        val accepted = lt.post { }      // после quit — отвергнуто
        return !accepted
    }

    fun confinedCounter(times: Int): Int {
        val box = intArrayOf(0)          // НЕ volatile, без локов — сериализация делает это безопасным
        val lt = LooperThread("worker").apply { start() }
        repeat(times) { lt.post { box[0] = box[0] + 1 } }
        lt.quitSafely(); lt.join()
        return box[0]
    }

    fun observedThreadName(name: String): String {
        val seen = arrayOfNulls<String>(1)
        val lt = LooperThread(name).apply { start() }
        lt.post { seen[0] = Thread.currentThread().name }
        lt.quitSafely(); lt.join()
        return seen[0]!!
    }

    fun joinIsCompletionBarrier(): Boolean {
        val done = booleanArrayOf(false)
        val lt = LooperThread("worker").apply { start() }
        lt.post { done[0] = true }
        lt.quitSafely(); lt.join()       // join() гарантирует, что задача завершилась и запись видна
        return done[0]
    }

    // ── Средние ──

    fun quitSafelyDrainsBacklog(backlog: List<String>): List<String> {
        val out = syncList<String>()
        val gate = CountDownLatch(1)
        val lt = LooperThread("worker").apply { start() }
        lt.post { gate.await() }                       // держим поток, пока набиваем очередь
        backlog.forEach { item -> lt.post { out.add(item) } }
        lt.quitSafely()                                // мягко: доработать бэклог
        gate.countDown()
        lt.join()
        return out.toList()
    }

    fun quitDropsBacklog(backlog: List<String>): List<String> {
        val out = syncList<String>()
        val started = CountDownLatch(1)                // gate-задача уже ВЗЯТА воркером
        val gate = CountDownLatch(1)
        val lt = LooperThread("worker").apply { start() }
        lt.post { out.add("gate"); started.countDown(); gate.await() }
        started.await()                                // теперь backlog точно встанет ПОЗАДИ gate-задачи
        backlog.forEach { item -> lt.post { out.add(item) } }
        lt.quit()                                      // жёстко: бэклог отброшен
        gate.countDown()
        lt.join()
        return out.toList()                            // только "gate"
    }

    fun roundTripToMain(input: Int): Int {
        val result = AtomicInteger(Int.MIN_VALUE)
        val main = LooperThread("main").apply { start() }
        val bg = LooperThread("bg").apply { start() }
        bg.post {
            val computed = input * 10                  // тяжёлая работа на фоне
            main.post { result.set(computed) }         // результат — обратно на "главный"
        }
        bg.quitSafely(); bg.join()                     // bg точно отправил в main
        main.quitSafely(); main.join()
        return result.get()
    }

    fun sharedLooperFifo(aItems: List<String>, bItems: List<String>): List<String> {
        val out = syncList<String>()
        val lt = LooperThread("worker").apply { start() }
        // Два «Handler'а» (a и b) кладут в ОДНУ очередь одного Looper'а — порядок глобально FIFO.
        val n = maxOf(aItems.size, bItems.size)
        for (i in 0 until n) {
            aItems.getOrNull(i)?.let { item -> lt.post { out.add(item) } }
            bItems.getOrNull(i)?.let { item -> lt.post { out.add(item) } }
        }
        lt.quitSafely(); lt.join()
        return out.toList()
    }

    fun handoffCount(jobs: Int): Int {
        val counter = AtomicInteger()
        val worker = LooperThread("worker").apply { start() }
        val producer = Thread { repeat(jobs) { worker.post { counter.incrementAndGet() } } }
        producer.start(); producer.join()              // все задания поставлены (кросс-поточно)
        worker.quitSafely(); worker.join()
        return counter.get()
    }

    fun backpressureOrder(items: List<String>): List<String> {
        val out = syncList<String>()
        val gate = CountDownLatch(1)
        val lt = LooperThread("worker").apply { start() }
        lt.post { gate.await() }                       // медленный обработчик — очередь копится
        items.forEach { item -> lt.post { out.add(item) } }
        gate.countDown()                               // отпускаем — единственный поток разгребает по порядку
        lt.quitSafely(); lt.join()
        return out.toList()
    }

    fun shardAcrossTwo(items: List<Int>): Pair<Int, Int> {
        val even = intArrayOf(0)
        val odd = intArrayOf(0)
        val a = LooperThread("shard-even").apply { start() }
        val b = LooperThread("shard-odd").apply { start() }
        items.forEach { x ->
            if (x % 2 == 0) a.post { even[0]++ } else b.post { odd[0]++ }
        }
        a.quitSafely(); b.quitSafely(); a.join(); b.join()
        return even[0] to odd[0]
    }

    // ── Сложные ──

    fun twoStagePipeline(input: List<Int>): List<Int> {
        val out = syncList<Int>()
        val stage2 = LooperThread("stage2").apply { start() }
        val stage1 = LooperThread("stage1").apply { start() }
        input.forEach { x ->
            stage1.post {
                val a = x + 1
                stage2.post { out.add(a * 2) }         // передаём дальше по конвейеру
            }
        }
        stage1.quitSafely(); stage1.join()             // stage1 всё отправил в stage2
        stage2.quitSafely(); stage2.join()
        return out.toList()
    }

    fun pingPong(rounds: Int): Int {
        val a = LooperThread("A").apply { start() }
        val b = LooperThread("B").apply { start() }
        val count = AtomicInteger()
        val done = CountDownLatch(1)
        lateinit var toA: () -> Unit
        lateinit var toB: () -> Unit
        toA = { a.post { if (count.incrementAndGet() >= rounds) done.countDown() else toB() } }
        toB = { b.post { if (count.incrementAndGet() >= rounds) done.countDown() else toA() } }
        if (rounds > 0) toA() else done.countDown()
        done.await()
        a.quitSafely(); b.quitSafely(); a.join(); b.join()
        return count.get()
    }

    fun requestResponse(ids: List<Int>): List<Pair<Int, Int>> {
        val replies = syncList<Pair<Int, Int>>()
        val caller = LooperThread("caller").apply { start() }
        val server = LooperThread("server").apply { start() }
        ids.forEach { id ->
            server.post {
                val answer = id * id
                caller.post { replies.add(id to answer) }   // ответ с корреляцией по id
            }
        }
        server.quitSafely(); server.join()
        caller.quitSafely(); caller.join()
        return replies.toList()
    }

    fun cancelWorkPastLifecycle(items: List<String>, cancelAfter: Int): List<String> {
        val ownerAlive = AtomicBoolean(true)
        val processed = syncList<String>()
        val lt = LooperThread("owner").apply { start() }
        items.forEach { item ->
            lt.post {
                if (!ownerAlive.get()) return@post          // владелец (экран) уже уничтожен — не трогаем
                processed.add(item)
                if (processed.size >= cancelAfter) ownerAlive.set(false)
            }
        }
        lt.quitSafely(); lt.join()
        return processed.toList()
    }

    fun reusesSingleThread(batches: Int, perBatch: Int): Int {
        val ids = syncList<Long>()
        val lt = LooperThread("worker").apply { start() }
        repeat(batches) {
            repeat(perBatch) { lt.post { ids.add(Thread.currentThread().id) } }
        }
        lt.quitSafely(); lt.join()
        return ids.toSet().size                              // всё обслужил ОДИН поток → 1
    }
}
