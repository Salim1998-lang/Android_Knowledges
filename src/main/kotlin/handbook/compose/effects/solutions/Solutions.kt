package handbook.compose.effects.solutions

import handbook.compose.effects.EffectEvent

/** Эталонные решения темы 4. Подсмотри, если застрял с [handbook.compose.effects.EffectsTasks]. */
object EffectsSolutions {

    // ── Лёгкие ──

    fun restartsOnKeyChange(prevKeys: List<Any?>, newKeys: List<Any?>): Boolean =
        prevKeys != newKeys

    fun sideEffectRunCount(compositions: Int): Int =
        compositions

    fun constantKeyLaunches(compositions: Int): Int =
        if (compositions > 0) 1 else 0

    fun disposableTransition(prevKeys: List<Any?>, newKeys: List<Any?>): List<EffectEvent> =
        if (prevKeys == newKeys) emptyList() else listOf(EffectEvent.DISPOSE, EffectEvent.START)

    fun enterEvents(): List<EffectEvent> =
        listOf(EffectEvent.START)

    fun leaveEvents(): List<EffectEvent> =
        listOf(EffectEvent.DISPOSE)

    fun scopeActive(inComposition: Boolean): Boolean =
        inComposition

    fun updatedStateValue(values: List<Any?>): Any? =
        values.lastOrNull()

    // ── Средние ──

    fun disposableLifecycle(frames: List<List<Any?>?>): List<EffectEvent> {
        val events = mutableListOf<EffectEvent>()
        var active: List<Any?>? = null
        for (frame in frames) {
            when {
                frame == null -> if (active != null) {
                    events += EffectEvent.DISPOSE
                    active = null
                }
                active == null -> {
                    events += EffectEvent.START
                    active = frame
                }
                active != frame -> {
                    events += EffectEvent.DISPOSE
                    events += EffectEvent.START
                    active = frame
                }
            }
        }
        return events
    }

    fun launchCount(frames: List<List<Any?>?>): Int =
        disposableLifecycle(frames).count { it == EffectEvent.START }

    fun disposeCount(frames: List<List<Any?>?>): Int =
        disposableLifecycle(frames).count { it == EffectEvent.DISPOSE }

    fun activeAfter(frames: List<List<Any?>?>): Boolean {
        var active: List<Any?>? = null
        for (frame in frames) {
            when {
                frame == null -> active = null
                active == null || active != frame -> active = frame
            }
        }
        return active != null
    }

    fun sideEffectVsLaunched(compositions: Int): Pair<Int, Int> =
        sideEffectRunCount(compositions) to constantKeyLaunches(compositions)

    fun effectRestartsOnValueChange(useUpdatedState: Boolean, valueChanged: Boolean): Boolean =
        if (useUpdatedState) false else valueChanged

    fun keyedRunCount(keySequence: List<List<Any?>>): Int {
        if (keySequence.isEmpty()) return 0
        return 1 + (1 until keySequence.size).count { keySequence[it] != keySequence[it - 1] }
    }

    // ── Сложные ──

    fun launchedEffectLifecycle(frames: List<List<Any?>?>, disposeAtEnd: Boolean): List<EffectEvent> {
        val events = disposableLifecycle(frames).toMutableList()
        if (disposeAtEnd && activeAfter(frames)) events += EffectEvent.DISPOSE
        return events
    }

    fun rememberUpdatedStateScenario(useUpdatedState: Boolean, valueSequence: List<Any?>): Pair<Int, Any?> {
        val observed = valueSequence.lastOrNull()
        val restarts = if (useUpdatedState) 0 else (1 until valueSequence.size).count { valueSequence[it] != valueSequence[it - 1] }
        return restarts to observed
    }

    fun applyChanges(effects: List<Triple<String, List<Any?>, List<Any?>>>): List<Pair<String, EffectEvent>> {
        val changed = effects.filter { (_, prev, new) -> prev != new }
        val disposes = changed.map { it.first to EffectEvent.DISPOSE }
        val starts = changed.map { it.first to EffectEvent.START }
        return disposes + starts
    }

    fun scopeJobsAfter(launched: Int, completed: Int, leftComposition: Boolean): Int =
        if (leftComposition) 0 else launched - completed

    fun disposableTrace(keySequence: List<List<Any?>>): List<Pair<Int, EffectEvent>> {
        val trace = mutableListOf<Pair<Int, EffectEvent>>()
        keySequence.forEachIndexed { i, keys ->
            when {
                i == 0 -> trace += i to EffectEvent.START
                keys != keySequence[i - 1] -> {
                    trace += i to EffectEvent.DISPOSE
                    trace += i to EffectEvent.START
                }
            }
        }
        return trace
    }
}
