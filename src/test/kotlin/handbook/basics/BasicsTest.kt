package handbook.basics

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 1. Кроме результата проверяют ВИРТУАЛЬНОЕ ВРЕМЯ (`currentTime`): что корутина
 * реально приостанавливалась через `delay` на ожидаемое число мс. Это отсекает решения без
 * задержки, с `Thread.sleep` (реальное время виртуальное не двигает) и — для конкурентных
 * задач — последовательные реализации вместо параллельных (сумма задержек вместо максимума).
 */
class BasicsTest {

    // ── Лёгкие ──

    @Test fun `Л1 delayedValue`() = runTest {
        val t0 = currentTime
        assertEquals(42, BasicsTasks.delayedValue(42))
        assertEquals(100, currentTime - t0, "должна быть delay(100)")
    }

    @Test fun `Л2 greet`() = runTest {
        val t0 = currentTime
        assertEquals("Привет, Kotlin!", BasicsTasks.greet("Kotlin"))
        assertEquals(50, currentTime - t0, "должна быть delay(50)")
    }

    @Test fun `Л3 sumAfterDelay`() = runTest {
        val t0 = currentTime
        assertEquals(7, BasicsTasks.sumAfterDelay(3, 4))
        assertEquals(10, currentTime - t0, "должна быть delay(10)")
    }

    @Test fun `Л4 sequentialSum`() = runTest {
        assertEquals(11, BasicsTasks.sequentialSum({ 5 }, { 6 }))
    }

    @Test fun `Л5 doubled`() = runTest {
        val t0 = currentTime
        assertEquals(20, BasicsTasks.doubled(10))
        assertEquals(10, currentTime - t0, "должна быть delay(10)")
    }

    @Test fun `Л6 repeatValue`() = runTest {
        val t0 = currentTime
        assertEquals(listOf("x", "x", "x"), BasicsTasks.repeatValue("x", 3))
        assertEquals(15, currentTime - t0, "3 × delay(5)")
    }

    @Test fun `Л7 countdown`() = runTest {
        val t0 = currentTime
        assertEquals(listOf(3, 2, 1), BasicsTasks.countdown(3))
        assertEquals(15, currentTime - t0, "3 × delay(5)")
    }

    @Test fun `Л8 runInChild`() = runTest {
        var ran = false
        BasicsTasks.runInChild { ran = true }
        assertTrue(ran)
    }

    // ── Средние ──

    @Test fun `С9 collectInLaunchOrder`() = runTest {
        val t0 = currentTime
        assertEquals(listOf(4, 3, 2, 1, 0), BasicsTasks.collectInLaunchOrder(5))
        // Конкурентно: время = МАКСИМУМ задержек (i=0 → 50), а не сумма. Ловит последовательную реализацию.
        assertEquals(50, currentTime - t0, "корутины должны идти параллельно (max=50), а не по очереди")
    }

    @Test fun `С10 launchN вызывает все`() = runTest {
        val seen = sortedSetOf<Int>()
        val count = BasicsTasks.launchN(4) { seen.add(it) }
        assertEquals(4, count)
        assertEquals(setOf(0, 1, 2, 3), seen)
    }

    @Test fun `С11 sequentialChain`() = runTest {
        val result = BasicsTasks.sequentialChain(1, listOf({ it + 1 }, { it * 3 }, { it - 2 }))
        assertEquals(4, result) // ((1+1)*3)-2
    }

    @Test fun `С12 delayedSum`() = runTest {
        val t0 = currentTime
        assertEquals(10, BasicsTasks.delayedSum(listOf(1, 2, 3, 4)))
        assertEquals(20, currentTime - t0, "4 × delay(5) последовательно")
    }

    @Test fun `С13 buildGreetings`() = runTest {
        val t0 = currentTime
        assertEquals(listOf("Привет, A!", "Привет, B!"), BasicsTasks.buildGreetings(listOf("A", "B")))
        // Последовательно: сумма = 2 × delay(50). Конкурентная реализация дала бы 50 и не прошла.
        assertEquals(100, currentTime - t0, "приветствия строятся последовательно (2 × 50)")
    }

