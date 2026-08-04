package handbook.exceptions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 4. Тема про обработку ошибок, но там, где есть задержки, проверяем и ВИРТУАЛЬНОЕ ВРЕМЯ:
 * что независимые загрузки идут параллельно (max, а не сумма), fail-fast отменяет сиблингов сразу
 * (не дожидаясь их задержек), а retry действительно выдерживает паузы между попытками.
 */
class ExceptionsTest {

    // ── Лёгкие ──

    @Test fun `Л1 coRunCatching`() = runTest {
        assertEquals(5, ExceptionsTasks.coRunCatching { 5 }.getOrNull())
        assertTrue(ExceptionsTasks.coRunCatching<Int> { error("x") }.isFailure)
    }

    @Test fun `Л1 coRunCatching пробрасывает Cancellation`() {
        assertThrows(CancellationException::class.java) {
            runTest { ExceptionsTasks.coRunCatching<Int> { throw CancellationException("c") } }
        }
    }

    @Test fun `Л2 successOrNull`() = runTest {
        assertEquals(3, ExceptionsTasks.successOrNull { 3 })
        assertNull(ExceptionsTasks.successOrNull<Int> { error("x") })
    }

    @Test fun `Л3 getOrDefault`() = runTest {
        assertEquals(9, ExceptionsTasks.getOrDefault(9) { error("x") })
        assertEquals(1, ExceptionsTasks.getOrDefault(9) { 1 })
    }

    @Test fun `Л4 recoverWith`() = runTest {
        assertEquals(-1, ExceptionsTasks.recoverWith<Int>({ error("x") }, { -1 }))
    }

    @Test fun `Л5 isFailure`() = runTest {
        assertTrue(ExceptionsTasks.isFailure { error("x") })
        assertFalse(ExceptionsTasks.isFailure { })
    }

    @Test fun `Л6 catchingTransform`() = runTest {
        assertEquals(4, ExceptionsTasks.catchingTransform(2) { it * it }.getOrNull())
        assertTrue(ExceptionsTasks.catchingTransform(0) { 1 / it }.isFailure)
    }

    @Test fun `Л7 firstSuccessful`() = runTest {
        val t0 = currentTime
        assertEquals(42, ExceptionsTasks.firstSuccessful(listOf({ error("n") }, { delay(5); 42 }, { 99 })))
        assertEquals(5, currentTime - t0, "перебор последовательный: 1-й упал, 2-й ждал 5")
        assertNull(ExceptionsTasks.firstSuccessful(listOf({ error("a") }, { error("b") })))
    }

    @Test fun `Л8 countFailures`() = runTest {
        assertEquals(2, ExceptionsTasks.countFailures(listOf({ error("a") }, { }, { error("b") })))
    }

    // ── Средние ──

    @Test fun `С9 loadAllIndependently изолирует`() = runTest {
        val t0 = currentTime
        val r = ExceptionsTasks.loadAllIndependently(
            listOf({ delay(10); "ok-1" }, { delay(20); error("fail-2") }, { delay(30); "ok-3" })
        )
        assertEquals("ok-1", r[0].getOrNull())
        assertTrue(r[1].isFailure)
        assertEquals("fail-2", r[1].exceptionOrNull()?.message)
        assertEquals("ok-3", r[2].getOrNull())
        assertEquals(30, currentTime - t0, "загрузки параллельны и независимы → max = 30, а не 60")
    }

    @Test fun `С10 partitionResults`() = runTest {
        val (ok, err) = ExceptionsTasks.partitionResults<Int>(listOf({ 1 }, { error("e") }, { 3 }))
        assertEquals(listOf(1, 3), ok)
        assertEquals(1, err.size)
    }

    @Test fun `С11 sumSuccesses`() = runTest {
        assertEquals(4, ExceptionsTasks.sumSuccesses(listOf({ 1 }, { error("x") }, { 3 })))
    }

    @Test fun `С12 recoverEach`() = runTest {
        val r = ExceptionsTasks.recoverEach<Int>(listOf({ 1 }, { error("x") }, { 3 })) { -1 }
        assertEquals(listOf(1, -1, 3), r)
    }

    @Test fun `С13 failureMessages`() = runTest {
        val r = ExceptionsTasks.failureMessages<Int>(listOf({ 1 }, { error("boom") }, { error("bang") }))
        assertEquals(listOf("boom", "bang"), r)
    }

