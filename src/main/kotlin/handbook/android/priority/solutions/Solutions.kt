package handbook.android.priority.solutions

import handbook.android.priority.Cgroup
import handbook.android.priority.Priority
import handbook.android.priority.ThreadSpec
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** Эталонные решения темы 4. Подсмотри, если застрял с [handbook.android.priority.PriorityTasks]. */
object PrioritySolutions {

    // ── общие модельные помощники ──

    /** Точный вес CFS (без округления) — доля CPU пропорциональна ему. */
    private fun weight(nice: Int): Double = Priority.NICE_0_WEIGHT * Priority.NICE_STEP.pow(-nice)

    /** Эффективный вес с учётом cgroup (фон троттлится множителем). */
    private fun effectiveWeight(s: ThreadSpec): Double = weight(s.nice) * s.cgroup.cpuShareMultiplier

    private fun sharePercent(part: Double, total: Double): Int = (part * 100.0 / total).roundToInt()

    // ── Лёгкие ──

    fun lowerNiceIsHigher(a: Int, b: Int): Boolean = a < b

    fun clampNice(nice: Int): Int = nice.coerceIn(Priority.MIN_NICE, Priority.MAX_NICE)

    fun niceForConstant(name: String): Int = when (name.uppercase()) {
        "DEFAULT" -> Priority.DEFAULT
        "BACKGROUND" -> Priority.BACKGROUND
        "FOREGROUND" -> Priority.FOREGROUND
        "DISPLAY" -> Priority.DISPLAY
        "URGENT_DISPLAY" -> Priority.URGENT_DISPLAY
        "AUDIO" -> Priority.AUDIO
        "URGENT_AUDIO" -> Priority.URGENT_AUDIO
        "LOWEST" -> Priority.LOWEST
        else -> throw IllegalArgumentException("неизвестная константа: $name")
    }

    fun adjustNice(current: Int, delta: Int): Int = clampNice(current + delta)

    fun niceToWeight(nice: Int): Int = weight(nice).roundToInt()

    fun cpuSharePercent(niceA: Int, niceB: Int): Int {
        val wa = niceToWeight(niceA).toDouble()
        val wb = niceToWeight(niceB).toDouble()
        return sharePercent(wa, wa + wb)
    }

    fun highestPriority(specs: List<ThreadSpec>): String =
        specs.withIndex().minWith(compareBy({ it.value.nice }, { it.index })).value.name

    fun backgroundCgroupGetsLess(nice: Int): Boolean {
        val fg = effectiveWeight(ThreadSpec("fg", nice, Cgroup.FOREGROUND))
        val bg = effectiveWeight(ThreadSpec("bg", nice, Cgroup.BACKGROUND))
        return fg > bg
    }

    // ── Средние ──

    fun cfsSchedule(specs: List<ThreadSpec>, quanta: Int): List<String> {
        val vruntime = DoubleArray(specs.size)
        val out = ArrayList<String>(quanta)
        repeat(quanta) {
            var idx = 0
            for (i in specs.indices) if (vruntime[i] < vruntime[idx]) idx = i  // min vruntime, тай-брейк по индексу
            out.add(specs[idx].name)
            vruntime[idx] += Priority.NICE_0_WEIGHT / weight(specs[idx].nice)  // += 1.25^nice
        }
        return out
    }

    fun cfsShareCounts(specs: List<ThreadSpec>, quanta: Int): Map<String, Int> {
        val counts = specs.associate { it.name to 0 }.toMutableMap()
        cfsSchedule(specs, quanta).forEach { counts[it] = counts.getValue(it) + 1 }
        return counts
    }

    fun backgroundThrottled(fgNice: Int, bgNice: Int): Pair<Int, Int> {
        val fg = weight(fgNice) * Cgroup.FOREGROUND.cpuShareMultiplier
        val bg = weight(bgNice) * Cgroup.BACKGROUND.cpuShareMultiplier
        val total = fg + bg
        return sharePercent(fg, total) to sharePercent(bg, total)
    }

    fun strictPrioritySchedule(specs: List<ThreadSpec>, quanta: Int): List<String> {
        // Строгий приоритет: всегда бежит самый приоритетный runnable-поток; он не блокируется → монополия.
        val top = specs.withIndex().minWith(compareBy({ it.value.nice }, { it.index })).value.name
        return List(quanta) { top }
    }

    fun starvedUnderStrict(specs: List<ThreadSpec>, quanta: Int): List<String> {
        val ran = strictPrioritySchedule(specs, quanta).toSet()
        return specs.map { it.name }.filter { it !in ran }
    }

    fun hasPriorityInversion(lowNice: Int, midNice: Int, highNice: Int): Boolean {
        // high ждёт лок, удерживаемый low. Если существует независимый mid, приоритетнее low (mid.nice < low.nice),
        // он вытесняет low → low не может отпустить лок → high косвенно ждёт mid = инверсия приоритетов.
        return midNice < lowNice && highNice < lowNice
    }

    fun inheritedNice(holderNice: Int, waiterNice: Int): Int = minOf(holderNice, waiterNice)

    // ── Сложные ──

    fun uiShareWithAndWithoutNicing(uiNice: Int, bgNiceWrong: Int, bgNiceRight: Int): Pair<Int, Int> {
        val ui = weight(uiNice)
        fun uiShareAgainst(bgNice: Int): Int {
            val bg = weight(bgNice)
            return sharePercent(ui, ui + bg)
        }
        return uiShareAgainst(bgNiceWrong) to uiShareAgainst(bgNiceRight)
    }

    fun rankByEffectiveShare(specs: List<ThreadSpec>): List<String> =
        specs.withIndex()
            .sortedWith(compareByDescending<IndexedValue<ThreadSpec>> { effectiveWeight(it.value) }.thenBy { it.index })
            .map { it.value.name }

    fun cfsIsFair(specs: List<ThreadSpec>, quanta: Int, tolerancePercent: Int): Boolean {
        val counts = cfsShareCounts(specs, quanta)
        val totalWeight = specs.sumOf { weight(it.nice) }
        val tolerance = quanta * tolerancePercent / 100.0
        return specs.all { s ->
            val expected = quanta * weight(s.nice) / totalWeight
            abs(counts.getValue(s.name) - expected) <= tolerance
        }
    }

    fun transitiveInheritedNice(chain: List<Int>): Int = chain.min()

    fun schedulingClasses(specs: List<ThreadSpec>, quanta: Int): List<String> {
        // Класс реального времени (SCHED_FIFO) вытесняет обычные потоки целиком: пока есть runnable RT,
        // бежит самый приоритетный из них. Нет RT → обычный CFS.
        val realtime = specs.withIndex().filter { it.value.realtime }
        if (realtime.isNotEmpty()) {
            val top = realtime.minWith(compareBy({ it.value.nice }, { it.index })).value.name
            return List(quanta) { top }
        }
        return cfsSchedule(specs, quanta)
    }
}
