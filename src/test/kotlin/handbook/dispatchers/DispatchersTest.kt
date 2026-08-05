package handbook.dispatchers

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

/**
 * Тесты темы 8. Идентичность диспетчера проверяем через перехватчик контекста
 * (`coroutineContext[ContinuationInterceptor] === Dispatchers.X`), имя — через [CoroutineName].
 * Тесты, которым нужен реальный диспетчер, оборачивают вызов в `withContext(Dispatchers.Default)`,
 * чтобы уйти с тестового диспатчера `runTest`.
 */
class DispatchersTest {

    // ── Лёгкие ──

    @Test fun `Л1 currentName читает имя из контекста`() = runTest {
        assertNull(DispatchersTasks.currentName())
        assertEquals("abc", withContext(CoroutineName("abc")) { DispatchersTasks.currentName() })
    }

    @Test fun `Л2 onIO возвращает результат`() = runTest {
        assertEquals(42, DispatchersTasks.onIO { 42 })
    }

    @Test fun `Л3 onDefault возвращает результат`() = runTest {
        assertEquals(42, DispatchersTasks.onDefault { 42 })
    }

    @Test fun `Л4 runsOn на IO и Default`() = runTest {
        assertTrue(DispatchersTasks.runsOn(Dispatchers.IO))
        assertTrue(DispatchersTasks.runsOn(Dispatchers.Default))
    }

    @Test fun `Л5 ioWithName собирает контекст`() {
        val ctx = DispatchersTasks.ioWithName("sync")
        assertEquals("sync", ctx[CoroutineName]?.name)
        assertEquals(Dispatchers.IO, ctx[kotlin.coroutines.ContinuationInterceptor])
    }

    @Test fun `Л6 nameUnder`() = runTest {
        assertEquals("loader", DispatchersTasks.nameUnder("loader"))
    }

    @Test fun `Л7 rightWins правый переопределяет левый`() {
        val merged: CoroutineContext = DispatchersTasks.rightWins(CoroutineName("a"), CoroutineName("b"))
        assertEquals("b", merged[CoroutineName]?.name)
    }

    @Test fun `Л8 withoutName убирает имя`() {
        val ctx = Dispatchers.IO + CoroutineName("x")
        assertNull(DispatchersTasks.withoutName(ctx)[CoroutineName])
    }

    // ── Средние ──

    @Test fun `С9 childInheritsName`() = runTest {
        assertEquals("parent", DispatchersTasks.childInheritsName("parent"))
    }

    @Test fun `С10 childOverridesName`() = runTest {
        assertEquals("child", DispatchersTasks.childOverridesName("parent", "child"))
    }

    @Test fun `С11 switchIOtoDefault`() = runTest {
        assertEquals(true to true, DispatchersTasks.switchIOtoDefault())
    }

    @Test fun `С12 observedConcurrency лимит реально ограничивает`() = runTest {
        // limit=2 безопасно: Dispatchers.Default всегда имеет >= 2 потока.
        assertEquals(2, DispatchersTasks.observedConcurrency(limit = 2, tasks = 8))
    }

    @Test fun `С13 runWithNameOnIO`() = runTest {
        assertEquals("sync" to true, DispatchersTasks.runWithNameOnIO("sync"))
    }

    @Test fun `С14 mapOnIO сохраняет порядок`() = runTest {
        assertEquals(listOf(1, 4, 9, 16), DispatchersTasks.mapOnIO(listOf(1, 2, 3, 4)) { it * it })
    }

    @Test fun `С15 namePropagatesToAsync`() = runTest {
        assertEquals("worker", DispatchersTasks.namePropagatesToAsync("worker"))
    }

    // ── Сложные ──

    @Test fun `СЛ16 boundedParallelSum`() = runTest {
        assertEquals((1..100).sum(), DispatchersTasks.boundedParallelSum((1..100).toList(), parallelism = 4))
    }

    @Test fun `СЛ17 dispatcherRoundTrip`() = runTest {
        assertEquals(listOf(true, true, true), DispatchersTasks.dispatcherRoundTrip())
    }

    @Test fun `СЛ18 childJobIsChildOfParent`() = runTest {
        assertTrue(DispatchersTasks.childJobIsChildOfParent())
    }

    @Test fun `СЛ19 fetchThenProcess`() = runTest {
        assertEquals(Triple(true, true, 21), DispatchersTasks.fetchThenProcess(10))
    }

    @Test fun `СЛ20 stagedPipeline сохраняет порядок`() = runTest {
        assertEquals(listOf(3, 5, 7), DispatchersTasks.stagedPipeline(listOf(1, 2, 3)))
    }
}
