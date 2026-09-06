package handbook.compose.composition.solutions

import handbook.compose.composition.Item

/** Эталонные решения темы 5. Подсмотри, если застрял с [handbook.compose.composition.CompositionTasks]. */
object CompositionSolutions {

    // ── Лёгкие ──

    fun positionalIdentities(ids: List<String>): List<Int> =
        ids.indices.toList()

    fun keyedIdentities(ids: List<String>): List<String> =
        ids.toList()

    fun stateByPosition(oldItems: List<Item>, newSize: Int, fresh: Any?): List<Any?> =
        (0 until newSize).map { if (it < oldItems.size) oldItems[it].state else fresh }

    fun reusedCount(oldSize: Int, newSize: Int): Int =
        minOf(oldSize, newSize)

    fun keyedReused(oldIds: List<String>, newIds: List<String>): Set<String> =
        oldIds.toSet() intersect newIds.toSet()

    fun keyedAdded(oldIds: List<String>, newIds: List<String>): Set<String> =
        newIds.toSet() - oldIds.toSet()

    fun keyedRemoved(oldIds: List<String>, newIds: List<String>): Set<String> =
        oldIds.toSet() - newIds.toSet()

    fun hasDuplicateKeys(ids: List<String>): Boolean =
        ids.toSet().size != ids.size

    // ── Средние ──

    fun statePositionalAfterReorder(oldItems: List<Item>, newIds: List<String>, fresh: Any?): List<Pair<String, Any?>> =
        newIds.mapIndexed { i, id -> id to (if (i < oldItems.size) oldItems[i].state else fresh) }

    fun stateKeyedAfterReorder(oldItems: List<Item>, newIds: List<String>, fresh: Any?): List<Pair<String, Any?>> {
        val byId = oldItems.associateBy { it.id }
        return newIds.map { id -> id to (if (byId.containsKey(id)) byId.getValue(id).state else fresh) }
    }

    fun mismatchedPositions(oldIds: List<String>, newIds: List<String>): List<Int> {
        val common = minOf(oldIds.size, newIds.size)
        return (0 until common).filter { oldIds[it] != newIds[it] }
    }

    fun firstDuplicateKey(ids: List<String>): String? {
        val seen = mutableSetOf<String>()
        for (id in ids) if (!seen.add(id)) return id
        return null
    }

    fun stateLostOnReorder(oldIds: List<String>, newIds: List<String>): Set<String> {
        val newIndex = newIds.withIndex().associate { (i, id) -> id to i }
        return oldIds.withIndex()
            .filter { (i, id) -> newIndex[id]?.let { it != i } == true }
            .map { it.value }
            .toSet()
    }

    fun affectedByHeadInsert(oldIds: List<String>, newIds: List<String>, keyed: Boolean): Int =
        if (keyed) {
            (newIds.toSet() - oldIds.toSet()).size
        } else {
            newIds.indices.count { i -> i >= oldIds.size || oldIds[i] != newIds[i] }
        }

    fun moveTargets(oldIds: List<String>, newIds: List<String>): Map<String, Int> {
        val oldIndex = oldIds.withIndex().associate { (i, id) -> id to i }
        return newIds.withIndex()
            .filter { (i, id) -> oldIndex[id]?.let { it != i } == true }
            .associate { (i, id) -> id to i }
    }

    // ── Сложные ──

    fun diff(oldIds: List<String>, newIds: List<String>): Triple<Set<String>, Set<String>, Set<String>> {
        val old = oldIds.toSet()
        val new = newIds.toSet()
        return Triple(old intersect new, new - old, old - new)
    }

    fun reuseSlots(oldIds: List<String>, newIds: List<String>): List<Int> =
        newIds.map { oldIds.indexOf(it) }

    fun netStateSurvivors(oldItems: List<Item>, newIds: List<String>, keyed: Boolean): Int {
        val oldIds = oldItems.map { it.id }
        return if (keyed) {
            (oldIds.toSet() intersect newIds.toSet()).size
        } else {
            val common = minOf(oldIds.size, newIds.size)
            (0 until common).count { oldIds[it] == newIds[it] }
        }
    }

    fun movableContentState(usesMovable: Boolean, preservedState: Any?, fresh: Any?): Any? =
        if (usesMovable) preservedState else fresh

    fun scopedIdentities(groups: List<List<String>>): List<Pair<Int, String>> =
        groups.flatMapIndexed { groupIndex, keys -> keys.map { groupIndex to it } }
}
