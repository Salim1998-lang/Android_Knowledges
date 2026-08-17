package handbook.android.queues

/**
 * Событие потока данных: значение [value], пришедшее в момент [timeMs] (на виртуальных
 * миллисекундах). Стратегии backpressure (throttle/debounce/sample/…) — это чистые функции над
 * списком таких событий, отсортированным по времени. Так их логику можно проверить детерминированно,
 * без реального времени и потоков (сам механизм `BlockingQueue` отрабатываем отдельно, на настоящих
 * потоках).
 */
data class Event(val timeMs: Long, val value: Int)

/** Сентинел («ядовитая пилюля») для остановки потребителя в примерах с реальной [java.util.concurrent.BlockingQueue]. */
const val POISON: Int = Int.MIN_VALUE
