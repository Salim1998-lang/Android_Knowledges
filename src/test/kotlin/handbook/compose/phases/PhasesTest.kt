package handbook.compose.phases

import handbook.compose.phases.Phase.COMPOSITION
import handbook.compose.phases.Phase.DRAWING
import handbook.compose.phases.Phase.LAYOUT
import handbook.compose.phases.ReadingModifier.DRAW_BEHIND
import handbook.compose.phases.ReadingModifier.GRAPHICS_LAYER
import handbook.compose.phases.ReadingModifier.OFFSET_LAMBDA
import handbook.compose.phases.ReadingModifier.OFFSET_STATIC
import handbook.compose.phases.solutions.PhasesSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 8 «Фазы кадра». Проверяют порядок фаз composition → layout → drawing, инвалидацию «от
 * фазы чтения и далее», пропуск ранних фаз и выигрыш отложенного чтения (offset-lambda, graphicsLayer)
 * на детерминированной модели [Phase] / [ReadingModifier].
 *
 * Чтобы гонять против эталона — замени `SUT` на [PhasesSolutions].
 */
private typealias SUT = PhasesTasks

class PhasesTest {

    // цены фаз для стоимостных задач: композиция дороже всего, отрисовка дешевле
    private val costs = mapOf(COMPOSITION to 5, LAYOUT to 3, DRAWING to 2)

    // ── Лёгкие ──

    @Test fun `Л1 order — три фазы по порядку`() {
        assertEquals(listOf(COMPOSITION, LAYOUT, DRAWING), SUT.order())
    }

    @Test fun `Л2 indexOf`() {
        assertEquals(0, SUT.indexOf(COMPOSITION))
        assertEquals(1, SUT.indexOf(LAYOUT))
        assertEquals(2, SUT.indexOf(DRAWING))
    }

    @Test fun `Л3 isBefore`() {
        assertTrue(SUT.isBefore(COMPOSITION, DRAWING))
        assertFalse(SUT.isBefore(DRAWING, LAYOUT))
        assertFalse(SUT.isBefore(LAYOUT, LAYOUT))
    }

    @Test fun `Л4 next`() {
        assertEquals(LAYOUT, SUT.next(COMPOSITION))
        assertEquals(DRAWING, SUT.next(LAYOUT))
        assertEquals(null, SUT.next(DRAWING))
    }

    @Test fun `Л5 role`() {
        assertEquals("что показывать", SUT.role(COMPOSITION))
        assertEquals("где и какого размера", SUT.role(LAYOUT))
        assertEquals("как нарисовать", SUT.role(DRAWING))
    }

    @Test fun `Л6 phasesFrom`() {
        assertEquals(listOf(LAYOUT, DRAWING), SUT.phasesFrom(LAYOUT))
        assertEquals(listOf(DRAWING), SUT.phasesFrom(DRAWING))
    }

    @Test fun `Л7 phasesBefore`() {
        assertEquals(listOf(COMPOSITION), SUT.phasesBefore(LAYOUT))
        assertEquals(emptyList<Phase>(), SUT.phasesBefore(COMPOSITION))
    }

    @Test fun `Л8 readsIn`() {
        assertEquals(COMPOSITION, SUT.readsIn(OFFSET_STATIC))
        assertEquals(LAYOUT, SUT.readsIn(OFFSET_LAMBDA))
        assertEquals(DRAWING, SUT.readsIn(GRAPHICS_LAYER))
    }

    // ── Средние ──

    @Test fun `С9 earliestRead — самая ранняя фаза`() {
        assertEquals(LAYOUT, SUT.earliestRead(listOf(DRAWING, LAYOUT)))
        assertEquals(COMPOSITION, SUT.earliestRead(listOf(COMPOSITION, DRAWING)))
    }

    @Test fun `С10 invalidatedPhases — от чтения и далее`() {
        assertEquals(listOf(LAYOUT, DRAWING), SUT.invalidatedPhases(listOf(LAYOUT)))
        assertEquals(listOf(COMPOSITION, LAYOUT, DRAWING), SUT.invalidatedPhases(listOf(COMPOSITION)))
        assertEquals(emptyList<Phase>(), SUT.invalidatedPhases(emptyList()))
    }

