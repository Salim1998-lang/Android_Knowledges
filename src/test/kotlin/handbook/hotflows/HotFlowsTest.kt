package handbook.hotflows

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Тесты темы 7. Горячие потоки бесконечны, поэтому коллекторы запускаются в `backgroundScope`
 * (иначе runTest повис бы), а состояние проверяется после `runCurrent()`.
 */
class HotFlowsTest {

    // ── Лёгкие ──

    @Test fun `Л1+Л2 mutableIntState + currentValue`() {
        assertEquals(5, HotFlowsTasks.currentValue(HotFlowsTasks.mutableIntState(5)))
        assertEquals(-7, HotFlowsTasks.currentValue(HotFlowsTasks.mutableIntState(-7))) // другой/отрицательный
    }

    @Test fun `Л3 setValue`() {
        val s = MutableStateFlow(0)
        HotFlowsTasks.setValue(s, 9)
        assertEquals(9, s.value)
    }

    @Test fun `Л4 incrementState`() {
        val s = MutableStateFlow(10)
        HotFlowsTasks.incrementState(s)
        HotFlowsTasks.incrementState(s)
        assertEquals(12, s.value) // два инкремента → +2 (не хардкод 11)
    }

    @Test fun `Л5 asReadOnly`() {
        val s = MutableStateFlow(3)
        assertEquals(3, HotFlowsTasks.asReadOnly(s).value)
    }

    @Test fun `Л6 compareAndSetValue`() {
        val s = MutableStateFlow(2)
        assertTrue(HotFlowsTasks.compareAndSetValue(s, 2, 7))
        assertEquals(7, s.value)
        assertFalse(HotFlowsTasks.compareAndSetValue(s, 2, 8)) // ожидаемое не совпало
        assertEquals(7, s.value, "при неуспешном CAS значение не меняется")
    }

    @Test fun `Л7 sharedWithReplay`() = runTest {
        val sf = HotFlowsTasks.sharedWithReplay(2)
        assertTrue(sf.replayCache.isEmpty())      // свежесозданный — пустой replay
        sf.tryEmit(1); sf.tryEmit(2); sf.tryEmit(3)
        assertEquals(listOf(2, 3), sf.replayCache) // replay=2 хранит последние два
    }

