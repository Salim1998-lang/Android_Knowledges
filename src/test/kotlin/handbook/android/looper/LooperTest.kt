package handbook.android.looper

import handbook.android.looper.solutions.LooperSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 1 «Главный поток и цикл событий». Проверяют механику `Looper`/`Handler`/`MessageQueue`
 * на детерминированной модели [Scheduled]: порядок по `when` (а не по `post`), sleep до next,
 * removeCallbacks, sync-барьер, idle-handler, confinement и head-of-line blocking (jank/ANR).
 *
 * Чтобы гонять против эталона — замени `SUT` на [LooperSolutions].
 */
private typealias SUT = LooperTasks

class LooperTest {

    // ── Лёгкие ──

    @Test fun `Л1 dispatchOrder — порядок по when, тай-брейк FIFO`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 30, label = "c"),
            Scheduled(seq = 1, whenMs = 10, label = "a"),
            Scheduled(seq = 2, whenMs = 10, label = "b"), // равное время → FIFO по seq
            Scheduled(seq = 3, whenMs = 20, label = "d"),
        )
        assertEquals(listOf("a", "b", "d", "c"), SUT.dispatchOrder(msgs))
    }

    @Test fun `Л2 dueTime — now плюс delay, отрицательный зажат в 0`() {
        assertEquals(150, SUT.dueTime(nowMs = 100, delayMs = 50))
        assertEquals(100, SUT.dueTime(nowMs = 100, delayMs = -20))
    }

    @Test fun `Л3 isDue`() {
        assertTrue(SUT.isDue(whenMs = 100, nowMs = 100))
        assertTrue(SUT.isDue(whenMs = 90, nowMs = 100))
        assertFalse(SUT.isDue(whenMs = 101, nowMs = 100))
    }

    @Test fun `Л4 nextDueLabel`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 50, label = "future"),
            Scheduled(seq = 1, whenMs = 10, label = "a"),
            Scheduled(seq = 2, whenMs = 10, label = "b"),
        )
        assertEquals("a", SUT.nextDueLabel(msgs, nowMs = 20))
        assertNull(SUT.nextDueLabel(msgs, nowMs = 5))
    }

    @Test fun `Л5 sleepMillis — due 0, пусто -1, иначе до ближайшего`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 40, label = "a"),
            Scheduled(seq = 1, whenMs = 25, label = "b"),
        )
        assertEquals(0, SUT.sleepMillis(msgs, nowMs = 30))          // b уже готово
        assertEquals(15, SUT.sleepMillis(msgs, nowMs = 10))         // до b (25) = 15
        assertEquals(-1, SUT.sleepMillis(emptyList(), nowMs = 10))  // очередь пуста
    }

    @Test fun `Л6 removeByToken`() {
        val t = Any()
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "a", token = t),
            Scheduled(seq = 1, whenMs = 0, label = "b"),
            Scheduled(seq = 2, whenMs = 0, label = "c", token = t),
        )
        assertEquals(listOf("b"), SUT.removeByToken(msgs, t))
    }

    @Test fun `Л7 hasToken`() {
        val t = "tok"
        val msgs = listOf(Scheduled(seq = 0, whenMs = 0, label = "a", token = t))
        assertTrue(SUT.hasToken(msgs, t))
        assertFalse(SUT.hasToken(msgs, "other"))
    }

    @Test fun `Л8 postAtFront — when 0 обгоняет всех`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "a"),
            Scheduled(seq = 1, whenMs = 5, label = "b"),
        )
        assertEquals(listOf("front", "a", "b"), SUT.postAtFront(msgs, "front"))
    }

    // ── Средние ──

    @Test fun `С9 runLoopTimed — часы продвигаются к when, назад не идут`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 10, label = "a"),
            Scheduled(seq = 1, whenMs = 10, label = "b"),
            Scheduled(seq = 2, whenMs = 30, label = "c"),
        )
        assertEquals(listOf("a" to 10L, "b" to 10L, "c" to 30L), SUT.runLoopTimed(msgs))
    }

    @Test fun `С10 heartbeat — периодический пульс`() {
        assertEquals(listOf(16L, 32L, 48L), SUT.heartbeat(intervalMs = 16, count = 3))
        assertEquals(listOf(105L, 110L), SUT.heartbeat(intervalMs = 5, count = 2, startMs = 100))
    }

    @Test fun `С11 removeDuringRun — отмена токена из цикла`() {
        val cancel = "job"
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "start"),
            Scheduled(seq = 1, whenMs = 10, label = "trigger"),
            Scheduled(seq = 2, whenMs = 20, label = "x", token = cancel),
            Scheduled(seq = 3, whenMs = 30, label = "y", token = cancel),
        )
        // trigger отменяет ещё не выполненные x, y
        assertEquals(listOf("start", "trigger"), SUT.removeDuringRun(msgs, "trigger", cancel))
    }

    @Test fun `С11 removeDuringRun — уже выполненное с токеном не откатывается`() {
        val cancel = "job"
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "early", token = cancel),
            Scheduled(seq = 1, whenMs = 10, label = "trigger"),
            Scheduled(seq = 2, whenMs = 20, label = "late", token = cancel),
        )
        assertEquals(listOf("early", "trigger"), SUT.removeDuringRun(msgs, "trigger", cancel))
    }

    @Test fun `С12 asyncPassesBarrier — сквозь барьер только async`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 5, label = "sync1"),
            Scheduled(seq = 1, whenMs = 10, label = "input", isAsync = true),
            Scheduled(seq = 2, whenMs = 15, label = "sync2"),
            Scheduled(seq = 3, whenMs = 2, label = "anim", isAsync = true),
        )
        assertEquals(listOf("anim", "input"), SUT.asyncPassesBarrier(msgs))
    }

    @Test fun `С13 quitSafely — доставляет готовые, роняет будущие`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "a"),
            Scheduled(seq = 1, whenMs = 50, label = "b"),
            Scheduled(seq = 2, whenMs = 100, label = "c"),
        )
        assertEquals(listOf("a"), SUT.quitSafely(msgs, nowMs = 10))
    }

    @Test fun `С14 runConfined — всё на одном потоке, не на текущем`() {
        val names = SUT.runConfined(5)
        assertEquals(5, names.size)
        assertEquals(1, names.toSet().size, "все задачи — на одном и том же потоке Looper'а")
        assertNotEquals(Thread.currentThread().name, names.first(), "не на потоке вызывающего")
    }

    @Test fun `С15 idleInvocations — true оставляет, false снимает`() {
        assertEquals(1, SUT.idleInvocations(returnsTrueTimes = 0)) // сразу false
        assertEquals(4, SUT.idleInvocations(returnsTrueTimes = 3)) // 3×true + 1×false
    }

    // ── Сложные ──

    @Test fun `СЛ16 runLoopWithDurations — долгое сообщение сдвигает старты`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "heavy", durationMs = 100),
            Scheduled(seq = 1, whenMs = 5, label = "a"),
            Scheduled(seq = 2, whenMs = 10, label = "b"),
        )
        // heavy держит поток 100мс → a и b стартуют только в 100
        assertEquals(listOf("heavy" to 0L, "a" to 100L, "b" to 100L), SUT.runLoopWithDurations(msgs))
    }

    @Test fun `СЛ16 runLoopWithDurations — простой до будущего сообщения`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "a", durationMs = 5),
            Scheduled(seq = 1, whenMs = 50, label = "b", durationMs = 5),
        )
        assertEquals(listOf("a" to 0L, "b" to 50L), SUT.runLoopWithDurations(msgs))
    }

    @Test fun `СЛ17 barrierWindow — sync в окне ждут barrierEnd, async течёт`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 5, label = "before"),               // до окна → 5
            Scheduled(seq = 1, whenMs = 20, label = "held"),                // в окне → 100
            Scheduled(seq = 2, whenMs = 25, label = "input", isAsync = true), // async → 25
            Scheduled(seq = 3, whenMs = 120, label = "after"),              // после окна → 120
        )
        assertEquals(
            listOf("before", "input", "held", "after"),
            SUT.barrierWindow(msgs, barrierStart = 10, barrierEnd = 100),
        )
    }

    @Test fun `СЛ18 repostTimes — fixed delay дрейфует, fixed rate догоняет`() {
        // interval=10, work=4, 4 запуска
        assertEquals(
            listOf(0L, 14L, 28L, 42L),
            SUT.repostTimes(intervalMs = 10, workMs = 4, count = 4, mode = RepostMode.FIXED_DELAY),
        )
        assertEquals(
            listOf(0L, 10L, 20L, 30L),
            SUT.repostTimes(intervalMs = 10, workMs = 4, count = 4, mode = RepostMode.FIXED_RATE),
        )
    }

    @Test fun `СЛ18 repostTimes — fixed rate догоняет, когда работа длиннее периода`() {
        // work=15 > interval=10 → планировщик не успевает, старты подряд по финишу
        assertEquals(
            listOf(0L, 15L, 30L),
            SUT.repostTimes(intervalMs = 10, workMs = 15, count = 3, mode = RepostMode.FIXED_RATE),
        )
    }

    @Test fun `СЛ19 idleBatches — дробление бэклога по порциям`() {
        assertEquals(listOf(3, 3, 1), SUT.idleBatches(backlog = 7, perIdle = 3))
        assertEquals(emptyList<Int>(), SUT.idleBatches(backlog = 0, perIdle = 3))
    }

    @Test fun `СЛ20 worstLatency — худшее ожидание из-за занятого потока`() {
        val msgs = listOf(
            Scheduled(seq = 0, whenMs = 0, label = "heavy", durationMs = 100),
            Scheduled(seq = 1, whenMs = 5, label = "a"),   // ждал 95
            Scheduled(seq = 2, whenMs = 10, label = "b"),  // ждал 90
        )
        assertEquals(95, SUT.worstLatency(msgs))
        assertEquals(0, SUT.worstLatency(emptyList()))
    }
}
