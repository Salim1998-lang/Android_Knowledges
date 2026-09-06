package handbook.compose.phases.solutions

import handbook.compose.phases.Phase
import handbook.compose.phases.ReadingModifier

/** Эталонные решения темы 8. Подсмотри, если застрял с [handbook.compose.phases.PhasesTasks]. */
object PhasesSolutions {

    // ── Лёгкие ──

    fun order(): List<Phase> =
        listOf(Phase.COMPOSITION, Phase.LAYOUT, Phase.DRAWING)

    fun indexOf(phase: Phase): Int =
        phase.ordinal

    fun isBefore(a: Phase, b: Phase): Boolean =
        a.ordinal < b.ordinal

    fun next(phase: Phase): Phase? =
        Phase.entries.getOrNull(phase.ordinal + 1)

    fun role(phase: Phase): String = when (phase) {
        Phase.COMPOSITION -> "что показывать"
        Phase.LAYOUT -> "где и какого размера"
        Phase.DRAWING -> "как нарисовать"
    }

    fun phasesFrom(phase: Phase): List<Phase> =
        order().filter { it.ordinal >= phase.ordinal }

    fun phasesBefore(phase: Phase): List<Phase> =
        order().filter { it.ordinal < phase.ordinal }

    fun readsIn(m: ReadingModifier): Phase =
        m.readsIn

    // ── Средние ──

    fun earliestRead(readPhases: List<Phase>): Phase =
        readPhases.minBy { it.ordinal }

    fun invalidatedPhases(readPhases: List<Phase>): List<Phase> =
        if (readPhases.isEmpty()) emptyList() else phasesFrom(earliestRead(readPhases))

    fun skippedPhases(readPhases: List<Phase>): List<Phase> =
        if (readPhases.isEmpty()) order() else phasesBefore(earliestRead(readPhases))

    fun deferSaves(m: ReadingModifier): List<Phase> =
        phasesBefore(m.readsIn)

    fun cheaperUnderChange(a: ReadingModifier, b: ReadingModifier): ReadingModifier =
        if (b.readsIn.ordinal > a.readsIn.ordinal) b else a

    fun changeCost(readPhases: List<Phase>, costs: Map<Phase, Int>): Int =
        invalidatedPhases(readPhases).sumOf { costs[it] ?: 0 }

    fun savedCost(readPhases: List<Phase>, costs: Map<Phase, Int>): Int =
        skippedPhases(readPhases).sumOf { costs[it] ?: 0 }

    // ── Сложные ──

    fun runFrame(reads: Map<String, List<Phase>>, changed: Set<String>): List<Phase> {
        val phases = changed.flatMap { reads[it].orEmpty() }
        return invalidatedPhases(phases)
    }

    fun animationCost(readPhase: Phase, costs: Map<Phase, Int>, frames: Int): Int =
        frames * changeCost(listOf(readPhase), costs)

    fun bestModifier(options: Map<String, ReadingModifier>): String =
        options.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, ReadingModifier>> { it.value.readsIn.ordinal }
                    .thenBy { it.key },
            )
            .first().key

    fun compareStrategies(frames: Int, costs: Map<Phase, Int>): Pair<Int, Int> =
        animationCost(Phase.COMPOSITION, costs, frames) to animationCost(Phase.DRAWING, costs, frames)

    fun timeline(reads: Map<String, List<Phase>>, changes: List<Set<String>>): List<List<Phase>> =
        changes.map { runFrame(reads, it) }
}
