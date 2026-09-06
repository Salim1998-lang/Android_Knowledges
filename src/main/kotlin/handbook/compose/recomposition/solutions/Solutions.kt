package handbook.compose.recomposition.solutions

import handbook.compose.recomposition.Node

/** Эталонные решения темы 1. Подсмотри, если застрял с [handbook.compose.recomposition.RecompositionTasks]. */
object RecompositionSolutions {

    // ── Лёгкие ──

    fun readersOf(nodes: List<Node>, state: String): Set<String> =
        nodes.filter { state in it.reads }.map { it.id }.toSet()

    fun writeInvalidates(old: Any?, new: Any?): Boolean =
        old != new

    fun hasReaders(nodes: List<Node>, state: String): Boolean =
        nodes.any { state in it.reads }

    fun unreadStates(nodes: List<Node>, allStates: Set<String>): Set<String> {
        val read = nodes.flatMap { it.reads }.toSet()
        return allStates - read
    }

    fun invalidatedScopes(nodes: List<Node>, changed: Set<String>): Set<String> =
        nodes.filter { it.reads.any { r -> r in changed } }.map { it.id }.toSet()

    fun willSkip(skippable: Boolean, paramsEqual: Boolean): Boolean =
        skippable && paramsEqual

    fun parentOf(nodes: List<Node>, nodeId: String): String? =
        nodes.firstOrNull { nodeId in it.children }?.id

    fun pathToRoot(nodes: List<Node>, nodeId: String): List<String> {
        val path = mutableListOf(nodeId)
        var cur = nodeId
        while (true) {
            val parent = parentOf(nodes, cur) ?: break
            path += parent
            cur = parent
        }
        return path
    }

    // ── Средние ──

    fun restartScope(nodes: List<Node>, nodeId: String): String {
        val byId = nodes.associateBy { it.id }
        val path = pathToRoot(nodes, nodeId)
        return path.firstOrNull { byId[it]?.restartable == true } ?: path.last()
    }

    fun recomposeTargets(nodes: List<Node>, changed: Set<String>): Set<String> =
        invalidatedScopes(nodes, changed).map { restartScope(nodes, it) }.toSet()

    fun recompose(nodes: List<Node>, root: String, changedParams: Set<String>): Set<String> {
        val byId = nodes.associateBy { it.id }
        val executed = linkedSetOf<String>()
        fun visit(id: String) {
            executed += id
            val node = byId[id] ?: return
            for (childId in node.children) {
                val child = byId[childId] ?: continue
                val runs = !child.skippable || childId in changedParams
                if (runs) visit(childId)
            }
        }
        visit(root)
        return executed
    }

    fun skippedNodes(nodes: List<Node>, root: String, changedParams: Set<String>): Set<String> {
        val byId = nodes.associateBy { it.id }
        val skipped = linkedSetOf<String>()
        fun visit(id: String) {
            val node = byId[id] ?: return
            for (childId in node.children) {
                val child = byId[childId] ?: continue
                val runs = !child.skippable || childId in changedParams
                if (runs) visit(childId) else skipped += childId
            }
        }
        visit(root)
        return skipped
    }

    fun skippableFromParams(paramStabilities: List<Boolean>): Boolean =
        paramStabilities.all { it }

    fun readDiff(oldReads: Set<String>, newReads: Set<String>): Pair<Set<String>, Set<String>> =
        (newReads - oldReads) to (oldReads - newReads)

    fun recomposeCount(nodes: List<Node>, batches: List<Set<String>>): Int =
        batches.sumOf { recomposeTargets(nodes, it).size }

    // ── Сложные ──

    fun recomposeFromState(nodes: List<Node>, changed: Set<String>, changedParams: Set<String>): Set<String> {
        val executed = linkedSetOf<String>()
        for (target in recomposeTargets(nodes, changed)) {
            executed += recompose(nodes, target, changedParams)
        }
        return executed
    }

    fun skippedVsRecomposed(nodes: List<Node>, root: String, changedParams: Set<String>): Pair<Int, Int> =
        recompose(nodes, root, changedParams).size to skippedNodes(nodes, root, changedParams).size

    fun perNodeRecompositionCounts(nodes: List<Node>, batches: List<Set<String>>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (batch in batches) {
            for (target in recomposeTargets(nodes, batch)) {
                counts[target] = (counts[target] ?: 0) + 1
            }
        }
        return counts
    }

    fun derivedReaderInvalidations(readerCount: Int, resultChanged: List<Boolean>): List<Int> =
        resultChanged.map { if (it) readerCount else 0 }

    fun recompositionHotspots(nodes: List<Node>, batches: List<Set<String>>, threshold: Int): Set<String> =
        perNodeRecompositionCounts(nodes, batches).filterValues { it >= threshold }.keys
}
