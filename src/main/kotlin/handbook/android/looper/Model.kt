package handbook.android.looper

/**
 * Учебная модель одного запланированного сообщения Android-очереди (`MessageQueue`).
 *
 * В реальном Android это `Message`, у которого поле `when` — абсолютный момент срабатывания на
 * часах [android.os.SystemClock.uptimeMillis] (монотонные «uptime»-миллисекунды, НЕ идут назад и
 * НЕ тикают в глубоком сне). `Handler.post`/`postDelayed` кладёт `Message` в очередь своего
 * `Looper`, а `Looper.loop()` достаёт их строго в порядке `when` и выполняет на потоке этого
 * `Looper`. Здесь мы моделируем те же поля, чтобы решать задачи детерминированно на JVM.
 */
data class Scheduled(
    /** Порядок постановки в очередь — тай-брейк FIFO при равном [whenMs] (как insertion order). */
    val seq: Int,
    /** Абсолютный момент срабатывания на виртуальных «uptime»-часах (аналог `Message.when`). */
    val whenMs: Long,
    /** Метка для проверки порядка выполнения в тестах. */
    val label: String,
    /** «Токен» сообщения — по нему бьёт `Handler.removeCallbacksAndMessages(token)`. */
    val token: Any? = null,
    /** Асинхронное сообщение (`Message.setAsynchronous(true)`) — проходит сквозь sync-барьер. */
    val isAsync: Boolean = false,
    /** Сколько «занимает» обработка сообщения на потоке (для тем про jank/ANR). */
    val durationMs: Long = 0,
)

/**
 * Стратегия повторной постановки самоперепланирующегося сообщения (repeating `Runnable`).
 *  • [FIXED_DELAY] — следующий запуск через `interval` ПОСЛЕ завершения текущего: пауза между
 *    запусками постоянна, но частота «плывёт» на время работы (как `postDelayed` из самого коллбэка).
 *  • [FIXED_RATE] — следующий запуск привязан к сетке `i * interval` от старта: если работа
 *    затянулась, планировщик «догоняет» расписание (как `Choreographer`/`Animator` по vsync).
 */
enum class RepostMode { FIXED_DELAY, FIXED_RATE }
