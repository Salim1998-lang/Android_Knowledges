package handbook.compose.layout

/**
 * «Бесконечность» для верхней границы constraints — как `Constraints.Infinity` в Compose. Возникает
 * по оси, вдоль которой контейнер прокручивается (например, `maxHeight` внутри вертикального скролла):
 * дочерний может быть сколь угодно большим, «сколько влезет» не ограничено.
 */
const val INFINITY: Int = Int.MAX_VALUE

/**
 * Ограничения раскладки — учебный аналог `androidx.compose.ui.unit.Constraints`.
 *
 * Родитель передаёт constraints КАЖДОМУ ребёнку, а ребёнок обязан выбрать размер В ИХ ПРЕДЕЛАХ
 * (`min..max` по каждой оси). Это язык, на котором родитель и ребёнок договариваются о размере за
 * ОДИН проход измерения.
 *
 *  • «tight» constraints (`min == max`) — точный размер навязан (`Modifier.size` изнутри такие ставит);
 *  • «loose» constraints (`min == 0`) — можно быть любым до `max` (обычный `wrapContent`);
 *  • неограниченная ось (`max == INFINITY`) — верхней границы нет (скролл).
 */
data class Constraints(
    val minWidth: Int,
    val maxWidth: Int,
    val minHeight: Int,
    val maxHeight: Int,
)

/**
 * Измеренный размер узла — аналог `Placeable`/`IntSize`: результат измерения, который ребёнок
 * возвращает родителю, чтобы тот посчитал свой размер и расставил детей.
 */
data class Size(val width: Int, val height: Int)
