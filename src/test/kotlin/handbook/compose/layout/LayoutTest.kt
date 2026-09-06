package handbook.compose.layout

import handbook.compose.layout.solutions.LayoutSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 6 «Измерение и раскладка». Проверяют работу с [Constraints] и measure/place на
 * детерминированной модели: coerce/fill/size/requiredSize, padding, размеры Column/Row/Box и позиции,
 * single-pass, intrinsics и weight.
 *
 * Чтобы гонять против эталона — замени `SUT` на [LayoutSolutions].
 */
private typealias SUT = LayoutTasks

class LayoutTest {

    private val loose = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

    // ── Лёгкие ──

    @Test fun `Л1 coerceWidth`() {
        assertEquals(100, SUT.coerceWidth(desired = 120, c = loose))
        assertEquals(50, SUT.coerceWidth(desired = 30, c = Constraints(50, 100, 0, 100)))
    }

    @Test fun `Л2 coerceHeight`() {
        assertEquals(100, SUT.coerceHeight(desired = 200, c = loose))
    }

    @Test fun `Л3 fillMaxWidth`() {
        assertEquals(100, SUT.fillMaxWidth(loose))
    }

    @Test fun `Л4 sizeWidth — уважает constraints`() {
        assertEquals(80, SUT.sizeWidth(size = 80, c = loose))
        assertEquals(100, SUT.sizeWidth(size = 120, c = loose)) // зажат в max
    }

    @Test fun `Л5 requiredSizeWidth — игнорирует constraints`() {
        assertEquals(120, SUT.requiredSizeWidth(120))
    }

    @Test fun `Л6 isTight`() {
        assertTrue(SUT.isTight(Constraints(100, 100, 50, 50)))
        assertFalse(SUT.isTight(loose))
    }

    @Test fun `Л7 isBounded`() {
        assertTrue(SUT.isBounded(loose))
        assertFalse(SUT.isBounded(Constraints(0, 100, 0, INFINITY)))
    }

    @Test fun `Л8 padContentConstraints — паддинг съедает место`() {
        assertEquals(Constraints(0, 80, 0, 80), SUT.padContentConstraints(loose, padding = 10))
        assertEquals(
            Constraints(30, 80, 20, 80),
            SUT.padContentConstraints(Constraints(50, 100, 40, 100), padding = 10),
        )
    }

    // ── Средние ──

    private val children = listOf(Size(30, 10), Size(50, 20), Size(20, 5))

    @Test fun `С9 columnSize — max ширина, sum высота`() {
        assertEquals(Size(50, 35), SUT.columnSize(children, loose))
        assertEquals(Size(0, 0), SUT.columnSize(emptyList(), loose))
    }

    @Test fun `С10 rowSize — sum ширина, max высота`() {
        assertEquals(Size(100, 20), SUT.rowSize(children, loose))
    }

    @Test fun `С11 boxSize — max по обеим осям`() {
        assertEquals(Size(50, 20), SUT.boxSize(children, loose))
    }

    @Test fun `С12 columnPlacements — стек сверху вниз`() {
        assertEquals(listOf(0, 10, 30), SUT.columnPlacements(children))
    }

    @Test fun `С13 rowPlacements — слева направо`() {
        assertEquals(listOf(0, 30, 80), SUT.rowPlacements(children))
    }

    @Test fun `С14 measureCount — один проход`() {
        assertEquals(3, SUT.measureCount(3))
    }

    @Test fun `С15 childConstraints — fill даёт tight ширину`() {
        assertEquals(Constraints(100, 100, 0, 100), SUT.childConstraints(loose, fill = true))
        assertEquals(Constraints(0, 100, 0, 100), SUT.childConstraints(loose, fill = false))
    }

    // ── Сложные ──

    @Test fun `СЛ16 layoutColumn — размер и позиции вместе`() {
        val col = listOf(Size(30, 10), Size(50, 20))
        assertEquals(Size(50, 30) to listOf(0, 10), SUT.layoutColumn(col, loose))
    }

    @Test fun `СЛ17 intrinsicRowHeight — высота по самому высокому минимуму`() {
        assertEquals(30, SUT.intrinsicRowHeight(listOf(10, 30, 20)))
        assertEquals(0, SUT.intrinsicRowHeight(emptyList()))
    }

    @Test fun `СЛ18 measurePassesWithIntrinsics — доп проход`() {
        assertEquals(3, SUT.measurePassesWithIntrinsics(3, usesIntrinsics = false))
        assertEquals(6, SUT.measurePassesWithIntrinsics(3, usesIntrinsics = true))
    }

    @Test fun `СЛ19 weightWidths — пропорционально с остатком`() {
        assertEquals(listOf(50, 50), SUT.weightWidths(100, listOf(1, 1)))
        assertEquals(listOf(25, 75), SUT.weightWidths(100, listOf(1, 3)))
        assertEquals(listOf(4, 3, 3), SUT.weightWidths(10, listOf(1, 1, 1))) // остаток 1 → первому
        assertEquals(emptyList<Int>(), SUT.weightWidths(100, emptyList()))
    }

    @Test fun `СЛ20 centerInBox`() {
        assertEquals(30 to 20, SUT.centerInBox(child = Size(40, 20), container = Size(100, 60)))
    }
}
