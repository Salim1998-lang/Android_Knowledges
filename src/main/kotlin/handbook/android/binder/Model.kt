package handbook.android.binder

/** Вид Binder-транзакции: синхронная (блокирует вызывающего) или `oneway` (асинхронная). */
enum class TransactionKind { SYNC, ONEWAY }

/** На каком потоке исполняется код. Входящие Binder-транзакции и AIDL-колбэки идут на BINDER, не MAIN. */
enum class ThreadKind { MAIN, BINDER }

/**
 * Константы Binder-подсистемы (приближены к реальным).
 *  • Пул Binder-тредов процесса ограничен (по умолчанию ~16): на них диспетчеризуются ВХОДЯЩИЕ
 *    транзакции. Если все заняты — новые ждут, процесс «висит».
 *  • Все транзакции процесса делят один буфер ~1 МБ; превышение → `TransactionTooLargeException`.
 *    Асинхронная (`oneway`) часть ограничена примерно половиной.
 */
object Binder {
    const val POOL_MAX = 16
    const val TRANSACTION_BUFFER_BYTES = 1024 * 1024   // ~1 МБ на процесс
    const val ONEWAY_BUFFER_BYTES = 512 * 1024         // async-часть ~ половина
    const val INPUT_ANR_MS = 5_000L                    // порог ANR по вводу (см. тему anr)
}

/** Учебное описание транзакции: имя, вид, размер маршалинга и длительность обработки на стороне вызываемого. */
data class Transaction(
    val name: String,
    val kind: TransactionKind,
    val sizeBytes: Int = 0,
    val durationMs: Long = 0,
)
