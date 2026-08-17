package handbook.android.looper.solutions

import handbook.android.looper.RepostMode
import handbook.android.looper.Scheduled
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/** Эталонные решения темы 1. Подсмотри, если застрял с [handbook.android.looper.LooperTasks]. */
object LooperSolutions {

    // ── Лёгкие ──

    fun dispatchOrder(msgs: List<Scheduled>): List<String> =
        msgs.sortedWith(compareBy({ it.whenMs }, { it.seq })).map { it.label }

    fun dueTime(nowMs: Long, delayMs: Long): Long =
        nowMs + delayMs.coerceAtLeast(0)

    fun isDue(whenMs: Long, nowMs: Long): Boolean =
        whenMs <= nowMs

    fun nextDueLabel(msgs: List<Scheduled>, nowMs: Long): String? =
        msgs.filter { it.whenMs <= nowMs }
            .minWithOrNull(compareBy({ it.whenMs }, { it.seq }))
            ?.label

    fun sleepMillis(msgs: List<Scheduled>, nowMs: Long): Long {
        if (msgs.isEmpty()) return -1
        if (msgs.any { it.whenMs <= nowMs }) return 0
        return msgs.minOf { it.whenMs } - nowMs
    }

    fun removeByToken(msgs: List<Scheduled>, token: Any?): List<String> =
        msgs.filter { it.token != token }.map { it.label }

    fun hasToken(msgs: List<Scheduled>, token: Any?): Boolean =
        msgs.any { it.token == token }

    fun postAtFront(msgs: List<Scheduled>, newLabel: String): List<String> =
        dispatchOrder(msgs + Scheduled(seq = -1, whenMs = 0, label = newLabel))

    // ── Средние ──

    fun runLoopTimed(msgs: List<Scheduled>): List<Pair<String, Long>> {
        var clock = 0L
        return msgs.sortedWith(compareBy({ it.whenMs }, { it.seq })).map {
            clock = maxOf(clock, it.whenMs)
            it.label to clock
        }
    }

    fun heartbeat(intervalMs: Long, count: Int, startMs: Long = 0): List<Long> =
        (1..count).map { startMs + intervalMs * it }

    fun removeDuringRun(msgs: List<Scheduled>, triggerLabel: String, cancelToken: Any?): List<String> {
        val queue = msgs.sortedWith(compareBy({ it.whenMs }, { it.seq })).toMutableList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < queue.size) {
            val m = queue[i]
            out += m.label
            if (m.label == triggerLabel) {
                // Удаляем ещё не выполненные (индексы > i) с этим токеном.
                val kept = queue.subList(0, i + 1).toList() +
                    queue.subList(i + 1, queue.size).filter { it.token != cancelToken }
                queue.clear()
                queue.addAll(kept)
            }
            i++
        }
        return out
    }

    fun asyncPassesBarrier(msgs: List<Scheduled>): List<String> =
        dispatchOrder(msgs.filter { it.isAsync })

    fun quitSafely(msgs: List<Scheduled>, nowMs: Long): List<String> =
        dispatchOrder(msgs.filter { it.whenMs <= nowMs })

    fun runConfined(count: Int): List<String> {
        val ex = Executors.newSingleThreadExecutor()
        return try {
            (1..count).map { ex.submit(Callable { Thread.currentThread().name }).get() }
        } finally {
            ex.shutdown()
        }
    }

    fun idleInvocations(returnsTrueTimes: Int): Int =
        returnsTrueTimes + 1

    // ── Сложные ──

    fun runLoopWithDurations(msgs: List<Scheduled>): List<Pair<String, Long>> {
        var clock = 0L
        return msgs.sortedWith(compareBy({ it.whenMs }, { it.seq })).map { m ->
            val start = maxOf(clock, m.whenMs)
            clock = start + m.durationMs
            m.label to start
        }
    }

    fun barrierWindow(msgs: List<Scheduled>, barrierStart: Long, barrierEnd: Long): List<String> {
        fun eff(m: Scheduled): Long = when {
            m.isAsync -> m.whenMs
            m.whenMs in barrierStart until barrierEnd -> barrierEnd
            else -> m.whenMs
        }
        return msgs.sortedWith(compareBy({ eff(it) }, { it.seq })).map { it.label }
    }

    fun repostTimes(intervalMs: Long, workMs: Long, count: Int, mode: RepostMode): List<Long> = when (mode) {
        RepostMode.FIXED_DELAY -> {
            var t = 0L
            (0 until count).map {
                val start = t
                t = start + workMs + intervalMs
                start
            }
        }
        RepostMode.FIXED_RATE -> {
            var finish = 0L
            (0 until count).map { i ->
                val scheduled = i * intervalMs
                val start = maxOf(scheduled, finish)
                finish = start + workMs
                start
            }
        }
    }

    fun idleBatches(backlog: Int, perIdle: Int): List<Int> {
        val out = mutableListOf<Int>()
        var left = backlog
        while (left > 0) {
            val take = minOf(perIdle, left)
            out += take
            left -= take
        }
        return out
    }

    fun worstLatency(msgs: List<Scheduled>): Long {
        if (msgs.isEmpty()) return 0
        var clock = 0L
        var worst = 0L
        for (m in msgs.sortedWith(compareBy({ it.whenMs }, { it.seq }))) {
            val start = maxOf(clock, m.whenMs)
            worst = maxOf(worst, start - m.whenMs)
            clock = start + m.durationMs
        }
        return worst
    }
}
