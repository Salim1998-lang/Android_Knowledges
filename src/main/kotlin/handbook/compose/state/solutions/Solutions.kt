package handbook.compose.state.solutions

import handbook.compose.state.Slot
import handbook.compose.state.SurvivalEvent

/** Эталонные решения темы 2. Подсмотри, если застрял с [handbook.compose.state.StateTasks]. */
object StateSolutions {

    // ── Лёгкие ──

    fun keysUnchanged(prevKeys: List<Any?>, newKeys: List<Any?>): Boolean =
        prevKeys == newKeys

    fun rememberValue(prev: Slot?, keys: List<Any?>, compute: () -> Any?): Slot =
        if (prev != null && prev.keys == keys) prev else Slot(keys, compute())

    fun stateValueAfterRecompose(remembered: Boolean, initial: Any?, current: Any?): Any? =
        if (remembered) current else initial

    fun rememberSurvives(event: SurvivalEvent): Boolean =
        event == SurvivalEvent.RECOMPOSE

    fun rememberSaveableSurvives(event: SurvivalEvent): Boolean =
        true

    fun saveableCanStore(autoSaveable: Boolean, hasSaver: Boolean): Boolean =
        autoSaveable || hasSaver

    fun derivedNotifies(oldResult: Any?, newResult: Any?): Boolean =
        oldResult != newResult

    fun recomputeTimes(keySequence: List<List<Any?>>): Int {
        var count = 0
        var prev: List<Any?>? = null
        for (keys in keySequence) {
            if (prev == null || prev != keys) count++
            prev = keys
        }
        return count
    }

    // ── Средние ──

    fun rememberedValues(keySequence: List<List<Any?>>, compute: (List<Any?>) -> Any?): List<Any?> {
        val out = mutableListOf<Any?>()
        var slot: Slot? = null
        for (keys in keySequence) {
            slot = rememberValue(slot, keys) { compute(keys) }
            out += slot.value
        }
        return out
    }

    fun derivedNotifications(results: List<Any?>): Int =
        (1 until results.size).count { results[it] != results[it - 1] }

    fun plainVsDerived(inputs: List<Any?>, derive: (Any?) -> Any?): Pair<Int, Int> {
        val results = inputs.map(derive)
        val plain = (1 until inputs.size).count { inputs[it] != inputs[it - 1] }
        val derived = (1 until results.size).count { results[it] != results[it - 1] }
        return plain to derived
    }

    fun lowestCommonAncestor(parents: Map<String, String>, a: String, b: String): String {
        val bAncestors = pathUp(parents, b).toSet()
        return pathUp(parents, a).first { it in bAncestors }
    }

    fun hoistTarget(parents: Map<String, String>, users: List<String>): String =
        users.reduce { acc, user -> lowestCommonAncestor(parents, acc, user) }

    fun isStateless(hasInternalState: Boolean, exposesValue: Boolean, exposesCallback: Boolean): Boolean =
        !hasInternalState && exposesValue && exposesCallback

    fun rememberIsUseless(keySequence: List<List<Any?>>): Boolean =
        keySequence.size >= 2 && (1 until keySequence.size).all { keySequence[it] != keySequence[it - 1] }

    // ── Сложные ──

    fun restoreAfterProcessDeath(saved: Map<String, Any?>, key: String, default: Any?): Any? =
        if (saved.containsKey(key)) saved[key] else default

    fun valueAfterEvents(saveable: Boolean, initial: Any?, written: Any?, events: List<SurvivalEvent>): Any? {
        var held = written
        for (event in events) {
            val survives = if (saveable) rememberSaveableSurvives(event) else rememberSurvives(event)
            if (!survives) held = initial
        }
        return held
    }

    fun derivedStats(inputChanged: List<Boolean>, resultChanged: List<Boolean>): Pair<Int, Int> =
        inputChanged.count { it } to resultChanged.count { it }

    fun overHoisted(parents: Map<String, String>, users: List<String>, host: String): Boolean {
        val lca = hoistTarget(parents, users)
        return host != lca && host in pathUp(parents, lca)
    }

    fun propDrillingDepth(parents: Map<String, String>, host: String, user: String): Int {
        var cur = user
        var depth = 0
        while (cur != host) {
            cur = parents[cur] ?: return -1
            depth++
        }
        return depth
    }

    // ── helpers ──

    /** Путь от узла вверх до корня включительно: [node, …, root]. */
    private fun pathUp(parents: Map<String, String>, node: String): List<String> {
        val path = mutableListOf(node)
        var cur = node
        while (parents.containsKey(cur)) {
            cur = parents.getValue(cur)
            path += cur
        }
        return path
    }
}
