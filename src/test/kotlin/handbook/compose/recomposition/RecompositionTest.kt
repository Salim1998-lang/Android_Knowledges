package handbook.compose.recomposition

import handbook.compose.recomposition.solutions.RecompositionSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 1 «Рекомпозиция». Проверяют механику Compose-рантайма на детерминированной модели
 * дерева [Node]: read tracking, fine-grained инвалидацию, поднятие к restartable-scope, skipping по
 * стабильности параметров, derivedStateOf и горячие точки рекомпозиции.
 *
 * Чтобы гонять против эталона — замени `SUT` на [RecompositionSolutions].
 */
private typealias SUT = RecompositionTasks

class RecompositionTest {

    // Пример дерева:
    //   app(restartable) ─ reads: theme
    //     ├─ header(restartable, skippable) ─ reads: title
    //     └─ list(restartable) ─ reads: items
    //          └─ row(restartable, skippable) ─ reads: selected
    private val tree = listOf(
        Node(id = "app", reads = setOf("theme"), children = listOf("header", "list")),
        Node(id = "header", reads = setOf("title")),
        Node(id = "list", reads = setOf("items"), children = listOf("row")),
        Node(id = "row", reads = setOf("selected")),
    )

    // ── Лёгкие ──

    @Test fun `Л1 readersOf — кто читает состояние`() {
        assertEquals(setOf("list"), SUT.readersOf(tree, "items"))
        assertEquals(emptySet<String>(), SUT.readersOf(tree, "nope"))
    }

    @Test fun `Л2 writeInvalidates — только при неравном значении`() {
        assertTrue(SUT.writeInvalidates(old = 1, new = 2))
        assertFalse(SUT.writeInvalidates(old = 1, new = 1))
        assertFalse(SUT.writeInvalidates(old = "a", new = "a"))
        assertTrue(SUT.writeInvalidates(old = null, new = "a"))
    }

    @Test fun `Л3 hasReaders`() {
        assertTrue(SUT.hasReaders(tree, "theme"))
        assertFalse(SUT.hasReaders(tree, "ghost"))
    }

    @Test fun `Л4 unreadStates — состояния без читателей`() {
        val all = setOf("theme", "title", "items", "selected", "dead1", "dead2")
        assertEquals(setOf("dead1", "dead2"), SUT.unreadStates(tree, all))
    }

    @Test fun `Л5 invalidatedScopes — объединение читателей изменённых состояний`() {
        assertEquals(setOf("app", "list"), SUT.invalidatedScopes(tree, setOf("theme", "items")))
        assertEquals(emptySet<String>(), SUT.invalidatedScopes(tree, setOf("nope")))
    }

    @Test fun `Л6 willSkip`() {
        assertTrue(SUT.willSkip(skippable = true, paramsEqual = true))
        assertFalse(SUT.willSkip(skippable = true, paramsEqual = false))
        assertFalse(SUT.willSkip(skippable = false, paramsEqual = true))
    }

    @Test fun `Л7 parentOf`() {
        assertEquals("list", SUT.parentOf(tree, "row"))
        assertEquals("app", SUT.parentOf(tree, "header"))
        assertNull(SUT.parentOf(tree, "app"))
    }

    @Test fun `Л8 pathToRoot`() {
        assertEquals(listOf("row", "list", "app"), SUT.pathToRoot(tree, "row"))
        assertEquals(listOf("app"), SUT.pathToRoot(tree, "app"))
    }

    // ── Средние ──

    @Test fun `С9 restartScope — inline-обёртка поднимает scope к предку`() {
        // list — inline (не restartable): его инвалидация всплывает к app.
        val t = listOf(
            Node(id = "app", children = listOf("list")),
            Node(id = "list", restartable = false, children = listOf("row")),
            Node(id = "row"),
        )
        assertEquals("app", SUT.restartScope(t, "list"))
        assertEquals("row", SUT.restartScope(t, "row")) // сам restartable
        assertEquals("app", SUT.restartScope(t, "app"))
    }

