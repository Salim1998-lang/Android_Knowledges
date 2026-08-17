package handbook.android.priority

import handbook.android.priority.solutions.PrioritySolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 4 «Приоритеты и планирование». Детерминированная модель планировщика: nice→веса CFS,
 * доля CPU, cgroup-троттлинг, vruntime-CFS, строгий приоритет и starvation, инверсия/наследование
 * приоритетов, RT-классы.
 *
 * Чтобы гонять против эталона — замени `SUT` на [PrioritySolutions].
 */
private typealias SUT = PriorityTasks

class PriorityTest {

    private fun spec(name: String, nice: Int, cgroup: Cgroup = Cgroup.FOREGROUND, rt: Boolean = false) =
        ThreadSpec(name, nice, cgroup, rt)

    // ── Лёгкие ──

    @Test fun `Л1 lowerNiceIsHigher`() {
        assertTrue(SUT.lowerNiceIsHigher(-4, 0))
        assertFalse(SUT.lowerNiceIsHigher(10, 0))
    }

    @Test fun `Л2 clampNice`() {
        assertEquals(19, SUT.clampNice(50))
        assertEquals(-20, SUT.clampNice(-100))
        assertEquals(0, SUT.clampNice(0))
    }

    @Test fun `Л3 niceForConstant`() {
        assertEquals(0, SUT.niceForConstant("DEFAULT"))
        assertEquals(10, SUT.niceForConstant("BACKGROUND"))
        assertEquals(-4, SUT.niceForConstant("DISPLAY"))
        assertEquals(-19, SUT.niceForConstant("URGENT_AUDIO"))
    }

    @Test fun `Л4 adjustNice`() {
        assertEquals(10, SUT.adjustNice(0, 10))
        assertEquals(19, SUT.adjustNice(15, 10))   // зажат
    }

    @Test fun `Л5 niceToWeight`() {
        assertEquals(1024, SUT.niceToWeight(0))
        assertEquals(819, SUT.niceToWeight(1))
        assertEquals(1280, SUT.niceToWeight(-1))
        assertEquals(110, SUT.niceToWeight(10))
    }

    @Test fun `Л6 cpuSharePercent`() {
        assertEquals(50, SUT.cpuSharePercent(0, 0))
        assertEquals(61, SUT.cpuSharePercent(-2, 0))
    }

    @Test fun `Л7 highestPriority`() {
        val specs = listOf(spec("bg", 10), spec("ui", -4), spec("norm", 0))
        assertEquals("ui", SUT.highestPriority(specs))
    }

    @Test fun `Л8 backgroundCgroupGetsLess`() {
        assertTrue(SUT.backgroundCgroupGetsLess(0))
    }

    // ── Средние ──

    @Test fun `С9 cfsSchedule — равные чередуются`() {
        val specs = listOf(spec("A", 0), spec("B", 0))
        assertEquals(listOf("A", "B", "A", "B"), SUT.cfsSchedule(specs, 4))
    }

    @Test fun `С9 cfsSchedule — приоритетный бежит чаще`() {
        val specs = listOf(spec("hi", 0), spec("lo", 3))
        val sched = SUT.cfsSchedule(specs, 12)
        assertTrue(sched.count { it == "hi" } > sched.count { it == "lo" })
    }

    @Test fun `С10 cfsShareCounts — включая нули не нужно, но все ключи есть`() {
        val specs = listOf(spec("A", 0), spec("B", 0), spec("C", 0))
        assertEquals(mapOf("A" to 2, "B" to 2, "C" to 2), SUT.cfsShareCounts(specs, 6))
    }

    @Test fun `С11 backgroundThrottled — двойной троттлинг`() {
        assertEquals(95 to 5, SUT.backgroundThrottled(0, 0))
    }

    @Test fun `С12 strictPrioritySchedule — монополия`() {
        val specs = listOf(spec("bg", 10), spec("ui", -4), spec("norm", 0))
        assertEquals(List(5) { "ui" }, SUT.strictPrioritySchedule(specs, 5))
    }

    @Test fun `С13 starvedUnderStrict`() {
        val specs = listOf(spec("ui", -4), spec("a", 0), spec("b", 10))
        assertEquals(listOf("a", "b"), SUT.starvedUnderStrict(specs, 5))
    }

    @Test fun `С14 hasPriorityInversion`() {
        // low=0, mid=-2 (приоритетнее low), high=-4 (приоритетнее low) → инверсия возможна
        assertTrue(SUT.hasPriorityInversion(lowNice = 0, midNice = -2, highNice = -4))
        // mid=5 менее приоритетен, чем low=0 → mid не вытеснит low → инверсии нет
        assertFalse(SUT.hasPriorityInversion(lowNice = 0, midNice = 5, highNice = -4))
    }

    @Test fun `С15 inheritedNice`() {
        assertEquals(-4, SUT.inheritedNice(holderNice = 0, waiterNice = -4))
        assertEquals(-4, SUT.inheritedNice(holderNice = -4, waiterNice = 0))
    }

    // ── Сложные ──

    @Test fun `СЛ16 uiShareWithAndWithoutNicing`() {
        // UI=DISPLAY(-4); фон «забыли понизить» (0) vs «понизили до BACKGROUND» (10)
        assertEquals(71 to 96, SUT.uiShareWithAndWithoutNicing(uiNice = -4, bgNiceWrong = 0, bgNiceRight = 10))
    }

    @Test fun `СЛ17 rankByEffectiveShare`() {
        val specs = listOf(
            spec("bgFg", 0, Cgroup.BACKGROUND),   // nice0 но background cgroup → мало
            spec("ui", -4, Cgroup.FOREGROUND),    // самый большой
            spec("norm", 0, Cgroup.FOREGROUND),
        )
        assertEquals(listOf("ui", "norm", "bgFg"), SUT.rankByEffectiveShare(specs))
    }

    @Test fun `СЛ18 cfsIsFair`() {
        val specs = listOf(spec("A", 0), spec("B", 2), spec("C", 4))
        assertTrue(SUT.cfsIsFair(specs, quanta = 1000, tolerancePercent = 5))
    }

    @Test fun `СЛ19 transitiveInheritedNice`() {
        // L=5 держит лок для M=0, M держит лок для H=-8 → эффективный = -8
        assertEquals(-8, SUT.transitiveInheritedNice(listOf(5, 0, -8)))
    }

    @Test fun `СЛ20 schedulingClasses — RT вытесняет обычные`() {
        val specs = listOf(spec("normalHi", -4), spec("audio", 0, rt = true), spec("normalLo", 5))
        assertEquals(List(5) { "audio" }, SUT.schedulingClasses(specs, 5))
    }

    @Test fun `СЛ20 schedulingClasses — без RT это CFS`() {
        val specs = listOf(spec("A", 0), spec("B", 0))
        assertEquals(listOf("A", "B", "A", "B"), SUT.schedulingClasses(specs, 4))
    }
}
