package handbook.compose.effects

/**
 * Событие жизненного цикла эффекта — учебная модель того, что происходит с `LaunchedEffect` /
 * `DisposableEffect` при (ре)композиции.
 *
 *  • [START] — тело эффекта ЗАПУСКАЕТСЯ: для `LaunchedEffect` — стартует корутина, для
 *    `DisposableEffect` — выполняется его блок. Происходит при входе в композицию и при смене ключа.
 *  • [DISPOSE] — эффект СВОРАЧИВАЕТСЯ: корутина `LaunchedEffect` отменяется, у `DisposableEffect`
 *    вызывается `onDispose { }`. Происходит при выходе из композиции и (перед новым [START]) при
 *    смене ключа.
 *
 * Ключевая гарантия порядка: при смене ключа сначала [DISPOSE] старого эффекта, затем [START] нового.
 */
enum class EffectEvent { START, DISPOSE }
