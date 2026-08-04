package handbook.cancellation

import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 3. Помимо результата проверяют ВИРТУАЛЬНОЕ ВРЕМЯ: успешный блок завершается на своей
 * внутренней задержке, а таймаут срабатывает РОВНО на дедлайне (ни раньше, ни позже). Это отсекает
 * решения, которые «ждут до конца» вместо отмены по таймауту.
 */
class CancellationTest {

    // ── Лёгкие ──

    @Test fun `Л1 orNull`() = runTest {
        val t0 = currentTime
        assertEquals("ok", CancellationTasks.orNull(1000) { delay(100); "ok" })
        assertEquals(100, currentTime - t0, "успех на 100")
        val t1 = currentTime
        assertNull(CancellationTasks.orNull(100) { delay(1000); "late" })
        assertEquals(100, currentTime - t1, "отмена ровно на таймауте 100, а не на 1000")
    }

    @Test fun `Л2 strict бросает при таймауте`() {
        assertThrows(TimeoutCancellationException::class.java) {
            runTest { CancellationTasks.strict(100) { delay(1000); "late" } }
        }
    }

    @Test fun `Л3 withFallback`() = runTest {
        val t0 = currentTime
        assertEquals("late-fb", CancellationTasks.withFallback(100, "late-fb") { delay(1000); "x" })
        assertEquals(100, currentTime - t0, "fallback после таймаута на 100")
        val t1 = currentTime
        assertEquals("ok", CancellationTasks.withFallback(1000, "fb") { delay(10); "ok" })
        assertEquals(10, currentTime - t1, "успел за 10")
    }

    @Test fun `Л4 isTimedOut`() = runTest {
        val t0 = currentTime
        assertTrue(CancellationTasks.isTimedOut(100) { delay(1000) })
        assertEquals(100, currentTime - t0, "таймаут на 100")
        val t1 = currentTime
        assertFalse(CancellationTasks.isTimedOut(1000) { delay(10) })
        assertEquals(10, currentTime - t1)
    }

    @Test fun `Л5 delayThenValue`() = runTest {
        val t0 = currentTime
        assertEquals(7, CancellationTasks.delayThenValue(50, 7))
        assertEquals(50, currentTime - t0)
    }

    @Test fun `Л6 sumCooperatively`() = runTest {
        val t0 = currentTime
        assertEquals(6, CancellationTasks.sumCooperatively(listOf(1, 2, 3)))
        assertEquals(3, currentTime - t0, "3 × delay(1)")
    }

    @Test fun `Л7 yieldingList`() = runTest {
        val t0 = currentTime
        assertEquals(listOf(0, 1, 2, 3), CancellationTasks.yieldingList(4))
        assertEquals(0, currentTime - t0, "yield() уступает, но время не двигает")
    }

    @Test fun `Л8 runUntilCancelled`() = runTest {
        var ticks = 0
        val job = launch { CancellationTasks.runUntilCancelled { ticks++ } }
        advanceTimeBy(55)
        job.cancelAndJoin()
        assertTrue(ticks in 5..7, "ожидалось ~6, было $ticks")
        assertTrue(job.isCancelled)
    }

    // ── Средние ──

    @Test fun `С9 countCompletedBeforeTimeout`() = runTest {
        val t0 = currentTime
        val done = CancellationTasks.countCompletedBeforeTimeout(35, (1..5).toList(), 10) {}
        assertEquals(3, done)
        assertEquals(35, currentTime - t0, "отмена на дедлайне 35 (успели 3 шага по 10)")
    }

    @Test fun `С10 mapUntilTimeout`() = runTest {
        val t0 = currentTime
        val r = CancellationTasks.mapUntilTimeout(35, listOf("a", "b", "c", "d"), 10) { it.uppercase() }
        assertEquals(listOf("A", "B", "C"), r)
        assertEquals(35, currentTime - t0, "отмена на дедлайне 35")
    }

    @Test fun `С11 loadAllWithDeadline`() = runTest {
        val t0 = currentTime
        val ok = CancellationTasks.loadAllWithDeadline(1000, listOf(1, 2, 3)) { delay(10); it * 10 }
        assertEquals(listOf(10, 20, 30), ok)
        assertEquals(10, currentTime - t0, "загрузки параллельны → 10, а не 30")
        val t1 = currentTime
        val late = CancellationTasks.loadAllWithDeadline(50, listOf(1, 2)) { delay(1000); it }
        assertNull(late)
        assertEquals(50, currentTime - t1, "отмена на дедлайне 50")
    }

    @Test fun `С12 cooperativeIndexOf`() = runTest {
        assertEquals(2, CancellationTasks.cooperativeIndexOf(listOf(1, 3, 4, 5)) { it % 2 == 0 })
        assertNull(CancellationTasks.cooperativeIndexOf(listOf(1, 3, 5)) { it % 2 == 0 })
    }