    @Test fun `С11 skippedPhases — ранние пропускаются`() {
        assertEquals(listOf(COMPOSITION), SUT.skippedPhases(listOf(LAYOUT)))
        assertEquals(listOf(COMPOSITION, LAYOUT), SUT.skippedPhases(listOf(DRAWING)))
        assertEquals(listOf(COMPOSITION, LAYOUT, DRAWING), SUT.skippedPhases(emptyList()))
    }

    @Test fun `С12 deferSaves — экономия отложенного чтения`() {
        assertEquals(emptyList<Phase>(), SUT.deferSaves(OFFSET_STATIC))
        assertEquals(listOf(COMPOSITION), SUT.deferSaves(OFFSET_LAMBDA))
        assertEquals(listOf(COMPOSITION, LAYOUT), SUT.deferSaves(GRAPHICS_LAYER))
    }

    @Test fun `С13 cheaperUnderChange — кто читает позже`() {
        assertEquals(GRAPHICS_LAYER, SUT.cheaperUnderChange(OFFSET_STATIC, GRAPHICS_LAYER))
        assertEquals(OFFSET_LAMBDA, SUT.cheaperUnderChange(OFFSET_LAMBDA, OFFSET_STATIC))
        // равная фаза (обе в drawing) → возвращаем первый
        assertEquals(GRAPHICS_LAYER, SUT.cheaperUnderChange(GRAPHICS_LAYER, DRAW_BEHIND))
    }

    @Test fun `С14 changeCost — цена изменения`() {
        assertEquals(5, SUT.changeCost(listOf(LAYOUT), costs))          // 3 + 2
        assertEquals(10, SUT.changeCost(listOf(COMPOSITION), costs))    // 5 + 3 + 2
        assertEquals(0, SUT.changeCost(emptyList(), costs))
    }

    @Test fun `С15 savedCost — сколько сэкономлено`() {
        assertEquals(5, SUT.savedCost(listOf(LAYOUT), costs))           // composition
        assertEquals(8, SUT.savedCost(listOf(DRAWING), costs))          // composition + layout
        assertEquals(10, SUT.savedCost(emptyList(), costs))             // всё пропущено
    }

    // ── Сложные ──

    @Test fun `СЛ16 runFrame — самая ранняя среди изменившихся`() {
        val reads = mapOf("a" to listOf(COMPOSITION), "b" to listOf(DRAWING))
        assertEquals(listOf(DRAWING), SUT.runFrame(reads, setOf("b")))
        assertEquals(listOf(COMPOSITION, LAYOUT, DRAWING), SUT.runFrame(reads, setOf("a", "b")))
        assertEquals(emptyList<Phase>(), SUT.runFrame(reads, emptySet()))
        assertEquals(emptyList<Phase>(), SUT.runFrame(reads, setOf("unread")))
    }

    @Test fun `СЛ17 animationCost — draw дешевле composition`() {
        assertEquals(30, SUT.animationCost(COMPOSITION, costs, 3))      // 3 * 10
        assertEquals(6, SUT.animationCost(DRAWING, costs, 3))           // 3 * 2
    }

    @Test fun `СЛ18 bestModifier — кто читает позже`() {
        val options = mapOf("static" to OFFSET_STATIC, "lambda" to OFFSET_LAMBDA, "gl" to GRAPHICS_LAYER)
        assertEquals("gl", SUT.bestModifier(options))
        // при равной фазе — лексикографически меньшее имя
        assertEquals("behind", SUT.bestModifier(mapOf("gl" to GRAPHICS_LAYER, "behind" to DRAW_BEHIND)))
    }

    @Test fun `СЛ19 compareStrategies — composition против drawing`() {
        assertEquals(100 to 20, SUT.compareStrategies(10, costs))       // 10*10, 10*2
    }

    @Test fun `СЛ20 timeline — фазы по кадрам`() {
        val reads = mapOf("a" to listOf(COMPOSITION), "b" to listOf(DRAWING))
        assertEquals(
            listOf(
                listOf(COMPOSITION, LAYOUT, DRAWING),
                listOf(DRAWING),
                emptyList(),
            ),
            SUT.timeline(reads, listOf(setOf("a"), setOf("b"), emptySet())),
        )
    }
}
