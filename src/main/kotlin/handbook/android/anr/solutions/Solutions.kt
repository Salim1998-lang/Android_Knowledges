package handbook.android.anr.solutions

import handbook.android.anr.AnrKind
import handbook.android.anr.FramePhase
import handbook.android.anr.MainOp
import handbook.android.anr.StrictPenalty
import handbook.android.anr.ThreadOp
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Эталонные решения темы 6. Подсмотри, если застрял с [handbook.android.anr.AnrTasks]. */
object AnrSolutions {

    private fun isStrictViolation(op: MainOp, onMain: Boolean): Boolean =
        onMain && (op == MainOp.DISK_READ || op == MainOp.DISK_WRITE || op == MainOp.NETWORK)

    // ── Лёгкие ──

    fun frameBudgetMillis(hz: Int): Double = 1000.0 / hz

    fun isJanky(workMs: Long, budgetMs: Long): Boolean = workMs > budgetMs

    fun droppedFrames(workMs: Long, budgetMs: Long): Int =
        if (workMs <= budgetMs) 0 else (ceil(workMs.toDouble() / budgetMs).toInt() - 1)

    fun jankyFrameCount(frameWorks: List<Long>, budgetMs: Long): Int =
        frameWorks.count { it > budgetMs }

    fun jankPercent(frameWorks: List<Long>, budgetMs: Long): Int {
        if (frameWorks.isEmpty()) return 0
        return (jankyFrameCount(frameWorks, budgetMs) * 100.0 / frameWorks.size).roundToInt()
    }

    fun anrTimeoutFor(kind: AnrKind): Long = kind.timeoutMs

    fun wouldAnr(handleLatencyMs: Long, kind: AnrKind): Boolean = handleLatencyMs >= kind.timeoutMs

    fun strictModeViolation(op: MainOp, onMainThread: Boolean): Boolean =
        isStrictViolation(op, onMainThread)

    // ── Средние ──

    fun framePresentTimes(frameWorks: List<Long>, budgetMs: Long): List<Long> {
        val present = ArrayList<Long>(frameWorks.size)
        var prev = 0L
        for (k in frameWorks.indices) {
            val vsync = k.toLong() * budgetMs
            val start = maxOf(vsync, prev)     // кадр не может начаться раньше своего vsync или конца предыдущего
            prev = start + frameWorks[k]
            present.add(prev)
        }
        return present
    }

    fun framesMissingDeadline(frameWorks: List<Long>, budgetMs: Long): Int {
        val present = framePresentTimes(frameWorks, budgetMs)
        var missed = 0
        for (k in present.indices) {
            val deadline = (k + 1).toLong() * budgetMs
            if (present[k] > deadline) missed++   // не успел к своему следующему vsync (с учётом каскада)
        }
        return missed
    }

    fun inputLatency(backlogMs: List<Long>, inputArrivalMs: Long): Long {
        val backlogEnd = backlogMs.sum()          // весь бэклог главного потока выполняется раньше ввода
        return maxOf(0L, backlogEnd - inputArrivalMs)
    }

    fun wouldAnrFromBacklog(backlogMs: List<Long>, inputArrivalMs: Long, kind: AnrKind): Boolean =
        wouldAnr(inputLatency(backlogMs, inputArrivalMs), kind)

    fun strictModeViolations(ops: List<ThreadOp>): List<String> =
        ops.filter { isStrictViolation(it.op, it.onMainThread) }.map { it.name }

    fun choreographerOrder(phases: List<FramePhase>): List<FramePhase> =
        phases.sortedBy { it.ordinal }

    fun frameFitsBudget(phaseWorks: List<Long>, budgetMs: Long): Boolean =
        phaseWorks.sum() <= budgetMs             // сумма ВСЕХ фаз кадра должна уложиться в бюджет

    // ── Сложные ──

    fun worstOverrunMs(frameWorks: List<Long>, budgetMs: Long): Long =
        frameWorks.maxOfOrNull { maxOf(0L, it - budgetMs) } ?: 0L

    fun percentile(frameWorks: List<Long>, p: Int): Long {
        if (frameWorks.isEmpty()) return 0
        val sorted = frameWorks.sorted()
        val rank = ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]                  // nearest-rank
    }

    fun wouldAnrSingleBlock(taskDurationsMs: List<Long>, kind: AnrKind): Boolean =
        (taskDurationsMs.maxOrNull() ?: 0L) >= kind.timeoutMs

    fun longestJankStreak(frameWorks: List<Long>, budgetMs: Long): Int {
        var best = 0
        var run = 0
        for (w in frameWorks) {
            if (w > budgetMs) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }

    fun strictModeCrashes(ops: List<ThreadOp>, penalty: StrictPenalty): Boolean =
        penalty == StrictPenalty.DEATH && ops.any { isStrictViolation(it.op, it.onMainThread) }
}