    @Test fun `С14 counterWithLaunches`() = runTest {
        val t0 = currentTime
        assertEquals(100, BasicsTasks.counterWithLaunches(100))
        // 100 корутин параллельно, каждая delay(1) → общее время 1, а не 100.
        assertEquals(1, currentTime - t0, "100 корутин должны идти параллельно (время=1)")
    }

    @Test fun `С15 orderedByDelay`() = runTest {
        val t0 = currentTime
        // задержки 30,10,20 → порядок завершения индексов: 1,2,0
        assertEquals(listOf(1, 2, 0), BasicsTasks.orderedByDelay(listOf(30, 10, 20)))
        assertEquals(30, currentTime - t0, "параллельно → max задержек = 30")
    }

    // ── Сложные ──

    @Test fun `СЛ16 retry успех сразу`() = runTest {
        var calls = 0
        val t0 = currentTime
        assertEquals("ok", BasicsTasks.retry(3) { calls++; "ok" })
        assertEquals(1, calls)
        assertEquals(0, currentTime - t0, "успех с первой попытки → без delay между попытками")
    }

    @Test fun `СЛ16 retry до успеха и проброс`() = runTest {
        var calls = 0
        val t0 = currentTime
        assertEquals("ok", BasicsTasks.retry(3) { calls++; if (calls < 3) error("x") else "ok" })
        assertEquals(3, calls)
        assertEquals(40, currentTime - t0, "2 неудачи → 2 × delay(20)")

        val t1 = currentTime
        val err = runCatching { BasicsTasks.retry<String>(2) { error("boom") } }.exceptionOrNull()
        assertTrue(err is IllegalStateException)
        assertEquals("boom", err?.message)
        assertEquals(20, currentTime - t1, "2 попытки → 1 delay(20) между ними")
    }

    @Test fun `СЛ17 pollUntil`() = runTest {
        var current = 0
        val t0 = currentTime
        val attempts = BasicsTasks.pollUntil(target = 3) { current++ } // вернёт 0,1,2,3
        assertEquals(4, attempts)
        assertEquals(15, currentTime - t0, "3 неудачные попытки → 3 × delay(5)")
    }

    @Test fun `СЛ18 factorial`() = runTest {
        val t0 = currentTime
        assertEquals(1L, BasicsTasks.factorial(0))
        assertEquals(0, currentTime - t0, "factorial(0) без delay")

        val t1 = currentTime
        assertEquals(120L, BasicsTasks.factorial(5))
        assertEquals(4, currentTime - t1, "рекурсия n=5..2 → 4 × delay(1)")
    }

    @Test fun `СЛ19 lazyStart`() = runTest {
        var started = false
        assertTrue(BasicsTasks.lazyStart(trigger = true) { started = true })
        assertTrue(started)

        var started2 = false
        assertFalse(BasicsTasks.lazyStart(trigger = false) { started2 = true })
        assertFalse(started2)
    }

    @Test fun `СЛ20 accumulate`() = runTest {
        val t0 = currentTime
        assertEquals(50, BasicsTasks.accumulate(n = 10, step = 5))
        assertEquals(10, currentTime - t0, "10 шагов × delay(1)")
    }

    // ── Дополнительные ──

    @Test fun `Д21 roundRobin чередует через yield`() = runTest {
        // yield заставляет первый проход всех корутин идти до второго.
        // Решение без yield дало бы [0,0,1,1,2,2] → тест отвергнет.
        assertEquals(listOf(0, 1, 2, 0, 1, 2), BasicsTasks.roundRobin(3))
    }

    @Test fun `Д22 lazyJobFlags состояния New и Cancelled`() = runTest {
        val (new, cancelled) = BasicsTasks.lazyJobFlags()
        // New: ничего не активно и не завершено.
        assertEquals(Triple(false, false, false), new, "New: все флаги false")
        // Cancelled: isCompleted тоже true (корутина «доехала» до конца через отмену).
        assertEquals(Triple(false, true, true), cancelled, "Cancelled: completed=true, cancelled=true")
    }

    @Test fun `Д23 queueBeforeSuspend ребёнок бежит в suspend-точке`() = runTest {
        // launch лишь ставит в очередь: до yield ребёнок не выполняется.
        // Модель «launch запускает сразу» дала бы [child, parent-before, parent-after] → отвергнется.
        assertEquals(
            listOf("parent-before", "child", "parent-after"),
            BasicsTasks.queueBeforeSuspend(),
        )
    }
}
