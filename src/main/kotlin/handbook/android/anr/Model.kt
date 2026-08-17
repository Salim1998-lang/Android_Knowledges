package handbook.android.anr

/**
 * Фазы кадра, которые `Choreographer` выполняет на главном потоке по каждому сигналу vsync, СТРОГО в
 * этом порядке (порядок = [ordinal]): ввод → анимация → вставки → обход дерева (measure/layout/draw)
 * → коммит. Вся работа всех фаз одного кадра должна уложиться в бюджет кадра, иначе — пропуск (jank).
 */
enum class FramePhase {
    INPUT,       // доставка ввода (касания)
    ANIMATION,   // аниматоры, Choreographer animation-callbacks
    INSETS,      // insets animation
    TRAVERSAL,   // measure + layout + draw дерева View
    COMMIT,      // фиксация кадра
}

/**
 * Пороги ANR по типу того, что «завис». Если главный поток не обработал соответствующее событие за
 * это время — система показывает ANR. Значения близки к реальным таймаутам Android.
 */
enum class AnrKind(val timeoutMs: Long) {
    INPUT(5_000),         // диспетчеризация ввода
    BROADCAST_FG(10_000), // BroadcastReceiver переднего плана
    BROADCAST_BG(60_000), // BroadcastReceiver фоновый
    SERVICE_FG(20_000),   // Service переднего плана
    SERVICE_BG(200_000),  // Service фоновый
}

/**
 * Тип операции на потоке. `StrictMode.ThreadPolicy` ловит на ГЛАВНОМ потоке дисковые и сетевые
 * операции (медленные), но не обычный CPU-код.
 */
enum class MainOp { CPU, DISK_READ, DISK_WRITE, NETWORK }

/** Операция с указанием, выполнялась ли она на главном потоке (для StrictMode-детектора). */
data class ThreadOp(val name: String, val op: MainOp, val onMainThread: Boolean)

/** Реакция `StrictMode` на нарушение: залогировать или уронить приложение. */
enum class StrictPenalty { LOG, DEATH }
