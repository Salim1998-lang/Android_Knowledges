package handbook.android.handlerthread

import handbook.android.handlerthread.solutions.HandlerThreadSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

/**
 * Тесты темы 2 «Фоновые Handler-потоки». Работают с НАСТОЯЩИМИ потоками ([LooperThread]);
 * детерминизм обеспечивают `quitSafely()`+`join()` (барьер завершения) и `CountDownLatch` (гейт,
 * гарантирующий состояние очереди). Проверяют FIFO, confinement, `quit`/`quitSafely`, обмен между
 * потоками, конвейеры и координацию.
 *
 * Чтобы гонять против эталона — замени `SUT` на [HandlerThreadSolutions].
 */
private typealias SUT = HandlerThreadTasks

class HandlerThreadTest {

    // ── Лёгкие ──

    @Test fun `Л1 runAndCollect — FIFO`() {
        assertEquals(listOf("a", "b", "c", "d"), SUT.runAndCollect(listOf("a", "b", "c", "d")))
    }

    @Test fun `Л2 sameThreadForAll — confinement`() {
        assertTrue(SUT.sameThreadForAll(10))
    }

    @Test fun `Л3 runsOffCallerThread`() {
        assertTrue(SUT.runsOffCallerThread())
    }

    @Test fun `Л4 postBeforeStartRejected`() {
        assertTrue(SUT.postBeforeStartRejected())
    }

    @Test fun `Л5 postAfterQuitRejected`() {
        assertTrue(SUT.postAfterQuitRejected())
    }

    @Test fun `Л6 confinedCounter — без локов`() {
        assertEquals(1000, SUT.confinedCounter(1000))
    }

    @Test fun `Л7 observedThreadName`() {
        assertEquals("io-worker", SUT.observedThreadName("io-worker"))
    }

    @Test fun `Л8 joinIsCompletionBarrier`() {
        assertTrue(SUT.joinIsCompletionBarrier())
    }

    // ── Средние ──

    @RepeatedTest(20) fun `С9 quitSafelyDrainsBacklog — доработать всё`() {
        assertEquals(listOf("x", "y", "z"), SUT.quitSafelyDrainsBacklog(listOf("x", "y", "z")))
    }

    @RepeatedTest(20) fun `С10 quitDropsBacklog — бросить ожидающее`() {
        assertEquals(listOf("gate"), SUT.quitDropsBacklog(listOf("x", "y", "z")))
    }

    @RepeatedTest(10) fun `С11 roundTripToMain`() {
        assertEquals(70, SUT.roundTripToMain(7))
    }

    @Test fun `С12 sharedLooperFifo — общий порядок`() {
        assertEquals(
            listOf("a1", "b1", "a2", "b2", "a3"),
            SUT.sharedLooperFifo(listOf("a1", "a2", "a3"), listOf("b1", "b2")),
        )
    }

    @RepeatedTest(10) fun `С13 handoffCount — кросс-поточный post`() {
        assertEquals(500, SUT.handoffCount(500))
    }

    @RepeatedTest(20) fun `С14 backpressureOrder`() {
        assertEquals(listOf("1", "2", "3", "4", "5"), SUT.backpressureOrder(listOf("1", "2", "3", "4", "5")))
    }

    @Test fun `С15 shardAcrossTwo`() {
        // 1..10: чётных 5 (2,4,6,8,10), нечётных 5
        assertEquals(5 to 5, SUT.shardAcrossTwo((1..10).toList()))
    }

    // ── Сложные ──

    @RepeatedTest(10) fun `СЛ16 twoStagePipeline — (x+1)*2`() {
        assertEquals(listOf(4, 6, 8), SUT.twoStagePipeline(listOf(1, 2, 3)))
    }

    @RepeatedTest(10) fun `СЛ17 pingPong`() {
        assertEquals(10, SUT.pingPong(10))
        assertEquals(0, SUT.pingPong(0))
    }

    @RepeatedTest(10) fun `СЛ18 requestResponse — корреляция по id`() {
        assertEquals(listOf(2 to 4, 3 to 9, 4 to 16), SUT.requestResponse(listOf(2, 3, 4)))
    }

    @Test fun `СЛ19 cancelWorkPastLifecycle`() {
        assertEquals(listOf("a", "b"), SUT.cancelWorkPastLifecycle(listOf("a", "b", "c", "d", "e"), 2))
    }

    @Test fun `СЛ20 reusesSingleThread — один поток на всё`() {
        assertEquals(1, SUT.reusesSingleThread(batches = 5, perBatch = 20))
    }
}
