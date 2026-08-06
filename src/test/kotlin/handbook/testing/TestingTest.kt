package handbook.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Тесты темы 10. Задачи — расширения TestScope, поэтому вызываются `with(TestingTasks) { ... }`
 * внутри `runTest`. Виртуальное время `runTest` делает задержки мгновенными и детерминированными.
 * Для СЛ16 (`runOnMain`) подменяем Dispatchers.Main тестовым диспетчером (setMain/resetMain).
 */
class TestingTest {

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    // ── Лёгкие ──

    @Test fun `Л1 elapsed`() = runTest {
        with(TestingTasks) {
            assertEquals(500, elapsed { delay(500) })
            assertEquals(250, elapsed { delay(250) })  // другой интервал — не хардкод
            assertEquals(0, elapsed { })               // нет задержки → 0
        }
    }

    // ВНИМАНИЕ: sequential/concurrentDelays/advanceAndTime возвращают АБСОЛЮТНЫЙ currentTime,
    // а часы в одном runTest общие — поэтому один вызов на тест (нетривиальный вход бьёт хардкод).

    @Test fun `Л2 sequentialDelays складываются`() = runTest {
        with(TestingTasks) { assertEquals(75, sequentialDelays(25, 50)) } // не круглое, не 300
    }

    @Test fun `Л3 concurrentDelays это максимум`() = runTest {
        with(TestingTasks) { assertEquals(300, concurrentDelays(300, 100)) } // max, а не порядок/сумма
    }

    @Test fun `Л4 advanceAndTime`() = runTest {
        with(TestingTasks) { assertEquals(42, advanceAndTime(42)) } // не круглое значение
    }

    @Test fun `Л5 pendingUntilIdle`() = runTest {
        with(TestingTasks) { assertEquals(42, pendingUntilIdle()) }
    }

    @Test fun `Л6 runCurrentImmediate`() = runTest {
        with(TestingTasks) { assertEquals(1 to 0, runCurrentImmediate()) }
    }

    @Test fun `Л7 advancePartial`() = runTest {
        with(TestingTasks) { assertEquals(1 to 0, advancePartial()) }
    }

    @Test fun `Л8 delayedValue`() = runTest {
        with(TestingTasks) {
            assertEquals(7, delayedValue(1000, 7))
            assertEquals(-3, delayedValue(50, -3)) // другое значение — возвращает именно его
        }
    }

    // ── Средние ──

    @Test fun `С9 standardIsLazy`() = runTest {
        with(TestingTasks) { assertEquals(false to true, standardIsLazy()) }
    }

    @Test fun `С10 unconfinedIsEager`() = runTest {
        with(TestingTasks) { assertTrue(unconfinedIsEager()) }
    }

    @Test fun `С11 orderedByDelay`() = runTest {
        with(TestingTasks) { assertEquals(listOf(1, 2, 3), orderedByDelay()) }
    }

    @Test fun `С12 currentTimeAtEachStep`() = runTest {
        with(TestingTasks) { assertEquals(listOf(100L, 200L, 300L), currentTimeAtEachStep()) }
    }

    @Test fun `С13 timeoutSucceeds`() = runTest {
        with(TestingTasks) { assertEquals(7, timeoutSucceeds()) }
    }

    @Test fun `С14 timeoutFails`() = runTest {
        with(TestingTasks) { assertTrue(timeoutFails()) }
    }

    @Test fun `С15 collectWithVirtualTime`() = runTest {
        with(TestingTasks) { assertEquals(listOf(1, 2, 3) to 300L, collectWithVirtualTime()) }
    }

    // ── Сложные ──

    @Test fun `СЛ16 runOnMain под подменённым Main`() = runTest {
        assertEquals("on-main", TestingTasks.runOnMain())
    }

    @Test fun `СЛ17 retryWithBackoff`() = runTest {
        with(TestingTasks) {
            assertEquals(4 to 700L, retryWithBackoff(failTimes = 3))   // 100+200+400
            assertEquals(1 to 0L, retryWithBackoff(failTimes = 0))     // успех с первой попытки, без задержек
            assertEquals(3 to 300L, retryWithBackoff(failTimes = 2))   // 100+200
        }
    }

    @Test fun `СЛ18 backgroundTicker не виснет`() = runTest {
        with(TestingTasks) {
            assertEquals(listOf(1, 2, 3, 4, 5), backgroundTicker(period = 100, steps = 5))
            assertEquals(listOf(1, 2, 3), backgroundTicker(period = 50, steps = 3)) // другие период/шаги
        }
    }

    @Test fun `СЛ19 parallelAwaitAll`() = runTest {
        with(TestingTasks) {
            assertEquals(listOf(100L, 200L, 300L) to 300L, parallelAwaitAll(100, 200, 300))
            // время == max, результаты сохраняют порядок аргументов даже при разных задержках
            assertEquals(listOf(300L, 50L, 150L) to 300L, parallelAwaitAll(300, 50, 150))
        }
    }

    @Test fun `СЛ20 sequentialVsParallel`() = runTest {
        with(TestingTasks) { assertEquals(600L to 300L, sequentialVsParallel()) }
    }
}
