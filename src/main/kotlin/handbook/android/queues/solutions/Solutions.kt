package handbook.android.queues.solutions

import handbook.android.queues.Event
import handbook.android.queues.POISON
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Эталонные решения темы 5. Подсмотри, если застрял с [handbook.android.queues.QueuesTasks]. */
object QueuesSolutions {

    private fun <T> syncList(): MutableList<T> = Collections.synchronizedList(mutableListOf())

    // ═══════════ Лёгкие: механика BlockingQueue (настоящие потоки) ═══════════

    fun offerToBounded(capacity: Int, n: Int): Int {
        val q = ArrayBlockingQueue<Int>(capacity)
        var accepted = 0
        repeat(n) { if (q.offer(it)) accepted++ }   // offer НЕ блокирует: вернёт false, когда полно
        return accepted
    }

    fun pollEmptyIsNull(): Boolean {
        val q = LinkedBlockingQueue<Int>()
        return q.poll() == null                      // poll НЕ блокирует (в отличие от take)
    }

    fun drainAll(items: List<Int>): List<Int> {
        val q = LinkedBlockingQueue<Int>()
        items.forEach { q.put(it) }
        val out = mutableListOf<Int>()
        q.drainTo(out)                               // забрать всё пачкой
        return out
    }

    fun producerConsumerPoison(items: List<Int>): List<Int> {
        val q = LinkedBlockingQueue<Int>()
        val out = syncList<Int>()
        val consumer = Thread {
            while (true) {
                val x = q.take()                     // take БЛОКИРУЕТ, пока пусто
                if (x == POISON) break
                out.add(x)
            }
        }
        consumer.start()
        items.forEach { q.put(it) }
        q.put(POISON)
        consumer.join()
        return out.toList()
    }

    fun losslessBackpressure(items: List<Int>): List<Int> {
        val q = ArrayBlockingQueue<Int>(2)           // маленький буфер
        val out = syncList<Int>()
        val consumer = Thread {
            while (true) {
                val x = q.take()
                if (x == POISON) break
                out.add(x)
            }
        }
        consumer.start()
        items.forEach { q.put(it) }                  // put БЛОКИРУЕТ на полном буфере → продюсер тормозится
        q.put(POISON)
        consumer.join()
        return out.toList()                          // ничего не потеряно, порядок сохранён
    }

    fun offerTimeoutFails(): Boolean {
        val q = ArrayBlockingQueue<Int>(1)
        q.put(1)                                     // заполнили
        val accepted = q.offer(2, 30, TimeUnit.MILLISECONDS) // подождёт и сдастся (потребителя нет)
        return !accepted
    }

    fun synchronousHandoff(value: Int): Int {
        val q = SynchronousQueue<Int>()              // ёмкость 0: put ждёт take
        val holder = AtomicInteger(0)
        val consumer = Thread { holder.set(q.take()) }
        consumer.start()
        q.put(value)                                 // разблокируется только когда consumer возьмёт
        consumer.join()
        return holder.get()
    }

    fun fanInMerge(a: List<Int>, b: List<Int>): Int {
        val q = LinkedBlockingQueue<Int>()
        val p1 = Thread { a.forEach { q.put(it) } }
        val p2 = Thread { b.forEach { q.put(it) } }
        p1.start(); p2.start(); p1.join(); p2.join()
        val out = mutableListOf<Int>()
        q.drainTo(out)
        return out.size
    }

    // ═══════════ Средние: стратегии backpressure (чистые модели) + fan-out ═══════════

    fun fanOutTotal(items: List<Int>, workers: Int): Int {
        val q = LinkedBlockingQueue<Int>()
        val consumed = AtomicInteger(0)
        val threads = (1..workers).map {
            Thread {
                while (true) {
                    val x = q.take()
                    if (x == POISON) break
                    consumed.incrementAndGet()
                }
            }
        }
        threads.forEach { it.start() }
        items.forEach { q.put(it) }
        repeat(workers) { q.put(POISON) }            // по «пилюле» на каждого потребителя
        threads.forEach { it.join() }
        return consumed.get()                        // каждый элемент взят ровно раз → == items.size
    }

