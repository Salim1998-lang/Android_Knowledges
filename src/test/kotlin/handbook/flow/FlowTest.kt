package handbook.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 6. Операторные тесты проверяют результат. Отдельная группа «корутинные свойства»
 * проверяет то, что делает Flow именно корутинным примитивом: ХОЛОДНОСТЬ (тело не исполняется до
 * collect), ПОСЛЕДОВАТЕЛЬНОСТЬ с backpressure (задержки в потоке двигают виртуальное время) и
 * РАННЮЮ ОТМЕНУ upstream (`take` обрывает источник до его задержек).
 */
class FlowTest {

    // ── Лёгкие ──

    @Test fun `Л1 rangeFlow`() = runTest {
        assertEquals(listOf(1, 2, 3, 4, 5), FlowTasks.rangeFlow(5).toList())
        assertEquals(listOf(1), FlowTasks.rangeFlow(1).toList())         // граница снизу
        assertEquals(emptyList<Int>(), FlowTasks.rangeFlow(0).toList())  // n=0 → пусто
    }

    @Test fun `Л2 evenSquares`() = runTest {
        assertEquals(listOf(4, 16, 36), FlowTasks.evenSquares(flowOf(1, 2, 3, 4, 5, 6)).toList())
        assertEquals(emptyList<Int>(), FlowTasks.evenSquares(flowOf(1, 3, 5)).toList()) // все нечётные
        assertEquals(listOf(100), FlowTasks.evenSquares(flowOf(10)).toList())           // другой вход (не хардкод)
    }

    @Test fun `Л3 fromList`() = runTest {
        assertEquals(listOf("a", "b"), FlowTasks.fromList(listOf("a", "b")).toList())
        assertEquals(emptyList<String>(), FlowTasks.fromList(emptyList<String>()).toList())
    }

    @Test fun `Л4 mapLength`() = runTest {
        assertEquals(listOf(1, 3, 2), FlowTasks.mapLength(flowOf("a", "bbb", "cc")).toList())
        assertEquals(listOf(0), FlowTasks.mapLength(flowOf("")).toList()) // пустая строка → 0
    }

    @Test fun `Л5 takeN`() = runTest {
        assertEquals(listOf(1, 2), FlowTasks.takeN(flowOf(1, 2, 3, 4), 2).toList())
        assertEquals(listOf(1), FlowTasks.takeN(flowOf(1, 2, 3), 1).toList())          // граница снизу (take требует n>=1)
        assertEquals(listOf(1, 2, 3), FlowTasks.takeN(flowOf(1, 2, 3), 9).toList())    // take > размера
    }

    @Test fun `Л6 dropN`() = runTest {
        assertEquals(listOf(3, 4), FlowTasks.dropN(flowOf(1, 2, 3, 4), 2).toList())
        assertEquals(listOf(1, 2, 3), FlowTasks.dropN(flowOf(1, 2, 3), 0).toList())    // drop 0 → всё
        assertEquals(emptyList<Int>(), FlowTasks.dropN(flowOf(1, 2), 5).toList())      // drop > размера
    }

    @Test fun `Л7 countItems`() = runTest {
        assertEquals(4, FlowTasks.countItems(flowOf(1, 2, 3, 4)))
        assertEquals(0, FlowTasks.countItems(flowOf<Int>()))          // пусто → 0
        assertEquals(1, FlowTasks.countItems(flowOf(42)))
    }

    @Test fun `Л8 toListSorted`() = runTest {
        assertEquals(listOf(1, 2, 3, 5), FlowTasks.toListSorted(flowOf(3, 1, 5, 2)))
        assertEquals(listOf(-3, -1, 0, 2), FlowTasks.toListSorted(flowOf(2, -1, -3, 0))) // отрицательные
        assertEquals(listOf(1, 1, 2), FlowTasks.toListSorted(flowOf(2, 1, 1)))           // дубликаты сохраняются
    }

    // ── Средние ──

