package handbook.compose.recomposition

/**
 * Учебная модель одного узла дерева композиции = **recomposition scope**.
 *
 * В реальном Compose каждый вызов `@Composable`-функции оставляет в slot table «слот», а
 * перезапускаемая (restartable) функция получает свой **RecomposeScope**. Во время композиции
 * рантайм записывает, к каким `State`-объектам обратился этот scope (read tracking). Когда значение
 * такого `State` меняется (`value = …` на структурно НЕ равное), рантайм инвалидирует ровно те
 * scope'ы, которые его читали, и планирует их пере-выполнение — это и есть **fine-grained
 * recomposition**: перерисовывается не весь экран, а только затронутые куски.
 *
 * Здесь мы моделируем те же понятия детерминированно на чистой JVM — без Compose-рантайма и Android.
 * Идеи 1:1 переносятся на настоящий `androidx.compose.runtime`.
 *
 * @property id стабильный идентификатор узла в дереве (позиционный «адрес» слота).
 * @property reads множество `State`-объектов (их id), которые тело этого scope читает при композиции.
 *   Именно read-set определяет, какая запись в состояние его инвалидирует.
 * @property children id дочерних узлов в порядке вызова (дети — вложенные `@Composable`).
 * @property restartable является ли функция перезапускаемой: только у restartable-функций есть
 *   собственный RecomposeScope, поэтому инвалидация «поднимается» до ближайшего restartable-предка.
 *   (Inline-функции — `Column`, `Row`, `Box` — своего scope не имеют.)
 * @property skippable может ли рантайм ПРОПУСТИТЬ этот узел при рекомпозиции родителя, если все его
 *   параметры стабильны и не изменились. Нестабильные параметры делают функцию неskippable.
 */
data class Node(
    val id: String,
    val reads: Set<String> = emptySet(),
    val children: List<String> = emptyList(),
    val restartable: Boolean = true,
    val skippable: Boolean = true,
)
