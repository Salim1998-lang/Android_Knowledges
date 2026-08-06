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
 * Тесты темы 5. Идентичность диспетчера проверяем через перехватчик контекста
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
        // limit=1 → строгая сериализация: одновременно максимум один
        assertEquals(1, DispatchersTasks.observedConcurrency(limit = 1, tasks = 8))
    }

    @Test fun `С13 runWithNameOnIO`() = runTest {
        assertEquals("sync" to true, DispatchersTasks.runWithNameOnIO("sync"))
        assertEquals("other" to true, DispatchersTasks.runWithNameOnIO("other")) // другое имя — не хардкод
    }

    @Test fun `С14 mapOnIO сохраняет порядок`() = runTest {
        assertEquals(listOf(1, 4, 9, 16), DispatchersTasks.mapOnIO(listOf(1, 2, 3, 4)) { it * it })
        assertEquals(emptyList<Int>(), DispatchersTasks.mapOnIO(emptyList<Int>()) { it })     // пусто
        assertEquals(listOf("1x", "2x"), DispatchersTasks.mapOnIO(listOf(1, 2)) { "${it}x" }) // смена типа
    }

    @Test fun `С15 namePropagatesToAsync`() = runTest {
        assertEquals("worker", DispatchersTasks.namePropagatesToAsync("worker"))
        assertEquals("other", DispatchersTasks.namePropagatesToAsync("other")) // другое имя — не хардкод
    }

    // ── Сложные ──

    @Test fun `СЛ16 boundedParallelSum`() = runTest {
        assertEquals((1..100).sum(), DispatchersTasks.boundedParallelSum((1..100).toList(), parallelism = 4))
        assertEquals((1..100).sum(), DispatchersTasks.boundedParallelSum((1..100).toList(), parallelism = 1)) // сериализация
        assertEquals(0, DispatchersTasks.boundedParallelSum(emptyList(), parallelism = 4))                    // пусто → 0
    }

    @Test fun `СЛ17 dispatcherRoundTrip`() = runTest {
        assertEquals(listOf(true, true, true), DispatchersTasks.dispatcherRoundTrip())
    }

    @Test fun `СЛ18 childJobIsChildOfParent`() = runTest {
        assertTrue(DispatchersTasks.childJobIsChildOfParent())
    }

    @Test fun `СЛ19 fetchThenProcess`() = runTest {
        assertEquals(Triple(true, true, 21), DispatchersTasks.fetchThenProcess(10))
        assertEquals(Triple(true, true, 7), DispatchersTasks.fetchThenProcess(3))   // другой вход (3*2+1)
        assertEquals(Triple(true, true, 1), DispatchersTasks.fetchThenProcess(0))   // 0*2+1
    }

    @Test fun `СЛ20 stagedPipeline сохраняет порядок`() = runTest {
        assertEquals(listOf(3, 5, 7), DispatchersTasks.stagedPipeline(listOf(1, 2, 3)))
        assertEquals(emptyList<Int>(), DispatchersTasks.stagedPipeline(emptyList()))        // пусто
        assertEquals(listOf(21), DispatchersTasks.stagedPipeline(listOf(10)))               // единичный (10*2+1)
        // порядок сохраняется даже когда «загрузка» идёт вразнобой
        assertEquals(listOf(11, 3, 15, 7), DispatchersTasks.stagedPipeline(listOf(5, 1, 7, 3)))
    }

    // ── Корутинные свойства: переключение диспетчеров реально ──

    @Test fun `КС1 observedConcurrency граница lim=1 строго серийна`() = runTest {
        // при лимите 1 в один момент выполняется РОВНО одна корутина, сколько бы задач ни было
        assertEquals(1, DispatchersTasks.observedConcurrency(limit = 1, tasks = 16))
    }

    @Test fun `КС2 fetchThenProcess действительно меняет диспетчер IO→Default`() = runTest {
        // флаги true подтверждают, что fetch шёл на IO, а process — на Default (не на тестовом диспетчере)
        val (onIO, onDefault, _) = DispatchersTasks.fetchThenProcess(21)
        assertTrue(onIO && onDefault, "этапы выполнялись на заявленных диспетчерах")
    }
}
