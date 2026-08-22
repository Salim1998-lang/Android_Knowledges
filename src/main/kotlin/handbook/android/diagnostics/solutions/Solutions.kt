package handbook.android.diagnostics.solutions

import handbook.android.diagnostics.MainOp
import handbook.android.diagnostics.StrictPenalty
import handbook.android.diagnostics.ThreadInfo
import handbook.android.diagnostics.ThreadState
import handbook.android.diagnostics.TraceEvent

/** Эталонные решения темы 10. Подсмотри, если застрял с [handbook.android.diagnostics.DiagnosticsTasks]. */
object DiagnosticsSolutions {

    // ── Лёгкие ──

    fun mainThreadViolation(op: MainOp): Boolean = op != MainOp.CPU

    fun penaltyCrashesApp(penalty: StrictPenalty): Boolean = penalty == StrictPenalty.DEATH

    fun blockedWaitsForMonitor(state: ThreadState): Boolean = state == ThreadState.BLOCKED

    fun mainResponsive(mainBlockedMs: Long, thresholdMs: Long): Boolean = mainBlockedMs < thresholdMs

    fun runnableIsRunning(state: ThreadState): Boolean = state == ThreadState.RUNNABLE

    fun unclosedResourceViolation(closed: Boolean): Boolean = !closed

    fun deadlockRequiresCycle(): Boolean = true

    fun anrTraceStartsAtMain(): Boolean = true

    // ── Средние ──

    fun traceSectionsBalanced(events: List<TraceEvent>): Boolean {
        var depth = 0
        for (e in events) when (e) {
            is TraceEvent.Begin -> depth++
            is TraceEvent.End -> if (depth == 0) return false else depth--
        }
        return depth == 0
    }

    fun lockHolderOf(dump: List<ThreadInfo>, lockName: String): String? =
        dump.firstOrNull { lockName in it.holdsLocks }?.name

    fun blockedThreadCount(dump: List<ThreadInfo>): Int = dump.count { it.state == ThreadState.BLOCKED }

    fun maxTraceDepth(events: List<TraceEvent>): Int {
        var depth = 0
        var max = 0
        for (e in events) when (e) {
            is TraceEvent.Begin -> { depth++; if (depth > max) max = depth }
            is TraceEvent.End -> if (depth > 0) depth--
        }
        return max
    }

    fun anrCulpritThread(dump: List<ThreadInfo>, mainThreadName: String): String? {
        val main = dump.firstOrNull { it.name == mainThreadName } ?: return null
        val lock = main.waitsForLock ?: return null
        return dump.firstOrNull { lock in it.holdsLocks }?.name
    }

    fun effectivePenalty(penalties: Set<StrictPenalty>): StrictPenalty =
        penalties.maxByOrNull { it.ordinal } ?: StrictPenalty.LOG

    fun strictModeViolationCount(ops: List<MainOp>): Int = ops.count { it != MainOp.CPU }

    // ── Сложные ──

    /** Граф «кто кого ждёт»: поток → поток, держащий лок, которого первый ждёт. */
    private fun waitsForEdges(dump: List<ThreadInfo>): Map<String, String> {
        val holderOf = HashMap<String, String>()
        for (t in dump) for (lock in t.holdsLocks) holderOf[lock] = t.name
        val adjacency = HashMap<String, String>()
        for (t in dump) {
            val lock = t.waitsForLock ?: continue
            val holder = holderOf[lock] ?: continue
            if (holder != t.name) adjacency[t.name] = holder
        }
        return adjacency
    }

    fun detectDeadlock(dump: List<ThreadInfo>): Boolean {
        val adjacency = waitsForEdges(dump)
        val visited = HashSet<String>()
        val inStack = HashSet<String>()

        fun dfs(node: String): Boolean {
            visited.add(node)
            inStack.add(node)
            val next = adjacency[node]
            if (next != null) {
                if (next !in visited) { if (dfs(next)) return true }
                else if (next in inStack) return true   // ребро назад в стек → цикл
            }
            inStack.remove(node)
            return false
        }

        for (t in dump) if (t.name !in visited && dfs(t.name)) return true
        return false
    }

    fun deadlockedThreads(dump: List<ThreadInfo>): List<String> {
        val adjacency = waitsForEdges(dump)
        val onCycle = sortedSetOf<String>()
        for (start in dump.map { it.name }) {
            var current = adjacency[start]
            val seen = HashSet<String>()
            while (current != null && current !in seen) {
                if (current == start) { onCycle.add(start); break }
                seen.add(current)
                current = adjacency[current]
            }
        }
        return onCycle.toList()
    }

    fun sectionSelfTimeMs(events: List<TraceEvent>, name: String): Long {
        val selfTime = HashMap<String, Long>()
        val stack = ArrayDeque<String>()
        var last: Long? = null
        for (e in events) {
            val ts = when (e) {
                is TraceEvent.Begin -> e.atMs
                is TraceEvent.End -> e.atMs
            }
            if (last != null && stack.isNotEmpty()) {
                val top = stack.last()
                selfTime[top] = (selfTime[top] ?: 0L) + (ts - last)
            }
            when (e) {
                is TraceEvent.Begin -> stack.addLast(e.name)
                is TraceEvent.End -> if (stack.isNotEmpty()) stack.removeLast()
            }
            last = ts
        }
        return selfTime[name] ?: 0L
    }

    fun anrRootBlocker(dump: List<ThreadInfo>, mainThreadName: String): String? {
        val byName = dump.associateBy { it.name }
        var current = byName[mainThreadName] ?: return null
        if (current.waitsForLock == null) return null
        val visited = HashSet<String>()
        while (true) {
            val lock = current.waitsForLock ?: return current.name   // никого не ждёт → корень
            if (current.name in visited) return null                 // цикл (дедлок)
            visited.add(current.name)
            val holderName = dump.firstOrNull { lock in it.holdsLocks }?.name ?: return null
            current = byName[holderName] ?: return null
        }
    }

    fun longestBlockChainLength(dump: List<ThreadInfo>): Int {
        val adjacency = waitsForEdges(dump)
        val memo = HashMap<String, Int>()
        fun chain(node: String): Int = memo.getOrPut(node) {
            val next = adjacency[node]
            if (next == null) 1 else 1 + chain(next)   // граф без циклов (дедлок — отдельная задача)
        }
        return dump.map { chain(it.name) }.maxOrNull() ?: 0
    }
}
