package handbook.android.diagnostics

import handbook.android.diagnostics.solutions.DiagnosticsSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 10 «Диагностика многопоточности». Детерминированная модель навыков анализа: StrictMode,
 * чтение thread dump / ANR-трейса (состояния, держатели и ожидатели локов), поиск дедлока как цикла в
 * графе waits-for, раскрутка корня ANR по цепочке блокировок, трассы (баланс/глубина/self-time секций).
 *
 * Чтобы гонять против эталона — замени `SUT` на [DiagnosticsSolutions].
 */
private typealias SUT = DiagnosticsTasks

class DiagnosticsTest {

    // ── Лёгкие ──

    @Test fun `Л1 mainThreadViolation`() {
        assertTrue(SUT.mainThreadViolation(MainOp.DISK_READ))
        assertTrue(SUT.mainThreadViolation(MainOp.NETWORK))
        assertFalse(SUT.mainThreadViolation(MainOp.CPU))
    }

    @Test fun `Л2 penaltyCrashesApp`() {
        assertTrue(SUT.penaltyCrashesApp(StrictPenalty.DEATH))
        assertFalse(SUT.penaltyCrashesApp(StrictPenalty.LOG))
    }

    @Test fun `Л3 blockedWaitsForMonitor`() {
        assertTrue(SUT.blockedWaitsForMonitor(ThreadState.BLOCKED))
        assertFalse(SUT.blockedWaitsForMonitor(ThreadState.RUNNABLE))
    }

    @Test fun `Л4 mainResponsive`() {
        assertTrue(SUT.mainResponsive(mainBlockedMs = 200, thresholdMs = 5_000))
        assertFalse(SUT.mainResponsive(mainBlockedMs = 6_000, thresholdMs = 5_000))
    }

    @Test fun `Л5 runnableIsRunning`() {
        assertTrue(SUT.runnableIsRunning(ThreadState.RUNNABLE))
        assertFalse(SUT.runnableIsRunning(ThreadState.WAITING))
    }

    @Test fun `Л6 unclosedResourceViolation`() {
        assertTrue(SUT.unclosedResourceViolation(closed = false))
        assertFalse(SUT.unclosedResourceViolation(closed = true))
    }

    @Test fun `Л7 deadlockRequiresCycle`() {
        assertTrue(SUT.deadlockRequiresCycle())
    }

    @Test fun `Л8 anrTraceStartsAtMain`() {
        assertTrue(SUT.anrTraceStartsAtMain())
    }

    // ── Средние ──

    @Test fun `С9 traceSectionsBalanced`() {
        assertTrue(SUT.traceSectionsBalanced(listOf(
            TraceEvent.Begin("frame"), TraceEvent.Begin("layout"), TraceEvent.End(), TraceEvent.End())))
        assertFalse(SUT.traceSectionsBalanced(listOf(TraceEvent.Begin("frame"))))          // не закрыта
        assertFalse(SUT.traceSectionsBalanced(listOf(TraceEvent.End())))                    // лишний End
    }

    @Test fun `С10 lockHolderOf`() {
        val dump = listOf(
            ThreadInfo("main", ThreadState.BLOCKED, waitsForLock = "A"),
            ThreadInfo("worker", ThreadState.RUNNABLE, holdsLocks = listOf("A")),
        )
        assertEquals("worker", SUT.lockHolderOf(dump, "A"))
        assertNull(SUT.lockHolderOf(dump, "B"))
    }

    @Test fun `С11 blockedThreadCount`() {
        val dump = listOf(
            ThreadInfo("t1", ThreadState.BLOCKED),
            ThreadInfo("t2", ThreadState.BLOCKED),
            ThreadInfo("t3", ThreadState.RUNNABLE),
        )
        assertEquals(2, SUT.blockedThreadCount(dump))
    }

    @Test fun `С12 maxTraceDepth`() {
        assertEquals(2, SUT.maxTraceDepth(listOf(
            TraceEvent.Begin("a"), TraceEvent.Begin("b"), TraceEvent.End(), TraceEvent.End())))
        assertEquals(1, SUT.maxTraceDepth(listOf(
            TraceEvent.Begin("a"), TraceEvent.End(), TraceEvent.Begin("b"), TraceEvent.End())))
        assertEquals(0, SUT.maxTraceDepth(emptyList()))
    }

    @Test fun `С13 anrCulpritThread`() {
        val dump = listOf(
            ThreadInfo("main", ThreadState.BLOCKED, waitsForLock = "db"),
            ThreadInfo("io", ThreadState.RUNNABLE, holdsLocks = listOf("db")),
        )
        assertEquals("io", SUT.anrCulpritThread(dump, "main"))
        assertNull(SUT.anrCulpritThread(
            listOf(ThreadInfo("main", ThreadState.RUNNABLE)), "main"))   // main ничего не ждёт
    }

