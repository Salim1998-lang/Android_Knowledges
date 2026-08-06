package handbook.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Тесты темы 9. Задачи запускают реально параллельные корутины (Dispatchers.Default) над общим
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
        assertEquals(777, ConcurrencyTasks.mutexIncrements(777)) // не круглое — не хардкод
        assertEquals(0, ConcurrencyTasks.mutexIncrements(0))
    }

    @Test fun `Л3 atomicIncrements`() = runTest {
        assertEquals(1000, ConcurrencyTasks.atomicIncrements(1000))
        assertEquals(333, ConcurrencyTasks.atomicIncrements(333)) // другой вход
    }

    @Test fun `Л4 safeAppendAll не теряет элементы`() = runTest {
        val values = (1..500).toList()
        val result = ConcurrencyTasks.safeAppendAll(values)
        assertEquals(500, result.size)
        assertEquals(values.toSet(), result.toSet())
    }

    @Test fun `Л5 sumConcurrently`() = runTest {
        assertEquals((1..1000).sum(), ConcurrencyTasks.sumConcurrently((1..1000).toList()))
        assertEquals(0, ConcurrencyTasks.sumConcurrently(emptyList()))               // пусто → 0
        assertEquals(0, ConcurrencyTasks.sumConcurrently(listOf(5, -3, -2)))         // отрицательные
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
        assertEquals(2500, ConcurrencyTasks.concurrentReadModifyWrite(IntBox(500), 2000)) // старт != 0
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
        assertEquals(-5, ConcurrencyTasks.atomicMax(listOf(-10, -5, -99, -7))) // все отрицательные
        assertEquals(42, ConcurrencyTasks.atomicMax(listOf(42)))               // единственный
    }

    @Test fun `С14 perKeyCounts`() = runTest {
        val keys = listOf("x", "y", "x", "z", "x", "y")
        assertEquals(mapOf("x" to 3, "y" to 2, "z" to 1), ConcurrencyTasks.perKeyCounts(keys))
    }

    @Test fun `С15 applyTransfers сохраняет сумму`() = runTest {
        // круговые переводы возвращают к исходным балансам (проверяем инвариант суммы)
        val balances = intArrayOf(100, 100, 100)
        val transfers = (0 until 300).map { Triple(it % 3, (it + 1) % 3, 1) }
        val result = ConcurrencyTasks.applyTransfers(balances, transfers)
        assertEquals(300, result.sum(), "сумма должна сохраняться")
        assertEquals(listOf(100, 100, 100), result.toList())

        // АСИММЕТРИЧНЫЙ сценарий: переводы реально двигают деньги (no-op так не пройдёт)
        val b2 = intArrayOf(100, 100, 100)
        val oneWay = (0 until 30).map { Triple(0, 1, 1) } + (0 until 10).map { Triple(2, 0, 1) }
        val r2 = ConcurrencyTasks.applyTransfers(b2, oneWay)
        assertEquals(300, r2.sum(), "сумма сохраняется")
        assertEquals(listOf(80, 130, 90), r2.toList(), "0: -30+10, 1: +30, 2: -10")
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
        assertEquals((1..100).sum(), ConcurrencyTasks.parallelSumLocalAgg((1..100).toList(), workers = 1)) // один воркер
        assertEquals(0, ConcurrencyTasks.parallelSumLocalAgg(emptyList(), workers = 4))                    // пусто → 0
    }

    // ── Корутинные свойства: защита ДЕРЖИТСЯ под реальным параллелизмом ──

    @Test fun `КС1 mutexIncrements точен при ПОВТОРНОМ высоком contention`() = runTest {
        // Гонка недетерминирована: без защиты какой-то из прогонов почти наверняка потерял бы приращения.
        // Верная реализация даёт РОВНО N в каждом из многих прогонов.
        repeat(5) { assertEquals(5000, ConcurrencyTasks.mutexIncrements(5000)) }
    }

    @Test fun `КС2 read-modify-write под замком не теряет приращений на повторах`() = runTest {
        repeat(5) { assertEquals(3000, ConcurrencyTasks.concurrentReadModifyWrite(IntBox(), 3000)) }
    }

    @Test fun `КС3 atomicIncrements точен на повторах`() = runTest {
        repeat(5) { assertEquals(4000, ConcurrencyTasks.atomicIncrements(4000)) }
    }
}
