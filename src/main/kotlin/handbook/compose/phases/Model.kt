package handbook.compose.phases

/**
 * Учебная модель трёх фаз кадра Compose.
 *
 * Каждый кадр Compose прогоняет composable через **три фазы строго по порядку**:
 * `COMPOSITION` (что показывать) → `LAYOUT` (где и какого размера) → `DRAWING` (как нарисовать).
 * Ключевая механика темы: изменение состояния инвалидирует ту фазу, в которой это состояние
 * ЧИТАЕТСЯ, и все последующие; более ранние фазы **пропускаются**. Значит, чем позже читаешь
 * состояние, тем меньше работы на его изменение — отсюда приём **отложенного чтения** (lambda-версии
 * модификаторов и `graphicsLayer`).
 *
 * Перечисление объявлено в порядке выполнения, поэтому `ordinal` = индекс фазы в кадре.
 */
enum class Phase { COMPOSITION, LAYOUT, DRAWING }

/**
 * Модификатор, который где-то читает состояние. Важно НЕ что он делает, а в какой фазе он это
 * состояние читает: именно эта фаза (и последующие) перезапустится при изменении состояния.
 * Lambda-версии откладывают чтение в более позднюю фазу и тем самым экономят ранние.
 *
 * @property readsIn фаза, в которой модификатор читает состояние.
 */
enum class ReadingModifier(val readsIn: Phase) {
    /** `Modifier.offset(x = state.dp)` — читает состояние в композиции: полный конвейер. */
    OFFSET_STATIC(Phase.COMPOSITION),

    /** `Modifier.offset { IntOffset(state, 0) }` — лямбда откладывает чтение в layout. */
    OFFSET_LAMBDA(Phase.LAYOUT),

    /** `Modifier.graphicsLayer { translationX = state }` — читает в фазе отрисовки. */
    GRAPHICS_LAYER(Phase.DRAWING),

    /** `Modifier.drawBehind { /* читает state */ }` — тоже только фаза отрисовки. */
    DRAW_BEHIND(Phase.DRAWING),
}