    @Test fun `С14 effectivePenalty`() {
        assertEquals(StrictPenalty.DEATH, SUT.effectivePenalty(setOf(StrictPenalty.LOG, StrictPenalty.DEATH)))
        assertEquals(StrictPenalty.DROPBOX, SUT.effectivePenalty(setOf(StrictPenalty.LOG, StrictPenalty.DROPBOX)))
        assertEquals(StrictPenalty.LOG, SUT.effectivePenalty(emptySet()))
    }

    @Test fun `С15 strictModeViolationCount`() {
        assertEquals(2, SUT.strictModeViolationCount(listOf(MainOp.CPU, MainOp.DISK_READ, MainOp.NETWORK)))
        assertEquals(0, SUT.strictModeViolationCount(listOf(MainOp.CPU, MainOp.CPU)))
    }

    // ── Сложные ──

    @Test fun `СЛ16 detectDeadlock`() {
        // A держит L1 ждёт L2; B держит L2 ждёт L1 → дедлок
        val deadlocked = listOf(
            ThreadInfo("A", ThreadState.BLOCKED, holdsLocks = listOf("L1"), waitsForLock = "L2"),
            ThreadInfo("B", ThreadState.BLOCKED, holdsLocks = listOf("L2"), waitsForLock = "L1"),
        )
        assertTrue(SUT.detectDeadlock(deadlocked))
        // A ждёт L1 у B, B ничего не ждёт → нет цикла
        val healthy = listOf(
            ThreadInfo("A", ThreadState.BLOCKED, waitsForLock = "L1"),
            ThreadInfo("B", ThreadState.RUNNABLE, holdsLocks = listOf("L1")),
        )
        assertFalse(SUT.detectDeadlock(healthy))
    }

    @Test fun `СЛ17 deadlockedThreads`() {
        val dump = listOf(
            ThreadInfo("A", ThreadState.BLOCKED, holdsLocks = listOf("L1"), waitsForLock = "L2"),
            ThreadInfo("B", ThreadState.BLOCKED, holdsLocks = listOf("L2"), waitsForLock = "L1"),
            ThreadInfo("C", ThreadState.BLOCKED, waitsForLock = "L1"),   // упирается в цикл, но сам не на нём
        )
        assertEquals(listOf("A", "B"), SUT.deadlockedThreads(dump))
        assertEquals(emptyList<String>(), SUT.deadlockedThreads(listOf(
            ThreadInfo("A", ThreadState.BLOCKED, waitsForLock = "L1"),
            ThreadInfo("B", ThreadState.RUNNABLE, holdsLocks = listOf("L1")),
        )))
    }

    @Test fun `СЛ18 sectionSelfTimeMs`() {
        // frame [0..20], внутри layout [5..10] → self(frame)=15, self(layout)=5
        val events = listOf(
            TraceEvent.Begin("frame", 0),
            TraceEvent.Begin("layout", 5),
            TraceEvent.End(10),
            TraceEvent.End(20),
        )
        assertEquals(15, SUT.sectionSelfTimeMs(events, "frame"))
        assertEquals(5, SUT.sectionSelfTimeMs(events, "layout"))
        assertEquals(0, SUT.sectionSelfTimeMs(events, "missing"))
    }

    @Test fun `СЛ19 anrRootBlocker`() {
        // main ждёт X (у worker), worker ждёт Y (у db), db работает → корень db
        val dump = listOf(
            ThreadInfo("main", ThreadState.BLOCKED, waitsForLock = "X"),
            ThreadInfo("worker", ThreadState.BLOCKED, holdsLocks = listOf("X"), waitsForLock = "Y"),
            ThreadInfo("db", ThreadState.RUNNABLE, holdsLocks = listOf("Y")),
        )
        assertEquals("db", SUT.anrRootBlocker(dump, "main"))
        assertNull(SUT.anrRootBlocker(listOf(ThreadInfo("main", ThreadState.RUNNABLE)), "main"))
    }

    @Test fun `СЛ20 longestBlockChainLength`() {
        // A→B→C цепочка из 3 потоков
        val dump = listOf(
            ThreadInfo("A", ThreadState.BLOCKED, waitsForLock = "L1"),
            ThreadInfo("B", ThreadState.BLOCKED, holdsLocks = listOf("L1"), waitsForLock = "L2"),
            ThreadInfo("C", ThreadState.RUNNABLE, holdsLocks = listOf("L2")),
        )
        assertEquals(3, SUT.longestBlockChainLength(dump))
        assertEquals(1, SUT.longestBlockChainLength(listOf(
            ThreadInfo("solo", ThreadState.RUNNABLE))))
        assertEquals(0, SUT.longestBlockChainLength(emptyList()))
    }
}