    @Test fun `С14 successCount`() = runTest {
        assertEquals(2, ExceptionsTasks.successCount<Int>(listOf({ 1 }, { error("x") }, { 3 })))
    }

    @Test fun `С15 firstSuccessfulResult`() = runTest {
        val t0 = currentTime
        assertEquals(2, ExceptionsTasks.firstSuccessfulResult(listOf<suspend () -> Int>({ error("a") }, { delay(5); 2 }, { 3 })))
        assertEquals(5, currentTime - t0, "кандидаты стартуют параллельно, первый успешный готов на 5")
        assertNull(ExceptionsTasks.firstSuccessfulResult<Int>(listOf({ error("a") }, { error("b") })))
    }

    // ── Сложные ──

    @Test fun `СЛ16 awaitAllOrCancel fail-fast`() = runTest {
        val t0 = currentTime
        val ok = ExceptionsTasks.awaitAllOrCancel(listOf({ delay(10); 1 }, { delay(20); 2 }))
        assertEquals(listOf(1, 2), ok)
        assertEquals(20, currentTime - t0, "параллельно → max(10,20) = 20")

        val sibling = AtomicBoolean(false)
        val t1 = currentTime
        val err = runCatching {
            ExceptionsTasks.awaitAllOrCancel<Int>(
                listOf({ delay(10); error("boom") }, { delay(1000); sibling.set(true); 2 })
            )
        }.exceptionOrNull()
        assertEquals("boom", err?.message)
        assertFalse(sibling.get(), "медленный сиблинг должен быть отменён")
        assertEquals(10, currentTime - t1, "fail-fast: падение на 10 отменяет сиблинга, не ждём 1000")
    }

    @Test fun `СЛ17 retryResult`() = runTest {
        var calls = 0
        val t0 = currentTime
        val ok = ExceptionsTasks.retryResult(3, 5) { calls++; if (calls < 3) error("x") else "ok" }
        assertEquals("ok", ok.getOrNull())
        assertEquals(10, currentTime - t0, "2 неудачи → 2 паузы по 5")

        val t1 = currentTime
        val fail = ExceptionsTasks.retryResult<String>(2, 5) { error("nope") }
        assertTrue(fail.isFailure)
        assertEquals("nope", fail.exceptionOrNull()?.message)
        assertEquals(5, currentTime - t1, "2 попытки → 1 пауза 5 между ними")
    }

    @Test fun `СЛ18 firstSuccessOf`() = runTest {
        val t0 = currentTime
        assertEquals(7, ExceptionsTasks.firstSuccessOf(listOf<suspend () -> Int>({ error("a") }, { delay(5); 7 })))
        assertEquals(5, currentTime - t0, "кандидаты параллельны, первый успешный готов на 5")
        val err = runCatching {
            ExceptionsTasks.firstSuccessOf<Int>(listOf({ error("a") }, { error("b") }))
        }.exceptionOrNull()
        assertEquals("b", err?.message)
    }

    @Test fun `СЛ19 sumOrFirstError`() = runTest {
        assertEquals(6, ExceptionsTasks.sumOrFirstError(listOf({ 1 }, { 2 }, { 3 })).getOrNull())
        val fail = ExceptionsTasks.sumOrFirstError(listOf({ 1 }, { error("bad") }))
        assertTrue(fail.isFailure)
    }

    @Test fun `СЛ20 countUncaught`() = runTest {
        val n = ExceptionsTasks.countUncaught(listOf({ error("a") }, { }, { error("c") }))
        assertEquals(2, n)
    }

    @Test fun `Д21 collectSuppressed`() = runTest {
        // work падает, close тоже падает → work основной, close сохраняется как suppressed.
        val (primary, suppressed) = ExceptionsTasks.collectSuppressed(
            work = { throw RuntimeException("work failed") },
            close = { throw IllegalStateException("close failed") },
        )
        assertEquals("work failed", primary)
        assertEquals(listOf("close failed"), suppressed)

        // только close падает (work ок) → close становится основным, подавлять нечего.
        val (p2, s2) = ExceptionsTasks.collectSuppressed(
            work = { },
            close = { throw IllegalStateException("close only") },
        )
        assertEquals("close only", p2)
        assertEquals(emptyList<String?>(), s2)

        // ничего не падает.
        val (p3, s3) = ExceptionsTasks.collectSuppressed(work = { }, close = { })
        assertNull(p3)
        assertEquals(emptyList<String?>(), s3)
    }
}
