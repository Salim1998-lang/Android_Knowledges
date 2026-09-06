package handbook.compose.layout.solutions

import handbook.compose.layout.Constraints
import handbook.compose.layout.INFINITY
import handbook.compose.layout.Size

/** Эталонные решения темы 6. Подсмотри, если застрял с [handbook.compose.layout.LayoutTasks]. */
object LayoutSolutions {

    // ── Лёгкие ──

    fun coerceWidth(desired: Int, c: Constraints): Int =
        desired.coerceIn(c.minWidth, c.maxWidth)

    fun coerceHeight(desired: Int, c: Constraints): Int =
        desired.coerceIn(c.minHeight, c.maxHeight)

    fun fillMaxWidth(c: Constraints): Int =
        c.maxWidth

    fun sizeWidth(size: Int, c: Constraints): Int =
        size.coerceIn(c.minWidth, c.maxWidth)

    fun requiredSizeWidth(size: Int): Int =
        size

    fun isTight(c: Constraints): Boolean =
        c.minWidth == c.maxWidth && c.minHeight == c.maxHeight

    fun isBounded(c: Constraints): Boolean =
        c.maxWidth < INFINITY && c.maxHeight < INFINITY

    fun padContentConstraints(c: Constraints, padding: Int): Constraints {
        val h = 2 * padding
        val maxW = (c.maxWidth - h).coerceAtLeast(0)
        val maxH = (c.maxHeight - h).coerceAtLeast(0)
        return Constraints(
            minWidth = (c.minWidth - h).coerceIn(0, maxW),
            maxWidth = maxW,
            minHeight = (c.minHeight - h).coerceIn(0, maxH),
            maxHeight = maxH,
        )
    }

    // ── Средние ──

    fun columnSize(children: List<Size>, c: Constraints): Size = Size(
        width = coerceWidth(children.maxOfOrNull { it.width } ?: 0, c),
        height = coerceHeight(children.sumOf { it.height }, c),
    )

    fun rowSize(children: List<Size>, c: Constraints): Size = Size(
        width = coerceWidth(children.sumOf { it.width }, c),
        height = coerceHeight(children.maxOfOrNull { it.height } ?: 0, c),
    )

    fun boxSize(children: List<Size>, c: Constraints): Size = Size(
        width = coerceWidth(children.maxOfOrNull { it.width } ?: 0, c),
        height = coerceHeight(children.maxOfOrNull { it.height } ?: 0, c),
    )

    fun columnPlacements(children: List<Size>): List<Int> {
        var y = 0
        return children.map { val at = y; y += it.height; at }
    }

    fun rowPlacements(children: List<Size>): List<Int> {
        var x = 0
        return children.map { val at = x; x += it.width; at }
    }

    fun measureCount(childCount: Int): Int =
        childCount

    fun childConstraints(parent: Constraints, fill: Boolean): Constraints = Constraints(
        minWidth = if (fill) parent.maxWidth else 0,
        maxWidth = parent.maxWidth,
        minHeight = 0,
        maxHeight = parent.maxHeight,
    )

    // ── Сложные ──

    fun layoutColumn(children: List<Size>, c: Constraints): Pair<Size, List<Int>> =
        columnSize(children, c) to columnPlacements(children)

    fun intrinsicRowHeight(childMinHeights: List<Int>): Int =
        childMinHeights.maxOrNull() ?: 0

    fun measurePassesWithIntrinsics(childCount: Int, usesIntrinsics: Boolean): Int =
        if (usesIntrinsics) childCount * 2 else childCount

    fun weightWidths(total: Int, weights: List<Int>): List<Int> {
        if (weights.isEmpty()) return emptyList()
        val sum = weights.sum()
        val base = weights.map { total * it / sum }.toMutableList()
        var leftover = total - base.sum()
        var i = 0
        while (leftover > 0) {
            base[i % base.size]++
            leftover--
            i++
        }
        return base
    }

    fun centerInBox(child: Size, container: Size): Pair<Int, Int> =
        (container.width - child.width) / 2 to (container.height - child.height) / 2
}
