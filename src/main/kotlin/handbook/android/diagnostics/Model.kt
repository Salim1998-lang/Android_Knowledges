package handbook.android.diagnostics

/**
 * Состояние потока в дампе (как `java.lang.Thread.State`). Прогресс делает только RUNNABLE; остальные
 * «стоят»: BLOCKED — ждёт монитор (`synchronized`), WAITING/TIMED_WAITING — ждёт условие/таймаут.
 */
enum class ThreadState { NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED }

/** Тип операции на потоке. `StrictMode.ThreadPolicy` ловит на ГЛАВНОМ потоке медленные диск/сеть, но не CPU. */
enum class MainOp { CPU, DISK_READ, DISK_WRITE, NETWORK }

/**
 * Реакция `StrictMode` на нарушение. Порядок [ordinal] = ВОЗРАСТАНИЕ строгости: LOG < DIALOG < DROPBOX
 * < DEATH. Несколько penalty складываются — «эффективной» считается самая строгая.
 */
enum class StrictPenalty { LOG, DIALOG, DROPBOX, DEATH }

/**
 * Событие трассировки (`Trace.beginSection`/`endSection`, видно в Perfetto/systrace). Секции вложенные:
 * каждый [Begin] должен закрываться парным [End] (LIFO). [atMs] — метка времени (для расчёта времени секции).
 */
sealed interface TraceEvent {
    data class Begin(val name: String, val atMs: Long = 0) : TraceEvent
    data class End(val atMs: Long = 0) : TraceEvent
}

/**
 * Строка потока в thread dump / ANR-трейсе: имя, состояние, какие локи ДЕРЖИТ и какой лок ЖДЁТ.
 * По совокупности таких строк строится граф «кто кого ждёт» — для поиска дедлоков и корня ANR.
 */
data class ThreadInfo(
    val name: String,
    val state: ThreadState,
    val holdsLocks: List<String> = emptyList(),
    val waitsForLock: String? = null,
)
