package handbook.compose.composition

import handbook.compose.composition.solutions.CompositionSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 5 «Slot table и композиция». Проверяют позиционную мемоизацию vs `key()` на
 * детерминированной модели [Item]: идентичность по позиции/id, переиспользование и потерю состояния
 * при reorder/insert, диф и карту переноса слотов, movableContent и области ключей.
 *
 * Чтобы гонять против эталона — замени `SUT` на [CompositionSolutions].
 */
private typealias SUT = CompositionTasks

class CompositionTest {

    // Старая композиция: a,b,c со слот-состоянием 1,2,3. Новая: c,a,b (c уехал в начало).
    private val oldItems = listOf(Item("a", 1), Item("b", 2), Item("c", 3))
    private val oldIds = listOf("a", "b", "c")
    private val reordered = listOf("c", "a", "b")

    // ── Лёгкие ──

    @Test fun `Л1 positionalIdentities — идентичность это позиция`() {
        assertEquals(listOf(0, 1, 2), SUT.positionalIdentities(oldIds))
    }

    @Test fun `Л2 keyedIdentities — идентичность это id`() {
        assertEquals(listOf("a", "b", "c"), SUT.keyedIdentities(oldIds))
    }

    @Test fun `Л3 stateByPosition — слот на позиции переиспользуется`() {
        assertEquals(listOf<Any?>(1, 2, 0), SUT.stateByPosition(listOf(Item("a", 1), Item("b", 2)), newSize = 3, fresh = 0))
    }

    @Test fun `Л4 reusedCount`() {
        assertEquals(2, SUT.reusedCount(oldSize = 3, newSize = 2))
        assertEquals(2, SUT.reusedCount(oldSize = 2, newSize = 5))
    }

    @Test fun `Л5 keyedReused`() {
        assertEquals(setOf("a", "c"), SUT.keyedReused(oldIds, listOf("c", "a", "d")))
    }

    @Test fun `Л6 keyedAdded`() {
        assertEquals(setOf("d"), SUT.keyedAdded(oldIds, listOf("c", "a", "d")))
    }

    @Test fun `Л7 keyedRemoved`() {
        assertEquals(setOf("b"), SUT.keyedRemoved(oldIds, listOf("c", "a", "d")))
    }

    @Test fun `Л8 hasDuplicateKeys`() {
        assertTrue(SUT.hasDuplicateKeys(listOf("a", "b", "a")))
        assertFalse(SUT.hasDuplicateKeys(oldIds))
    }

    // ── Средние ──

    @Test fun `С9 statePositionalAfterReorder — элемент показывает чужое состояние`() {
        assertEquals(
            listOf("c" to 1, "a" to 2, "b" to 3),
            SUT.statePositionalAfterReorder(oldItems, reordered, fresh = 0),
        )
    }

    @Test fun `С10 stateKeyedAfterReorder — состояние следует за id`() {
        assertEquals(
            listOf("c" to 3, "a" to 1, "b" to 2),
            SUT.stateKeyedAfterReorder(oldItems, reordered, fresh = 0),
        )
    }

    @Test fun `С11 mismatchedPositions`() {
        assertEquals(listOf(0, 1, 2), SUT.mismatchedPositions(oldIds, reordered))
        assertEquals(listOf(2), SUT.mismatchedPositions(oldIds, listOf("a", "b", "x")))
    }

    @Test fun `С12 firstDuplicateKey`() {
        assertEquals("a", SUT.firstDuplicateKey(listOf("a", "b", "a", "c")))
        assertNull(SUT.firstDuplicateKey(oldIds))
    }

    @Test fun `С13 stateLostOnReorder — сменившие позицию`() {
        assertEquals(setOf("a", "b", "c"), SUT.stateLostOnReorder(oldIds, reordered))
        assertEquals(emptySet<String>(), SUT.stateLostOnReorder(oldIds, listOf("a", "b", "c")))
    }

    @Test fun `С14 affectedByHeadInsert — ключи спасают хвост`() {
        val withInsert = listOf("x", "a", "b", "c")
        assertEquals(1, SUT.affectedByHeadInsert(oldIds, withInsert, keyed = true))
        assertEquals(4, SUT.affectedByHeadInsert(oldIds, withInsert, keyed = false))
    }

    @Test fun `С15 moveTargets — что перенесётся, а не пересоздастся`() {
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 0), SUT.moveTargets(oldIds, reordered))
    }

    // ── Сложные ──

    @Test fun `СЛ16 diff — переиспользованные, добавленные, удалённые`() {
        assertEquals(
            Triple(setOf("a", "c"), setOf("d"), setOf("b")),
            SUT.diff(oldIds, listOf("c", "a", "d")),
        )
    }

    @Test fun `СЛ17 reuseSlots — откуда переиспользовать слот`() {
        assertEquals(listOf(2, 0, -1), SUT.reuseSlots(oldIds, listOf("c", "a", "d")))
    }

    @Test fun `СЛ18 netStateSurvivors — сколько состояний уцелело`() {
        assertEquals(3, SUT.netStateSurvivors(oldItems, reordered, keyed = true))
        assertEquals(0, SUT.netStateSurvivors(oldItems, reordered, keyed = false))
        // хвостовая замена: с ключами и без результат совпадает
        assertEquals(2, SUT.netStateSurvivors(oldItems, listOf("a", "b", "x"), keyed = true))
        assertEquals(2, SUT.netStateSurvivors(oldItems, listOf("a", "b", "x"), keyed = false))
    }

    @Test fun `СЛ19 movableContentState — перенос сохраняет состояние`() {
        assertEquals("S", SUT.movableContentState(usesMovable = true, preservedState = "S", fresh = null))
        assertNull(SUT.movableContentState(usesMovable = false, preservedState = "S", fresh = null))
    }

    @Test fun `СЛ20 scopedIdentities — одинаковый ключ в разных группах различается`() {
        assertEquals(
            listOf(0 to "a", 0 to "b", 1 to "a", 1 to "c"),
            SUT.scopedIdentities(listOf(listOf("a", "b"), listOf("a", "c"))),
        )
    }
}
