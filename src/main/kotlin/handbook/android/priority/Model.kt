package handbook.android.priority

/**
 * Константы приоритетов Android — это значения **nice** ядра Linux (диапазон −20..19), а НЕ
 * `java.lang.Thread` priority (1..10). Их выставляют через `android.os.Process.setThreadPriority(...)`.
 * Правило: **чем МЕНЬШЕ nice, тем ВЫШЕ приоритет** (больше времени CPU).
 *
 * Значения совпадают с `android.os.Process.THREAD_PRIORITY_*`.
 */
object Priority {
    const val DEFAULT = 0            // обычный поток
    const val LESS_FAVORABLE = 1
    const val MORE_FAVORABLE = -1
    const val BACKGROUND = 10        // фоновая работа — должна уступать UI
    const val FOREGROUND = -2        // поток переднего плана
    const val DISPLAY = -4           // отрисовка/UI
    const val URGENT_DISPLAY = -8    // ввод + анимация текущего кадра
    const val AUDIO = -16            // аудио
    const val URGENT_AUDIO = -19     // low-latency аудио — почти realtime
    const val LOWEST = 19            // максимально «уступчивый»

    const val MIN_NICE = -20
    const val MAX_NICE = 19

    /** Вес «эталонного» потока с nice=0 в CFS Linux (`NICE_0_LOAD`). */
    const val NICE_0_WEIGHT = 1024

    /**
     * Множитель нагрузки CFS на один шаг nice (~1.25). Каждый шаг nice меняет долю CPU примерно в
     * 1.25 раза (≈ ±20%). Реальное ядро использует табличку `sched_prio_to_weight`, очень близкую к
     * этой геометрии — здесь берём чистую степень 1.25 как учебную модель.
     */
    const val NICE_STEP = 1.25
}

/**
 * cgroup процесса/потока (упрощённо). Android помещает фоновые приложения/потоки в отдельную
 * cgroup, которой суммарно выделяется лишь малая доля CPU — поэтому фоновый поток троттлится
 * ДВАЖДЫ: высоким nice И принадлежностью к background-cgroup.
 */
enum class Cgroup(val cpuShareMultiplier: Double) {
    FOREGROUND(1.0),
    BACKGROUND(0.05),
}

/**
 * Учебное описание потока для модели планировщика:
 *  • [nice] — приоритет (см. [Priority]); меньше = важнее;
 *  • [cgroup] — группа CPU (фон троттлится множителем);
 *  • [realtime] — поток класса реального времени (SCHED_FIFO): вытесняет обычные потоки целиком.
 */
data class ThreadSpec(
    val name: String,
    val nice: Int,
    val cgroup: Cgroup = Cgroup.FOREGROUND,
    val realtime: Boolean = false,
)
