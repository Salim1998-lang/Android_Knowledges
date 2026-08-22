package handbook.android.lifecycle

/**
 * Состояния жизненного цикла (порядок и семантика — как в androidx `Lifecycle.State`).
 * Порядок [ordinal] РАСТЁТ от «мертво» к «активно»: DESTROYED < INITIALIZED < CREATED < STARTED < RESUMED.
 * Поэтому [isAtLeast] сравнивает по ordinal. «Активным» (видимым, можно трогать UI / доставлять
 * обновления наблюдателям) считается состояние ≥ STARTED.
 */
enum class LifecycleState { DESTROYED, INITIALIZED, CREATED, STARTED, RESUMED }

/** Достигнут ли этот статус как минимум [other] (по возрастающему порядку жизненного цикла). */
fun LifecycleState.isAtLeast(other: LifecycleState): Boolean = ordinal >= other.ordinal

/**
 * Область корутин и её привязка к жизненному циклу:
 *  • LIFECYCLE — `lifecycleScope`/`viewLifecycleScope`: отменяется при DESTROY компонента (и при
 *    config change, т.к. Activity/Fragment пересоздаются);
 *  • VIEW_MODEL — `viewModelScope`: ПЕРЕЖИВАЕТ config change, отменяется только в `onCleared()`.
 */
enum class ScopeKind { LIFECYCLE, VIEW_MODEL }

/**
 * Что делать с результатом фоновой работы перед касанием UI:
 *  • DROP         — компонент уже уничтожен, доставлять некуда (иначе утечка/крах);
 *  • POST_TO_MAIN — мы на фоновом потоке, надо перекинуть на главный;
 *  • DELIVER      — можно доставить прямо сейчас (главный поток, компонент жив).
 */
enum class DeliverAction { DELIVER, POST_TO_MAIN, DROP }

/** Событие в модели LiveData: установка нового значения или смена активности наблюдателя. */
sealed interface LiveEvent {
    /** `setValue`/`postValue`: новое значение. */
    data class SetValue(val value: Int) : LiveEvent
    /** Наблюдатель стал активен (STARTED+) или неактивен. */
    data class SetActive(val active: Boolean) : LiveEvent
}
