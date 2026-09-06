package handbook.compose.effects

import handbook.compose.effects.EffectEvent.DISPOSE
import handbook.compose.effects.EffectEvent.START
import handbook.compose.effects.solutions.EffectsSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 4 «Эффекты». Проверяют жизненный цикл эффектов на детерминированной модели [EffectEvent]:
 * перезапуск по ключу, вход/выход из композиции, SideEffect vs LaunchedEffect(Unit),
 * rememberUpdatedState, rememberCoroutineScope и порядок dispose-before-start.
 *
 * Чтобы гонять против эталона — замени `SUT` на [EffectsSolutions].
 */
private typealias SUT = EffectsTasks

class EffectsTest {

    // ── Лёгкие ──

    @Test fun `Л1 restartsOnKeyChange`() {
        assertFalse(SUT.restartsOnKeyChange(listOf(1), listOf(1)))
        assertTrue(SUT.restartsOnKeyChange(listOf(1), listOf(2)))
    }

    @Test fun `Л2 sideEffectRunCount — каждая композиция`() {
        assertEquals(3, SUT.sideEffectRunCount(3))
    }

    @Test fun `Л3 constantKeyLaunches — один запуск`() {
        assertEquals(1, SUT.constantKeyLaunches(5))
        assertEquals(0, SUT.constantKeyLaunches(0))
    }

    @Test fun `Л4 disposableTransition — dispose перед start при смене ключа`() {
        assertEquals(emptyList<EffectEvent>(), SUT.disposableTransition(listOf(1), listOf(1)))
        assertEquals(listOf(DISPOSE, START), SUT.disposableTransition(listOf(1), listOf(2)))
    }

    @Test fun `Л5 enterEvents`() {
        assertEquals(listOf(START), SUT.enterEvents())
    }

    @Test fun `Л6 leaveEvents`() {
        assertEquals(listOf(DISPOSE), SUT.leaveEvents())
    }

    @Test fun `Л7 scopeActive`() {
        assertTrue(SUT.scopeActive(inComposition = true))
        assertFalse(SUT.scopeActive(inComposition = false))
    }

    @Test fun `Л8 updatedStateValue — последнее значение`() {
        assertEquals(30, SUT.updatedStateValue(listOf(10, 20, 30)))
        assertEquals(null, SUT.updatedStateValue(emptyList()))
    }

    // ── Средние ──

    private val frames = listOf<List<Any?>?>(listOf(1), listOf(1), listOf(2), null, listOf(2))

    @Test fun `С9 disposableLifecycle — по кадрам`() {
        assertEquals(listOf(START, DISPOSE, START, DISPOSE, START), SUT.disposableLifecycle(frames))
    }

    @Test fun `С10 launchCount`() {
        assertEquals(3, SUT.launchCount(frames))
    }

    @Test fun `С11 disposeCount`() {
        assertEquals(2, SUT.disposeCount(frames))
    }

    @Test fun `С12 activeAfter`() {
        assertTrue(SUT.activeAfter(frames))
        assertFalse(SUT.activeAfter(listOf(listOf(1), null)))
    }

    @Test fun `С13 sideEffectVsLaunched`() {
        assertEquals(3 to 1, SUT.sideEffectVsLaunched(3))
        assertEquals(0 to 0, SUT.sideEffectVsLaunched(0))
    }

    @Test fun `С14 effectRestartsOnValueChange — updatedState рвёт связь`() {
        assertFalse(SUT.effectRestartsOnValueChange(useUpdatedState = true, valueChanged = true))
        assertTrue(SUT.effectRestartsOnValueChange(useUpdatedState = false, valueChanged = true))
        assertFalse(SUT.effectRestartsOnValueChange(useUpdatedState = false, valueChanged = false))
    }

    @Test fun `С15 keyedRunCount — первый плюс смены ключа`() {
        assertEquals(2, SUT.keyedRunCount(listOf(listOf(1), listOf(1), listOf(2))))
        assertEquals(0, SUT.keyedRunCount(emptyList()))
    }

    // ── Сложные ──

    @Test fun `СЛ16 launchedEffectLifecycle — финальная отмена при уничтожении`() {
        assertEquals(
            listOf(START, DISPOSE, START, DISPOSE, START, DISPOSE),
            SUT.launchedEffectLifecycle(frames, disposeAtEnd = true),
        )
        assertEquals(
            listOf(START, DISPOSE, START, DISPOSE, START),
            SUT.launchedEffectLifecycle(frames, disposeAtEnd = false),
        )
    }

    @Test fun `СЛ17 rememberUpdatedStateScenario — 0 перезапусков против шторма`() {
        val values = listOf(10, 20, 20, 30)
        assertEquals(0 to 30, SUT.rememberUpdatedStateScenario(useUpdatedState = true, valueSequence = values))
        assertEquals(2 to 30, SUT.rememberUpdatedStateScenario(useUpdatedState = false, valueSequence = values))
    }

    @Test fun `СЛ18 applyChanges — сначала все dispose, потом все start`() {
        val effects = listOf(
            Triple("a", listOf<Any?>(1), listOf<Any?>(2)), // изменился
            Triple("b", listOf<Any?>(9), listOf<Any?>(9)), // не изменился
            Triple("c", listOf<Any?>(3), listOf<Any?>(4)), // изменился
        )
        assertEquals(
            listOf("a" to DISPOSE, "c" to DISPOSE, "a" to START, "c" to START),
            SUT.applyChanges(effects),
        )
    }

    @Test fun `СЛ19 scopeJobsAfter — всё умирает с композицией`() {
        assertEquals(2, SUT.scopeJobsAfter(launched = 3, completed = 1, leftComposition = false))
        assertEquals(0, SUT.scopeJobsAfter(launched = 3, completed = 1, leftComposition = true))
    }

    @Test fun `СЛ20 disposableTrace — трейс с индексами кадров`() {
        assertEquals(
            listOf(0 to START, 2 to DISPOSE, 2 to START),
            SUT.disposableTrace(listOf(listOf(1), listOf(1), listOf(2))),
        )
    }
}
