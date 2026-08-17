package handbook.android.queues

import handbook.android.queues.solutions.QueuesSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

/**
 * Тесты темы 5 «Producer-consumer и backpressure». Механика `BlockingQueue` — на настоящих потоках
 * (детерминизм через poison-pill и `join`); стратегии backpressure — чистые модели над потоком
 * [Event] (throttle/debounce/sample/конфляция/drop/батчинг/credit).
 *
 * Чтобы гонять против эталона — замени `SUT` на [QueuesSolutions].
 */
private typealias SUT = QueuesTasks

class QueuesTest {

    private fun ev(vararg pairs: Pair<Long, Int>): List<Event> = pairs.map { Event(it.first, it.second) }

    // ── Лёгкие: BlockingQueue ──

    @Test fun `Л1 offerToBounded`() {
        assertEquals(3, SUT.offerToBounded(capacity = 3, n = 10))
        assertEquals(5, SUT.offerToBounded(capacity = 8, n = 5))
    }

    @Test fun `Л2 pollEmptyIsNull`() {
        assertTrue(SUT.pollEmptyIsNull())
    }

    @Test fun `Л3 drainAll`() {
        assertEquals(listOf(1, 2, 3), SUT.drainAll(listOf(1, 2, 3)))
    }

    @RepeatedTest(10) fun `Л4 producerConsumerPoison`() {
        assertEquals((1..20).toList(), SUT.producerConsumerPoison((1..20).toList()))
    }

    @RepeatedTest(10) fun `Л5 losslessBackpressure — ничего не потеряно`() {
        assertEquals((1..20).toList(), SUT.losslessBackpressure((1..20).toList()))
    }

    @Test fun `Л6 offerTimeoutFails`() {
        assertTrue(SUT.offerTimeoutFails())
    }

    @RepeatedTest(10) fun `Л7 synchronousHandoff`() {
        assertEquals(99, SUT.synchronousHandoff(99))
    }

    @RepeatedTest(10) fun `Л8 fanInMerge`() {
        assertEquals(6, SUT.fanInMerge(listOf(1, 2, 3), listOf(4, 5, 6)))
    }

    // ── Средние: стратегии ──

    @RepeatedTest(15) fun `С9 fanOutTotal — каждый элемент ровно раз`() {
        assertEquals(100, SUT.fanOutTotal((1..100).toList(), workers = 4))
    }

    @Test fun `С10 conflateLatest — latest-wins`() {
        assertEquals(listOf(1, 3, 4), SUT.conflateLatest(ev(0L to 1, 10L to 2, 20L to 3, 30L to 4), serviceMs = 25))
    }

    @Test fun `С11 throttleFirst`() {
        assertEquals(
            ev(0L to 1, 5L to 3, 10L to 4),
            SUT.throttleFirst(ev(0L to 1, 2L to 2, 5L to 3, 10L to 4, 11L to 5), intervalMs = 5),
        )
    }

    @Test fun `С12 debounce`() {
        assertEquals(
            ev(11L to 3, 25L to 4),
            SUT.debounce(ev(0L to 1, 3L to 2, 6L to 3, 20L to 4), quietMs = 5),
        )
    }

    @Test fun `С13 sampleLatest`() {
        assertEquals(
            ev(10L to 3, 20L to 4),
            SUT.sampleLatest(ev(1L to 1, 2L to 2, 3L to 3, 12L to 4), periodMs = 10),
        )
    }

    @Test fun `С14 dropOldest`() {
        assertEquals(listOf(3, 4, 5), SUT.dropOldest(listOf(1, 2, 3, 4, 5), capacity = 3))
    }

    @Test fun `С15 dropLatest`() {
        assertEquals(listOf(1, 2, 3), SUT.dropLatest(listOf(1, 2, 3, 4, 5), capacity = 3))
    }

    // ── Сложные ──

    @Test fun `СЛ16 batchByCount`() {
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), SUT.batchByCount(listOf(1, 2, 3, 4, 5), size = 2))
    }

    @Test fun `СЛ17 batchByTime`() {
        assertEquals(
            listOf(listOf(1, 2), listOf(3, 4), listOf(5)),
            SUT.batchByTime(ev(0L to 1, 3L to 2, 10L to 3, 12L to 4, 25L to 5), windowMs = 10),
        )
    }

    @Test fun `СЛ18 creditBasedEmits`() {
        assertEquals(listOf(2, 2, 4), SUT.creditBasedEmits(listOf(2, 0, 3), itemCount = 4))
    }

    @Test fun `СЛ19 throttleThenBatch`() {
        assertEquals(
            listOf(listOf(1, 3), listOf(4)),
            SUT.throttleThenBatch(ev(0L to 1, 2L to 2, 5L to 3, 10L to 4, 11L to 5), intervalMs = 5, batchSize = 2),
        )
    }

    @Test fun `СЛ20 conflationDropCount`() {
        // 4 события, доставлено 3 → отброшено 1
        assertEquals(1, SUT.conflationDropCount(ev(0L to 1, 10L to 2, 20L to 3, 30L to 4), serviceMs = 25))
    }
}
