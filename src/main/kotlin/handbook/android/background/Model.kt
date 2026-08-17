package handbook.android.background

/**
 * Способ запуска фоновой работы в Android — от «сырого» потока до системного планировщика.
 *  • RAW_THREAD — голый `Thread`/`Executor`: НЕ переживает смерть процесса, нет гарантий и рескедулинга.
 *  • WORK_MANAGER / JOB_SCHEDULER — отложенная ГАРАНТИРОВАННАЯ работа: переживает перезагрузку, учитывает
 *    ограничения (Doze, App Standby, constraints), умеет ретраи с backoff.
 *  • FOREGROUND_SERVICE — видимая пользователю работа «прямо сейчас»: обходит фоновые лимиты, требует
 *    постоянную нотификацию.
 *  • ALARM_MANAGER — запуск в точное/будильниковое время (алармы сбрасываются на reboot — нужен re-register).
 */
enum class BackgroundApi { RAW_THREAD, WORK_MANAGER, JOB_SCHEDULER, FOREGROUND_SERVICE, ALARM_MANAGER }

/**
 * App Standby bucket: система относит приложение в «корзину» по частоте использования. Чем «холоднее»
 * корзина, тем сильнее откладывается его фоновая работа (deferMinutes — до скольки минут может быть
 * отложен запуск отложенной задачи). Значения приближены к реальным окнам Android.
 */
enum class StandbyBucket(val deferMinutes: Long) {
    ACTIVE(0),          // используется прямо сейчас — без задержки
    WORKING_SET(120),   // регулярно — отложить до ~2 ч
    FREQUENT(480),      // часто, но не сегодня — до ~8 ч
    RARE(1440),         // редко — до ~24 ч
    RESTRICTED(1440),   // сильно ограничено (пользователь/система) — до ~24 ч
}

/** Способ отложенного повтора при неудаче: линейный или экспоненциальный рост задержки. */
enum class BackoffPolicy { LINEAR, EXPONENTIAL }

/** Результат `Worker.doWork()`: успех, окончательный провал или запрос на повтор (re-enqueue). */
enum class WorkResult { SUCCESS, FAILURE, RETRY }

/** Состояние work-задачи в WorkManager (упрощённо). BLOCKED — ждёт предшественников в цепочке. */
enum class WorkState { ENQUEUED, RUNNING, SUCCEEDED, FAILED, BLOCKED, CANCELLED }

/**
 * Политика при постановке УНИКАЛЬНОЙ работы, когда работа с таким именем уже существует:
 *  • REPLACE — отменить старую, поставить новую;
 *  • KEEP    — оставить старую, новую проигнорировать;
 *  • APPEND  — выполнить новую ПОСЛЕ существующей (в цепочку).
 */
enum class ExistingWorkPolicy { REPLACE, KEEP, APPEND }

/** Ограничения запуска work-задачи: она стартует только когда ВСЕ требуемые условия выполнены. */
data class Constraints(
    val requiresNetwork: Boolean = false,
    val requiresUnmeteredNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
    val requiresIdle: Boolean = false,
    val requiresStorageNotLow: Boolean = false,
)

/** Текущее состояние устройства — по нему проверяются [Constraints]. */
data class DeviceState(
    val hasNetwork: Boolean = false,
    val networkUnmetered: Boolean = false,
    val charging: Boolean = false,
    val batteryLow: Boolean = false,
    val idle: Boolean = false,
    val storageLow: Boolean = false,
)

/** Константы планировщиков (приближены к реальным). */
object WorkLimits {
    const val MIN_BACKOFF_MS = 10_000L                 // WorkManager MIN_BACKOFF_MILLIS (10 с)
    const val MAX_BACKOFF_MS = 5L * 60 * 60 * 1000     // MAX_BACKOFF_MILLIS (5 часов)
    const val JOB_SCHEDULER_MAX_JOBS = 100             // лимит запланированных задач на приложение
}
