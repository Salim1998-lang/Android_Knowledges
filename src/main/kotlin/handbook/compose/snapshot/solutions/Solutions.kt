package handbook.compose.snapshot.solutions

import handbook.compose.snapshot.ApplyResult
import handbook.compose.snapshot.Record

/** Эталонные решения темы 3. Подсмотри, если застрял с [handbook.compose.snapshot.SnapshotTasks]. */
object SnapshotSolutions {

    // ── Лёгкие ──

    fun visibleRecord(history: List<Record>, atVersion: Int): Record? =
        history.filter { it.version <= atVersion }.maxByOrNull { it.version }

    fun readAt(history: List<Record>, atVersion: Int, default: Any?): Any? {
        val record = visibleRecord(history, atVersion)
        return if (record != null) record.value else default
    }

    fun latestVersion(history: List<Record>): Int =
        history.maxOfOrNull { it.version } ?: 0

    fun writeRecord(history: List<Record>, version: Int, value: Any?): List<Record> =
        history + Record(version, value)

    fun isVisibleTo(writeVersion: Int, snapshotBase: Int): Boolean =
        writeVersion <= snapshotBase

    fun hiddenWrites(history: List<Record>, base: Int): List<Record> =
        history.filter { it.version > base }

    fun readInSnapshot(hasOwnWrite: Boolean, ownWrite: Any?, committed: Any?): Any? =
        if (hasOwnWrite) ownWrite else committed

    fun nextVersion(currentGlobal: Int): Int =
        currentGlobal + 1

    // ── Средние ──

    fun readForSnapshot(history: List<Record>, base: Int, hasOwnWrite: Boolean, ownWrite: Any?, default: Any?): Any? =
        if (hasOwnWrite) ownWrite else readAt(history, base, default)

    fun hasConflict(myWrites: Set<String>, modifiedSinceBase: Set<String>): Boolean =
        myWrites.intersect(modifiedSinceBase).isNotEmpty()

    fun conflictingStates(myWrites: Set<String>, modifiedSinceBase: Set<String>): Set<String> =
        myWrites.intersect(modifiedSinceBase)

    fun merge(previous: Any?, current: Any?, applied: Any?): Any? = when {
        current == previous -> applied   // менялась только наша сторона
        applied == previous -> current   // менялась только чужая сторона
        applied == current -> applied    // обе пришли к одному значению
        else -> null                     // настоящий конфликт
    }

    fun applyResult(conflict: Boolean, mergeable: Boolean): ApplyResult = when {
        !conflict -> ApplyResult.SUCCESS
        mergeable -> ApplyResult.MERGED
        else -> ApplyResult.FAILURE
    }

    fun applyAtomic(global: Map<String, Any?>, writes: Map<String, Any?>, success: Boolean): Map<String, Any?> =
        if (success) global + writes else global

    fun snapshotFlowEmits(values: List<Any?>): Int =
        if (values.isEmpty()) 0 else 1 + (1 until values.size).count { values[it] != values[it - 1] }

    // ── Сложные ──

    fun withMutableSnapshot(global: Map<String, Any?>, writes: Map<String, Any?>): Map<String, Any?> =
        global + writes

    fun applyTwo(
        global: Map<String, Any?>,
        aWrites: Map<String, Any?>,
        bWrites: Map<String, Any?>,
        mergeableStates: Set<String>,
    ): Pair<Map<String, Any?>, List<ApplyResult>> {
        val afterA = global + aWrites
        val conflicts = bWrites.keys.intersect(aWrites.keys)
        if (conflicts.isEmpty()) {
            return (afterA + bWrites) to listOf(ApplyResult.SUCCESS, ApplyResult.SUCCESS)
        }
        val result = afterA.toMutableMap()
        var anyMerged = false
        for ((state, applied) in bWrites) {
            if (state !in conflicts) {
                result[state] = applied
                continue
            }
            val merged = if (state in mergeableStates) merge(global[state], afterA[state], applied) else null
            if (merged == null) {
                // apply B проваливается целиком — все записи B отброшены (атомарность).
                return afterA to listOf(ApplyResult.SUCCESS, ApplyResult.FAILURE)
            }
            result[state] = merged
            anyMerged = true
        }
        return result to listOf(ApplyResult.SUCCESS, if (anyMerged) ApplyResult.MERGED else ApplyResult.SUCCESS)
    }

    fun stableReadsWithinSnapshot(history: List<Record>, base: Int, reads: Int, default: Any?): List<Any?> {
        val value = readAt(history, base, default)
        return List(reads) { value }
    }

    fun visibleRecordExcluding(history: List<Record>, atVersion: Int, aborted: Set<Int>): Record? =
        history.filter { it.version <= atVersion && it.version !in aborted }.maxByOrNull { it.version }

    fun autoMergeable(triples: Map<String, Triple<Any?, Any?, Any?>>): Pair<Set<String>, Set<String>> {
        val mergeable = mutableSetOf<String>()
        val conflicting = mutableSetOf<String>()
        for ((state, t) in triples) {
            if (merge(t.first, t.second, t.third) != null) mergeable += state else conflicting += state
        }
        return mergeable to conflicting
    }
}
