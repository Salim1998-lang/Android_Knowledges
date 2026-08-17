package handbook.android.anr

import handbook.android.anr.solutions.AnrSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 6 «ANR, кадры и jank». Детерминированные модели: бюджет кадра и refresh rate, каскад
 * презентаций кадров, метрики jank (janky %, перцентили, worst hitch, streak), пороги ANR и задержка
 * ввода от бэклога главного потока, `Choreographer`-фазы, `StrictMode`-детектор.
 *
 * Чтобы гонять против эталона — замени `SUT` на [AnrSolutions].
 */
private typealias SUT = AnrTasks

class AnrTest {

    // ── Лёгкие ──

    @Test fun `Л1 frameBudgetMillis`() {
        assertEquals(1000.0 / 60, SUT.frameBudgetMillis(60), 1e-9)
        assertEquals(1000.0 / 120, SUT.frameBudgetMillis(120), 1e-9)
    }

    @Test fun `Л2 isJanky`() {
        assertTrue(SUT.isJanky(20, 16))
        assertFalse(SUT.isJanky(16, 16))
    }

    @Test fun `Л3 droppedFrames`() {
        assertEquals(0, SUT.droppedFrames(10, 16))
        assertEquals(3, SUT.droppedFrames(50, 16))
    }

    @Test fun `Л4 jankyFrameCount`() {
        assertEquals(2, SUT.jankyFrameCount(listOf(5, 20, 20, 10), 16))
    }

    @Test fun `Л5 jankPercent`() {
        assertEquals(50, SUT.jankPercent(listOf(5, 20, 20, 5), 16))
    }

    @Test fun `Л6 anrTimeoutFor`() {
        assertEquals(5_000, SUT.anrTimeoutFor(AnrKind.INPUT))
        assertEquals(10_000, SUT.anrTimeoutFor(AnrKind.BROADCAST_FG))
    }

    @Test fun `Л7 wouldAnr`() {
        assertTrue(SUT.wouldAnr(5_000, AnrKind.INPUT))
        assertFalse(SUT.wouldAnr(4_999, AnrKind.INPUT))
    }

    @Test fun `Л8 strictModeViolation`() {
        assertTrue(SUT.strictModeViolation(MainOp.DISK_READ, onMainThread = true))
        assertFalse(SUT.strictModeViolation(MainOp.DISK_READ, onMainThread = false))
        assertFalse(SUT.strictModeViolation(MainOp.CPU, onMainThread = true))
    }

    // ── Средние ──

    @Test fun `С9 framePresentTimes — каскад`() {
        assertEquals(listOf(10L, 36L, 41L), SUT.framePresentTimes(listOf(10, 20, 5), 16))
    }

    @Test fun `С10 framesMissingDeadline`() {
        assertEquals(1, SUT.framesMissingDeadline(listOf(10, 20, 5), 16))
    }

    @Test fun `С11 inputLatency`() {
        assertEquals(5_200, SUT.inputLatency(listOf(100, 200, 5_000), inputArrivalMs = 100))
        assertEquals(0, SUT.inputLatency(listOf(50), inputArrivalMs = 100))
    }

    @Test fun `С12 wouldAnrFromBacklog`() {
        assertTrue(SUT.wouldAnrFromBacklog(listOf(100, 200, 5_000), inputArrivalMs = 100, kind = AnrKind.INPUT))
        assertFalse(SUT.wouldAnrFromBacklog(listOf(100, 200), inputArrivalMs = 100, kind = AnrKind.INPUT))
    }

    @Test fun `С13 strictModeViolations`() {
        val ops = listOf(
            ThreadOp("loadPrefs", MainOp.DISK_READ, onMainThread = true),
            ThreadOp("calc", MainOp.CPU, onMainThread = true),
            ThreadOp("bgFetch", MainOp.NETWORK, onMainThread = false),
            ThreadOp("saveDb", MainOp.DISK_WRITE, onMainThread = true),
        )
        assertEquals(listOf("loadPrefs", "saveDb"), SUT.strictModeViolations(ops))
    }

    @Test fun `С14 choreographerOrder`() {
        val shuffled = listOf(FramePhase.COMMIT, FramePhase.INPUT, FramePhase.TRAVERSAL, FramePhase.ANIMATION)
        assertEquals(
            listOf(FramePhase.INPUT, FramePhase.ANIMATION, FramePhase.TRAVERSAL, FramePhase.COMMIT),
            SUT.choreographerOrder(shuffled),
        )
    }

    @Test fun `С15 frameFitsBudget`() {
        assertTrue(SUT.frameFitsBudget(listOf(2, 3, 8), 16))   // сумма 13 ≤ 16
        assertFalse(SUT.frameFitsBudget(listOf(2, 3, 20), 16)) // сумма 25 > 16
    }

    // ── Сложные ──

    @Test fun `СЛ16 worstOverrunMs`() {
        assertEquals(34, SUT.worstOverrunMs(listOf(5, 20, 50), 16))
        assertEquals(0, SUT.worstOverrunMs(listOf(5, 10, 16), 16))
    }

    @Test fun `СЛ17 percentile`() {
        val frames = listOf(10L, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        assertEquals(90, SUT.percentile(frames, 90))
        assertEquals(100, SUT.percentile(frames, 100))
        assertEquals(10, SUT.percentile(frames, 1))
    }

    @Test fun `СЛ18 wouldAnrSingleBlock`() {
        assertTrue(SUT.wouldAnrSingleBlock(listOf(100, 200, 6_000), AnrKind.INPUT))
        assertFalse(SUT.wouldAnrSingleBlock(listOf(100, 200, 300), AnrKind.INPUT))
    }

    @Test fun `СЛ19 longestJankStreak`() {
        assertEquals(2, SUT.longestJankStreak(listOf(5, 20, 20, 5, 20), 16))
        assertEquals(0, SUT.longestJankStreak(listOf(5, 10, 16), 16))
    }

    @Test fun `СЛ20 strictModeCrashes`() {
        val ops = listOf(ThreadOp("loadPrefs", MainOp.DISK_READ, onMainThread = true))
        assertTrue(SUT.strictModeCrashes(ops, StrictPenalty.DEATH))
        assertFalse(SUT.strictModeCrashes(ops, StrictPenalty.LOG))
        assertFalse(SUT.strictModeCrashes(listOf(ThreadOp("calc", MainOp.CPU, true)), StrictPenalty.DEATH))
    }
}
