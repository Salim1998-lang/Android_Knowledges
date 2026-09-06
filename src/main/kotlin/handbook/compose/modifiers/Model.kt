package handbook.compose.modifiers

/**
 * Учебный элемент цепочки модификаторов — один «кирпичик» `Modifier` (аналог `Modifier.Element` /
 * `ModifierNodeElement`).
 *
 * В Compose `Modifier` — это **упорядоченный неизменяемый список** элементов: `Modifier.a().b().c()`.
 * Порядок критичен: он задаёт, как constraints идут ВНИЗ и как отрисовка/размер оборачиваются
 * СНАРУЖИ внутрь. Мы записываем цепочку как `List<Element>` (слева направо = как написано: первый —
 * самый ВНЕШНИЙ), и детерминированно моделируем свёртку, влияние порядка на размер и переиспользование
 * узлов ([Modifier.Node]).
 *
 * @property name короткое имя типа элемента (для свёртки и сравнения типов узлов).
 */
sealed interface Element {
    val name: String

    /** `Modifier.padding(amount)` — отступ вокруг контента по всем сторонам. */
    data class Padding(val amount: Int) : Element {
        override val name: String get() = "padding"
    }

    /** `Modifier.size(value)` — задать размер (уважая входящие constraints). */
    data class Size(val value: Int) : Element {
        override val name: String get() = "size"
    }

    /** `Modifier.background(color)` — залить фон. */
    data class Background(val color: String) : Element {
        override val name: String get() = "background"
    }

    /** `Modifier.clickable { }` — обработка кликов (область клика зависит от места в цепочке). */
    object Clickable : Element {
        override val name: String get() = "clickable"
    }

    /** `Modifier.semantics { key = value }` — узел дерева семантики (доступность/тесты). */
    data class Semantics(val key: String, val value: String) : Element {
        override val name: String get() = "semantics"
    }
}
