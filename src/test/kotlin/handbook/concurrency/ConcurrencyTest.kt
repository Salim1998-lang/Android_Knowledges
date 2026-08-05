package handbook.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Тесты темы 7. Задачи запускают реально параллельные корутины (Dispatchers.Default) над общим
 * состоянием — там, где живут гонки. Правильная защита (Mutex / атомик / замыкание / actor) даёт
 * ВЕРНЫЙ, детерминированный результат при любом планировании: именно это и проверяется.
 */
class ConcurrencyTest {

    // ── Лёгкие ──

    @Test fun `Л1 guard возвращает значение под замком`() = runTest {
        assertEquals(42, ConcurrencyTasks.guard(Mutex()) { 42 })
    }

    @Test fun `Л2 mutexIncrements без потерянных приращений`() = runTest {
        assertEquals(1000, ConcurrencyTasks.mutexIncrements(1000))
    }

    @Test fun `Л3 atomicIncrements`() = runTest {
        assertEquals(1000, ConcurrencyTasks.atomicIncrements(1000))
    }

    @Test fun `Л4 safeAppendAll не теряет элементы`() = runTest {
        val values = (1..500).toList()
        val result = ConcurrencyTasks.safeAppendAll(values)
        assertEquals(500, result.size)
        assertEquals(values.toSet(), result.toSet())
    }

    @Test fun `Л5 sumConcurrently`() = runTest {
        assertEquals((1..1000).sum(), ConcurrencyTasks.sumConcurrently((1..1000).toList()))
    }

    @Test fun `Л6 atomicAddAndGet`() {
        assertEquals(15, ConcurrencyTasks.atomicAddAndGet(AtomicInteger(10), 5))
    }

    @Test fun `Л7 setIfZero`() {
        assertTrue(ConcurrencyTasks.setIfZero(AtomicInteger(0), 7))
        assertFalse(ConcurrencyTasks.setIfZero(AtomicInteger(3), 7))
    }

    @Test fun `Л8 mutexDecrements`() = runTest {
        assertEquals(0, ConcurrencyTasks.mutexDecrements(start = 1000, times = 1000))
    }

    // ── Средние ──

    @Test fun `С9 concurrentReadModifyWrite`() = runTest {
        assertEquals(1000, ConcurrencyTasks.concurrentReadModifyWrite(IntBox(), 1000))
    }

    @Test fun `С10 confinedCounterFine`() = runTest {
        assertEquals(1000, ConcurrencyTasks.confinedCounterFine(1000))
    }

    @Test fun `С11 confinedCounterCoarse`() = runTest {
        assertEquals(1000, ConcurrencyTasks.confinedCounterCoarse(1000))
    }

    @Test fun `С12 wordFrequencies`() = runTest {
        val words = listOf("a", "b", "a", "c", "b", "a")
        val freq = ConcurrencyTasks.wordFrequencies(words)
        assertEquals(mapOf("a" to 3, "b" to 2, "c" to 1), freq)
        assertEquals(words.size, freq.values.sum())
    }

    @Test fun `С13 atomicMax`() = runTest {
        assertEquals(999, ConcurrencyTasks.atomicMax((0..999).shuffled()))
    }

    @Test fun `С14 perKeyCounts`() = runTest {
        val keys = listOf("x", "y", "x", "z", "x", "y")
        assertEquals(mapOf("x" to 3, "y" to 2, "z" to 1), ConcurrencyTasks.perKeyCounts(keys))
    }

    @Test fun `С15 applyTransfers сохраняет сумму`() = runTest {
        val balances = intArrayOf(100, 100, 100)
        val total = balances.sum()
        // 300 переводов по кругу 0→1→2→0
        val transfers = (0 until 300).map { Triple(it % 3, (it + 1) % 3, 1) }
        val result = ConcurrencyTasks.applyTransfers(balances, transfers)
        assertEquals(total, result.sum(), "сумма должна сохраняться")
        assertEquals(listOf(100, 100, 100), result.toList())
    }

    // ── Сложные ──

    @Test fun `СЛ16 counterActorFinalValue`() = runTest {
        assertEquals(1000, ConcurrencyTasks.counterActorFinalValue(1000))
    }

    @Test fun `СЛ17 stripedIncrement`() = runTest {
        val result = ConcurrencyTasks.stripedIncrement(listOf("a", "b", "c"), perKey = 500)
        assertEquals(mapOf("a" to 500, "b" to 500, "c" to 500), result)
    }

    @Test fun `СЛ18 runInitOnce инициализирует ровно раз`() = runTest {
        assertEquals(1, ConcurrencyTasks.runInitOnce(callers = 1000) { 42 })
    }

    @Test fun `СЛ19 treiberStackRoundTrip не теряет и не дублирует`() = runTest {
        val values = (1..1000).toList()
        val popped = ConcurrencyTasks.treiberStackRoundTrip(values)
        assertEquals(values.size, popped.size)
        assertEquals(values.toSet(), popped.toSet())
    }

    @Test fun `СЛ20 parallelSumLocalAgg`() = runTest {
        assertEquals((1..1000).sum(), ConcurrencyTasks.parallelSumLocalAgg((1..1000).toList(), workers = 8))
    }
}