    @Test fun `С10 recomposeTargets — инвалидация поднята к restartable`() {
        val t = listOf(
            Node(id = "app", children = listOf("list")),
            Node(id = "list", restartable = false, reads = setOf("items"), children = listOf("row")),
            Node(id = "row", reads = setOf("selected")),
        )
        // items читает list (inline) → цель поднимается до app
        assertEquals(setOf("app"), SUT.recomposeTargets(t, setOf("items")))
        assertEquals(setOf("row"), SUT.recomposeTargets(t, setOf("selected")))
    }

    @Test fun `С11 recompose — skipping вырезает стабильные неизменённые ветки`() {
        // app рекомпозируется; header skippable и параметры не менялись → пропущен вместе с веткой,
        // list рекомпозируется (в changedParams), row skippable но его параметры изменились.
        val executed = SUT.recompose(tree, root = "app", changedParams = setOf("list", "row"))
        assertEquals(setOf("app", "list", "row"), executed)
        assertFalse("header" in executed)
    }

    @Test fun `С11 recompose — неskippable ребёнок выполняется всегда`() {
        val t = listOf(
            Node(id = "app", children = listOf("a", "b")),
            Node(id = "a", skippable = false),
            Node(id = "b", skippable = true),
        )
        // a неskippable → выполняется даже без изменения параметров; b пропущен
        assertEquals(setOf("app", "a"), SUT.recompose(t, root = "app", changedParams = emptySet()))
    }

    @Test fun `С12 skippedNodes`() {
        assertEquals(setOf("header"), SUT.skippedNodes(tree, root = "app", changedParams = setOf("list", "row")))
    }

    @Test fun `С13 skippableFromParams — один нестабильный ломает skippable`() {
        assertTrue(SUT.skippableFromParams(listOf(true, true)))
        assertTrue(SUT.skippableFromParams(emptyList()))
        assertFalse(SUT.skippableFromParams(listOf(true, false, true)))
    }

    @Test fun `С14 readDiff — условные чтения меняют read-set`() {
        val (added, removed) = SUT.readDiff(oldReads = setOf("flag", "count"), newReads = setOf("flag"))
        assertEquals(emptySet<String>(), added)
        assertEquals(setOf("count"), removed)
    }

    @Test fun `С15 recomposeCount — сумма по батчам`() {
        val batches = listOf(setOf("theme"), setOf("items", "selected"))
        // батч1: {app}=1; батч2: {list, row}=2 → всего 3
        assertEquals(3, SUT.recomposeCount(tree, batches))
    }

    // ── Сложные ──

    @Test fun `СЛ16 recomposeFromState — инвалидация вверх, рекомпозиция вниз`() {
        // theme читает app → цель app; распространение вниз: list в changedParams, header пропущен.
        val executed = SUT.recomposeFromState(
            tree,
            changed = setOf("theme"),
            changedParams = setOf("list", "row"),
        )
        assertEquals(setOf("app", "list", "row"), executed)
    }

    @Test fun `СЛ17 skippedVsRecomposed`() {
        val (recomposed, skipped) = SUT.skippedVsRecomposed(tree, root = "app", changedParams = setOf("list", "row"))
        assertEquals(3, recomposed) // app, list, row
        assertEquals(1, skipped)    // header
    }

    @Test fun `СЛ18 perNodeRecompositionCounts`() {
        val batches = listOf(setOf("theme"), setOf("theme"), setOf("items"), setOf("selected"))
        val counts = SUT.perNodeRecompositionCounts(tree, batches)
        assertEquals(mapOf("app" to 2, "list" to 1, "row" to 1), counts)
    }

    @Test fun `СЛ19 derivedReaderInvalidations — результат не менялся, инвалидаций нет`() {
        assertEquals(
            listOf(3, 0, 3, 0),
            SUT.derivedReaderInvalidations(readerCount = 3, resultChanged = listOf(true, false, true, false)),
        )
    }

    @Test fun `СЛ20 recompositionHotspots — частые инвалидации выше порога`() {
        val batches = listOf(setOf("theme"), setOf("theme"), setOf("theme"), setOf("items"))
        // app=3, list=1, порог 3 → только app
        assertEquals(setOf("app"), SUT.recompositionHotspots(tree, batches, threshold = 3))
    }
}