    @Test fun `Л8 emitEach`() = runTest {
        val sf = MutableSharedFlow<Int>(replay = 3)
        HotFlowsTasks.emitEach(sf, listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), sf.replayCache)
    }

    // ── Средние ──

    @Test fun `С9 lastTwoReplayed`() {
        assertEquals(listOf(3, 4), HotFlowsTasks.lastTwoReplayed(listOf(1, 2, 3, 4)))
        assertEquals(listOf(8, 9), HotFlowsTasks.lastTwoReplayed(listOf(5, 6, 7, 8, 9))) // другой вход
        assertEquals(listOf(42), HotFlowsTasks.lastTwoReplayed(listOf(42)))               // меньше replay
    }

    @Test fun `С10 dropOldestKeepsLatest`() {
        assertEquals(listOf(4, 5), HotFlowsTasks.dropOldestKeepsLatest(listOf(1, 2, 3, 4, 5)))
        assertEquals(listOf(9, 10), HotFlowsTasks.dropOldestKeepsLatest((1..10).toList())) // держит последние
    }

    @Test fun `С11 subscriberCount`() = runTest {
        val sf = MutableSharedFlow<Int>()
        assertEquals(0, HotFlowsTasks.subscriberCount(sf))
        backgroundScope.launch { sf.collect { } }
        runCurrent()
        assertEquals(1, HotFlowsTasks.subscriberCount(sf))
    }

    @Test fun `С12 firstValue из replay`() = runTest {
        val sf = MutableSharedFlow<Int>(replay = 1)
        sf.tryEmit(7)
        assertEquals(7, HotFlowsTasks.firstValue(sf))
        val sf2 = MutableSharedFlow<Int>(replay = 1)
        sf2.tryEmit(99)
        assertEquals(99, HotFlowsTasks.firstValue(sf2)) // другой источник — не хардкод
    }

    @Test fun `С13 stateFromFlow`() = runTest {
        val state = HotFlowsTasks.stateFromFlow(backgroundScope, flowOf(1, 2, 3), initial = 0)
        runCurrent()
        assertEquals(3, state.value)
        // пустой источник → остаётся начальное значение
        val empty = HotFlowsTasks.stateFromFlow(backgroundScope, flowOf<Int>(), initial = 42)
        runCurrent()
        assertEquals(42, empty.value)
    }

    @Test fun `С14 shareFromFlow`() = runTest {
        val shared = HotFlowsTasks.shareFromFlow(backgroundScope, flowOf(1, 2, 3), replay = 3)
        runCurrent()
        assertEquals(listOf(1, 2, 3), shared.replayCache)
        // replay=2 хранит только последние два
        val two = HotFlowsTasks.shareFromFlow(backgroundScope, flowOf(1, 2, 3), replay = 2)
        runCurrent()
        assertEquals(listOf(2, 3), two.replayCache)
    }

    @Test fun `С15 sumState обновляется`() = runTest {
        val a = MutableStateFlow(1)
        val b = MutableStateFlow(2)
        val sum = HotFlowsTasks.sumState(backgroundScope, a, b)
        runCurrent()
        assertEquals(3, sum.value)
        a.value = 10
        runCurrent()
        assertEquals(12, sum.value)
        b.value = 100          // реагирует и на второй источник
        runCurrent()
        assertEquals(110, sum.value)
    }

    // ── Сложные ──

    @Test fun `СЛ16 mirror дедуплицирует StateFlow`() = runTest {
        val state = MutableStateFlow(0)
        val sink = mutableListOf<Int>()
        HotFlowsTasks.mirror(backgroundScope, state, sink)
        runCurrent()
        state.value = 0; runCurrent()   // то же значение — не эмитит
        state.value = 1; runCurrent()
        state.value = 1; runCurrent()   // повтор — не эмитит
        state.value = 2; runCurrent()
        assertEquals(listOf(0, 1, 2), sink)
    }

    @Test fun `СЛ17 runningTotalState`() = runTest {
        val total = HotFlowsTasks.runningTotalState(backgroundScope, flowOf(1, 2, 3))
        runCurrent()
        assertEquals(6, total.value)
        // другой вход, включая отрицательные — итог = сумма
        val total2 = HotFlowsTasks.runningTotalState(backgroundScope, flowOf(10, -4, 1))
        runCurrent()
        assertEquals(7, total2.value)
    }

    @Test fun `СЛ18 takeN из SharedFlow`() = runTest {
        val sf = MutableSharedFlow<Int>(replay = 5)
        listOf(1, 2, 3, 4, 5).forEach { sf.tryEmit(it) }
        assertEquals(listOf(1, 2, 3), HotFlowsTasks.takeN(sf, 3))
        assertEquals(listOf(1), HotFlowsTasks.takeN(sf, 1))          // граница
        assertEquals(listOf(1, 2, 3, 4, 5), HotFlowsTasks.takeN(sf, 5)) // все
    }

    @Test fun `СЛ19 mapState`() = runTest {
        val source = MutableStateFlow(2)
        val mapped = HotFlowsTasks.mapState(backgroundScope, source) { it * 10 }
        runCurrent()
        assertEquals(20, mapped.value)
        source.value = 5
        runCurrent()
        assertEquals(50, mapped.value)
        // другая функция (смена типа) — не хардкод умножения
        val labeled = HotFlowsTasks.mapState(backgroundScope, source) { "v=$it" }
        runCurrent()
        assertEquals("v=5", labeled.value)
    }

    @Test fun `СЛ20 whileSubscribedShare ленив`() = runTest {
        val started = AtomicBoolean(false)
        val source = flow {
            started.set(true)
            emit(1); emit(2); emit(3)
            awaitCancellation()
        }
        val shared = HotFlowsTasks.whileSubscribedShare(backgroundScope, source, replay = 3)
        runCurrent()
        assertFalse(started.get(), "без подписчиков апстрим не должен стартовать")

        val sink = mutableListOf<Int>()
        backgroundScope.launch { shared.collect { sink.add(it) } }
        runCurrent()
        assertTrue(started.get(), "с первым подписчиком апстрим включается")
        assertEquals(listOf(1, 2, 3), sink)
    }
}