    @Test fun `С12 отменяется на каждом шаге, а не только при совпадении`() = runTest {
        // Предикат НЕ суспендится и НИКОГДА не совпадает, но на 2-м элементе отменяет сам поиск.
        // Корректная реализация зовёт ensureActive() перед КАЖДЫМ элементом → на следующем шаге
        // бросит CancellationException и прекратит скан: предикат вызван только для [10, 20].
        // Реализация с ensureActive() лишь в ветке совпадения досканирует весь список.
        val calls = mutableListOf<Int>()
        lateinit var job: Job
        job = launch {
            CancellationTasks.cooperativeIndexOf(listOf(10, 20, 30, 40, 50)) { item ->
                calls.add(item)
                if (item == 20) job.cancel()
                false
            }
        }
        job.join()
        assertEquals(listOf(10, 20), calls, "после отмены скан обязан прекратиться на следующем шаге")
    }

    @Test fun `С13 runChildThenCancel`() = runTest {
        val t0 = currentTime
        assertEquals(6, CancellationTasks.runChildThenCancel(runMs = 55, tickEvery = 10))
        assertEquals(55, currentTime - t0, "ребёнок работал ровно 55 мс до отмены")
    }

    @Test fun `С14 sumWithDeadline`() = runTest {
        // stepMs=10, timeout=35 → успевают 3 элемента: 10+20+30
        val t0 = currentTime
        assertEquals(6, CancellationTasks.sumWithDeadline(35, listOf(1, 2, 3, 4, 5), 10))
        assertEquals(35, currentTime - t0, "отмена на дедлайне 35")
    }

    @Test fun `С15 firstOrSecond`() = runTest {
        val t0 = currentTime
        assertEquals("fast", CancellationTasks.firstOrSecond(1000, { delay(10); "fast" }, { "second" }))
        assertEquals(10, currentTime - t0)
        val t1 = currentTime
        assertEquals("second", CancellationTasks.firstOrSecond(50, { delay(1000); "slow" }, { "second" }))
        assertEquals(50, currentTime - t1, "первый отвалился по таймауту 50, затем second()")
    }

    // ── Сложные ──

    @Test fun `СЛ16 runWithGuaranteedCleanup при отмене`() = runTest {
        var cleaned = false
        val t0 = currentTime
        val ok = CancellationTasks.runWithGuaranteedCleanup({ delay(10); "ok" }, { cleaned = true })
        assertEquals("ok", ok)
        assertTrue(cleaned)
        assertEquals(10, currentTime - t0)

        var cleaned2 = false
        val job = launch {
            CancellationTasks.runWithGuaranteedCleanup({ delay(1000); "never" }, { cleaned2 = true })
        }
        advanceTimeBy(10)
        job.cancelAndJoin()
        assertTrue(cleaned2, "cleanup должен выполниться даже при отмене")
    }

    @Test fun `СЛ17 retryWithTimeout`() = runTest {
        var attempts = 0
        val t0 = currentTime
        val r = CancellationTasks.retryWithTimeout(times = 3, perAttemptMs = 50, delayMs = 5) {
            attempts++
            if (attempts < 3) delay(1000) // таймаут первых двух попыток
            "ok"
        }
        assertEquals("ok", r)
        assertEquals(3, attempts)
        // попытка1 таймаут 50 + delay 5 + попытка2 таймаут 50 + delay 5 + попытка3 успех = 110
        assertEquals(110, currentTime - t0, "2 таймаута по 50 + 2 паузы по 5")
    }

    @Test fun `СЛ18 raceFirst`() = runTest {
        val t0 = currentTime
        assertEquals("fast", CancellationTasks.raceFirst({ delay(10); "fast" }, { delay(1000); "slow" }))
        assertEquals(10, currentTime - t0, "победитель на 10, проигравший отменён — не ждём 1000")
    }

    @Test fun `СЛ19 withTimeoutCleanup`() = runTest {
        var cancelled = false
        val t0 = currentTime
        val ok = CancellationTasks.withTimeoutCleanup(1000, { delay(10); "ok" }, { cancelled = true })
        assertEquals("ok", ok)
        assertFalse(cancelled)
        assertEquals(10, currentTime - t0)

        var cancelled2 = false
        val t1 = currentTime
        val late = CancellationTasks.withTimeoutCleanup(50, { delay(1000); "x" }, { cancelled2 = true })
        assertNull(late)
        assertTrue(cancelled2)
        assertEquals(50, currentTime - t1, "onCancel после таймаута на 50")
    }

    @Test fun `СЛ20 parallelWithDeadline`() = runTest {
        val t0 = currentTime
        val ok = CancellationTasks.parallelWithDeadline(1000, listOf({ delay(10); 1 }, { delay(20); 2 }))
        assertEquals(listOf(1, 2), ok)
        assertEquals(20, currentTime - t0, "параллельно → max(10,20) = 20")
        val t1 = currentTime
        val late = CancellationTasks.parallelWithDeadline(50, listOf({ delay(1000); 1 }))
        assertNull(late)
        assertEquals(50, currentTime - t1, "отмена на дедлайне 50")
    }
}