    fun conflateLatest(events: List<Event>, serviceMs: Long): List<Int> {
        val delivered = mutableListOf<Int>()
        var free = 0L
        var i = 0
        val n = events.size
        while (i < n) {
            val t = maxOf(free, events[i].timeMs)    // момент, когда потребитель может взять значение
            var j = i
            while (j + 1 < n && events[j + 1].timeMs <= t) j++ // конфляция: берём САМОЕ СВЕЖЕЕ из накопившихся
            delivered.add(events[j].value)
            free = t + serviceMs
            i = j + 1
        }
        return delivered
    }

    fun throttleFirst(events: List<Event>, intervalMs: Long): List<Event> {
        val out = mutableListOf<Event>()
        var lastEmit: Long? = null
        for (e in events) {
            if (lastEmit == null || e.timeMs - lastEmit >= intervalMs) {
                out.add(e)
                lastEmit = e.timeMs
            }
        }
        return out
    }

    fun debounce(events: List<Event>, quietMs: Long): List<Event> {
        val out = mutableListOf<Event>()
        for (i in events.indices) {
            val next = events.getOrNull(i + 1)
            if (next == null || next.timeMs - events[i].timeMs >= quietMs) {
                out.add(Event(events[i].timeMs + quietMs, events[i].value))
            }
        }
        return out
    }

    fun sampleLatest(events: List<Event>, periodMs: Long): List<Event> {
        if (events.isEmpty()) return emptyList()
        val maxTime = events.last().timeMs
        val out = mutableListOf<Event>()
        var tick = periodMs
        while (tick - periodMs < maxTime) {
            val inWindow = events.filter { it.timeMs > tick - periodMs && it.timeMs <= tick }
            if (inWindow.isNotEmpty()) out.add(Event(tick, inWindow.last().value)) // последнее в окне
            tick += periodMs
        }
        return out
    }

    fun dropOldest(values: List<Int>, capacity: Int): List<Int> {
        val buf = ArrayDeque<Int>()
        for (v in values) {
            buf.addLast(v)
            if (buf.size > capacity) buf.removeFirst()   // переполнение → выбросить самое старое
        }
        return buf.toList()
    }

    fun dropLatest(values: List<Int>, capacity: Int): List<Int> {
        val buf = mutableListOf<Int>()
        for (v in values) {
            if (buf.size < capacity) buf.add(v)          // переполнение → выбросить НОВОЕ
        }
        return buf
    }

    // ═══════════ Сложные: батчинг, credit-based, композиция ═══════════

    fun batchByCount(values: List<Int>, size: Int): List<List<Int>> =
        values.chunked(size)

    fun batchByTime(events: List<Event>, windowMs: Long): List<List<Int>> {
        if (events.isEmpty()) return emptyList()
        return events
            .groupBy { it.timeMs / windowMs }            // индекс окна
            .toSortedMap()
            .map { (_, group) -> group.map { it.value } }
    }

    fun creditBasedEmits(requests: List<Int>, itemCount: Int): List<Int> {
        var credits = 0
        var emitted = 0
        val out = mutableListOf<Int>()
        for (r in requests) {
            credits += r
            val canEmit = minOf(credits, itemCount - emitted) // продюсер не обгоняет спрос
            emitted += canEmit
            credits -= canEmit
            out.add(emitted)
        }
        return out
    }

    fun throttleThenBatch(events: List<Event>, intervalMs: Long, batchSize: Int): List<List<Int>> {
        val throttled = throttleFirst(events, intervalMs).map { it.value }
        return batchByCount(throttled, batchSize)
    }

    fun conflationDropCount(events: List<Event>, serviceMs: Long): Int =
        events.size - conflateLatest(events, serviceMs).size
}