    @Test fun `С9 runningTotal`() = runTest {
        assertEquals(listOf(1, 3, 6, 10), FlowTasks.runningTotal(flowOf(1, 2, 3, 4)).toList())
        assertEquals(listOf(5, 3, 8), FlowTasks.runningTotal(flowOf(5, -2, 5)).toList()) // отрицательные
        assertEquals(emptyList<Int>(), FlowTasks.runningTotal(flowOf<Int>()).toList())
    }

    @Test fun `С10 runningMax`() = runTest {
        assertEquals(listOf(3, 3, 4, 4, 5), FlowTasks.runningMax(flowOf(3, 1, 4, 1, 5)).toList())
        assertEquals(listOf(5, 5, 5), FlowTasks.runningMax(flowOf(5, 4, 3)).toList()) // убывающий → держит max
    }

    @Test fun `С11 duplicateEach`() = runTest {
        assertEquals(listOf("a", "a", "b", "b"), FlowTasks.duplicateEach(flowOf("a", "b")).toList())
        assertEquals(emptyList<String>(), FlowTasks.duplicateEach(flowOf<String>()).toList())
    }

    @Test fun `С12 dedupAdjacent`() = runTest {
        assertEquals(listOf(1, 2, 1), FlowTasks.dedupAdjacent(flowOf(1, 1, 2, 2, 2, 1)).toList())
        assertEquals(listOf(1, 2, 3), FlowTasks.dedupAdjacent(flowOf(1, 2, 3)).toList()) // без повторов — как есть
        assertEquals(listOf(7), FlowTasks.dedupAdjacent(flowOf(7, 7, 7, 7)).toList())    // всё одинаково → один
    }

    @Test fun `С13 zipSum`() = runTest {
        assertEquals(listOf(11, 22, 33), FlowTasks.zipSum(flowOf(1, 2, 3), flowOf(10, 20, 30)).toList())
        // zip завершается на КОРОТКОМ потоке
        assertEquals(listOf(11, 22), FlowTasks.zipSum(flowOf(1, 2, 3), flowOf(10, 20)).toList())
    }

    @Test fun `С14 indexedPairs`() = runTest {
        assertEquals(listOf(0 to "a", 1 to "b"), FlowTasks.indexedPairs(flowOf("a", "b")).toList())
        assertEquals(emptyList<Pair<Int, String>>(), FlowTasks.indexedPairs(flowOf<String>()).toList())
    }

    @Test fun `С15 foldSum`() = runTest {
        assertEquals(10, FlowTasks.foldSum(flowOf(1, 2, 3, 4)))
        assertEquals(0, FlowTasks.foldSum(flowOf<Int>()))               // пусто → 0 (начальный аккумулятор)
        assertEquals(0, FlowTasks.foldSum(flowOf(5, -5)))               // другой вход
    }

    // ── Сложные ──

