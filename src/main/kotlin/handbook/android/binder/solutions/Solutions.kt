package handbook.android.binder.solutions

import handbook.android.binder.Binder
import handbook.android.binder.ThreadKind
import handbook.android.binder.Transaction
import handbook.android.binder.TransactionKind

/** Эталонные решения темы 7. Подсмотри, если застрял с [handbook.android.binder.BinderTasks]. */
object BinderSolutions {

    // ── Лёгкие ──

    fun binderPoolMax(): Int = Binder.POOL_MAX

    fun incomingCallThread(): ThreadKind = ThreadKind.BINDER

    fun callbackNeedsMainPost(touchesUi: Boolean): Boolean = touchesUi

    fun syncBlocksCaller(kind: TransactionKind): Boolean = kind == TransactionKind.SYNC

    fun onewayReturnsValue(): Boolean = false

    fun transactionTooLarge(sizeBytes: Int): Boolean = sizeBytes > Binder.TRANSACTION_BUFFER_BYTES

    fun totalBufferExceeded(sizes: List<Int>): Boolean = sizes.sum() > Binder.TRANSACTION_BUFFER_BYTES

    fun deadObjectOnDeath(remoteAlive: Boolean): Boolean = !remoteAlive

    // ── Средние ──

    fun concurrentTransactions(incoming: Int, poolSize: Int): Int = minOf(incoming, poolSize)

    fun poolExhausted(incoming: Int, poolSize: Int): Boolean = incoming > poolSize

    fun onewaySerialized(transactions: List<Transaction>): List<String> =
        transactions.map { it.name }   // oneway к одному биндеру обрабатываются по порядку

    fun mainBlockedMs(calleeDurationMs: Long, kind: TransactionKind): Long =
        if (kind == TransactionKind.SYNC) calleeDurationMs else 0L

    fun blockingCallAnr(calleeDurationMs: Long): Boolean = calleeDurationMs >= Binder.INPUT_ANR_MS

    fun reentrantOnSameThread(): Boolean = true

    fun inheritedNiceAcrossBinder(callerNice: Int, calleeNice: Int): Int = minOf(callerNice, calleeNice)

    // ── Сложные ──

    fun poolExhaustionHang(poolSize: Int, blockedThreads: Int): Boolean = blockedThreads >= poolSize

    fun reentrancyDeadlock(holdsLock: Boolean, reentrantLock: Boolean): Boolean =
        holdsLock && !reentrantLock

    fun onewayBufferOverflow(sizes: List<Int>): Boolean = sizes.sum() > Binder.ONEWAY_BUFFER_BYTES

    fun needsCallbackSync(inbound: Int, poolSize: Int): Boolean = minOf(inbound, poolSize) > 1

    fun detectCallCycle(edges: List<Pair<String, String>>): Boolean {
        val adjacency = HashMap<String, MutableList<String>>()
        edges.forEach { (from, to) -> adjacency.getOrPut(from) { mutableListOf() }.add(to) }
        val visited = HashSet<String>()
        val inStack = HashSet<String>()

        fun dfs(node: String): Boolean {
            visited.add(node)
            inStack.add(node)
            for (next in adjacency[node].orEmpty()) {
                if (next !in visited) {
                    if (dfs(next)) return true
                } else if (next in inStack) {
                    return true   // ребро назад в стек → цикл
                }
            }
            inStack.remove(node)
            return false
        }

        val nodes = (edges.map { it.first } + edges.map { it.second }).toSet()
        for (node in nodes) if (node !in visited && dfs(node)) return true
        return false
    }
}