    @Test fun `СЛ16 chunked`() = runTest {
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), FlowTasks.chunked(flowOf(1, 2, 3, 4, 5), 2).toList())
        assertEquals(emptyList<List<Int>>(), FlowTasks.chunked(flowOf<Int>(), 3).toList()) // пустой источник
        assertEquals(listOf(listOf(1, 2, 3)), FlowTasks.chunked(flowOf(1, 2, 3), 3).toList()) // ровно один батч
        assertEquals(listOf(listOf(1), listOf(2)), FlowTasks.chunked(flowOf(1, 2), 1).toList()) // size=1
    }

    @Test fun `СЛ17 withFallback`() = runTest {
        val failing = flow { emit(1); emit(2); error("boom") }
        assertEquals(listOf(1, 2, -1), FlowTasks.withFallback(failing, -1).toList())
    }

    @Test fun `СЛ18 retryUpstream`() = runTest {
        val attempts = AtomicInteger(0)
        val flaky = flow {
            val n = attempts.incrementAndGet()
            emit(1)
            if (n < 3) error("try again") // упадёт на первых двух подписках
            emit(2)
        }
        assertEquals(listOf(1, 1, 1, 2), FlowTasks.retryUpstream(flaky, retries = 2).toList())
    }

    @Test fun `СЛ19 expand`() = runTest {
        assertEquals(listOf(2, 2, 3, 3, 3), FlowTasks.expand(flowOf(2, 3)).toList())
        assertEquals(listOf(1), FlowTasks.expand(flowOf(1)).toList())        // n=1 → одна копия
        assertEquals(emptyList<Int>(), FlowTasks.expand(flowOf(0)).toList()) // n=0 → нет копий
    }

    @Test fun `СЛ20 batchSums`() = runTest {
        assertEquals(listOf(3, 7, 5), FlowTasks.batchSums(flowOf(1, 2, 3, 4, 5), 2).toList())
        assertEquals(listOf(3, 7), FlowTasks.batchSums(flowOf(1, 2, 3, 4), 2).toList())      // ровно кратно (без остатка)
        assertEquals(listOf(1, 2, 3), FlowTasks.batchSums(flowOf(1, 2, 3), 1).toList())      // size=1 → сами элементы
    }

    // ── Корутинные свойства Flow ──

    @Test fun `КС1 evenSquares холодный - тело источника не бежит без collect`() = runTest {
        val ran = AtomicInteger(0)
        val source = flow { ran.incrementAndGet(); emit(2); emit(4) }
        val transformed = FlowTasks.evenSquares(source) // ещё НЕ собрали
        assertEquals(0, ran.get(), "Flow холодный: тело не должно запуститься до collect")
        assertEquals(listOf(4, 16), transformed.toList())
        assertEquals(1, ran.get(), "после collect тело выполнилось ровно один раз")
    }

    @Test fun `КС2 backpressure - задержки источника двигают виртуальное время последовательно`() = runTest {
        val source = flow { for (i in 1..3) { delay(10); emit(i) } }
        val t0 = currentTime
        assertEquals(listOf(1, 2, 3), FlowTasks.takeN(source, 3).toList())
        assertEquals(30, currentTime - t0, "3 эмиссии по delay(10) обрабатываются последовательно")
    }

    @Test fun `КС3 takeN отменяет upstream до его задержки`() = runTest {
        val source = flow { emit(1); emit(2); delay(1000); emit(3) }
        val t0 = currentTime
        assertEquals(listOf(1, 2), FlowTasks.takeN(source, 2).toList())
        assertEquals(0, currentTime - t0, "take(2) обрывает источник до delay(1000) третьей эмиссии")
    }

    @Test fun `КС4 холодность - каждый collect ПЕРЕЗАПУСКАЕТ тело источника`() = runTest {
        val runs = AtomicInteger(0)
        val source = flow { runs.incrementAndGet(); emit(1); emit(2) }
        val f = FlowTasks.runningTotal(source)
        assertEquals(0, runs.get(), "до collect тело не бежит")
        assertEquals(listOf(1, 3), f.toList())     // первый сбор
        assertEquals(listOf(1, 3), f.toList())     // второй сбор — тот же результат
        assertEquals(2, runs.get(), "два collect → тело выполнилось ДВАЖДЫ (холодный поток)")
    }

    // ── Мост с колбэк-API ──

    @Test fun `СЛ21 emitterFlow мостит колбэк и снимает подписку`() = runTest {
        val emitter = IntEmitter()
        val collected = mutableListOf<Int>()
        // Unconfined-диспетчер: сборщик подписывается сразу и получает значения по мере emit.
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowTasks.emitterFlow(emitter).collect { collected.add(it) }
        }
        assertTrue(emitter.isSubscribed(), "collect должен подписаться на источник")
        emitter.emit(1); emitter.emit(2); emitter.emit(3)
        emitter.complete()
        job.join()
        assertEquals(listOf(1, 2, 3), collected)
        assertFalse(emitter.isSubscribed(), "awaitClose должен снять подписку по завершении")
    }

    @Test fun `СЛ22 mergeConcurrently сливает все источники`() = runTest {
        val merged = FlowTasks.mergeConcurrently(listOf(flowOf(1, 2, 3), flowOf(4, 5, 6))).toList()
        assertEquals(setOf(1, 2, 3, 4, 5, 6), merged.toSet())
    }
}
