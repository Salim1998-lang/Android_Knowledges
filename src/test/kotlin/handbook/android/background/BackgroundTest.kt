package handbook.android.background

import handbook.android.background.solutions.BackgroundSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 8 «Фоновое выполнение и гарантии». Детерминированная модель планировщиков Android:
 * гарантии выживания, Doze/App Standby, constraints, backoff/ретраи, foreground service, уникальность
 * и цепочки работ, коалесинг будильников.
 *
 * Чтобы гонять против эталона — замени `SUT` на [BackgroundSolutions].
 */
private typealias SUT = BackgroundTasks

class BackgroundTest {

    // ── Лёгкие ──

    @Test fun `Л1 survivesReboot`() {
        assertTrue(SUT.survivesReboot(BackgroundApi.WORK_MANAGER))
        assertTrue(SUT.survivesReboot(BackgroundApi.JOB_SCHEDULER))
        assertFalse(SUT.survivesReboot(BackgroundApi.RAW_THREAD))
        assertFalse(SUT.survivesReboot(BackgroundApi.FOREGROUND_SERVICE))
        assertFalse(SUT.survivesReboot(BackgroundApi.ALARM_MANAGER))
    }

    @Test fun `Л2 workerRunsOnBackgroundThread`() {
        assertTrue(SUT.workerRunsOnBackgroundThread())
    }

    @Test fun `Л3 foregroundServiceNeedsNotification`() {
        assertTrue(SUT.foregroundServiceNeedsNotification())
    }

    @Test fun `Л4 deferredInDoze`() {
        assertTrue(SUT.deferredInDoze(isForegroundService = false))
        assertFalse(SUT.deferredInDoze(isForegroundService = true))
    }

    @Test fun `Л5 constraintsSatisfied`() {
        val c = Constraints(requiresNetwork = true, requiresCharging = true)
        assertTrue(SUT.constraintsSatisfied(c, DeviceState(hasNetwork = true, charging = true)))
        assertFalse(SUT.constraintsSatisfied(c, DeviceState(hasNetwork = true, charging = false)))
    }

    @Test fun `Л6 willReRun`() {
        assertTrue(SUT.willReRun(WorkResult.RETRY))
        assertFalse(SUT.willReRun(WorkResult.SUCCESS))
        assertFalse(SUT.willReRun(WorkResult.FAILURE))
    }

    @Test fun `Л7 keepIgnoresNewWork`() {
        assertTrue(SUT.keepIgnoresNewWork(ExistingWorkPolicy.KEEP))
        assertFalse(SUT.keepIgnoresNewWork(ExistingWorkPolicy.REPLACE))
        assertFalse(SUT.keepIgnoresNewWork(ExistingWorkPolicy.APPEND))
    }

    @Test fun `Л8 rawThreadUnreliable`() {
        assertTrue(SUT.rawThreadUnreliable(BackgroundApi.RAW_THREAD))
        assertFalse(SUT.rawThreadUnreliable(BackgroundApi.WORK_MANAGER))
    }

    // ── Средние ──

    @Test fun `С9 backoffDelayMs`() {
        // LINEAR: initial * attempt
        assertEquals(30_000, SUT.backoffDelayMs(BackoffPolicy.LINEAR, 10_000, 3))
        // EXPONENTIAL: initial * 2^(attempt-1)
        assertEquals(10_000, SUT.backoffDelayMs(BackoffPolicy.EXPONENTIAL, 10_000, 1))
        assertEquals(40_000, SUT.backoffDelayMs(BackoffPolicy.EXPONENTIAL, 10_000, 3))
        // нижняя граница: 1_000 * 1 < MIN → MIN
        assertEquals(WorkLimits.MIN_BACKOFF_MS, SUT.backoffDelayMs(BackoffPolicy.LINEAR, 1_000, 1))
        // верхняя граница: экспонента упирается в MAX
        assertEquals(WorkLimits.MAX_BACKOFF_MS, SUT.backoffDelayMs(BackoffPolicy.EXPONENTIAL, 10_000, 20))
    }

    @Test fun `С10 effectiveDelayMinutes`() {
        assertEquals(1440, SUT.effectiveDelayMinutes(StandbyBucket.RARE, 10))
        assertEquals(30, SUT.effectiveDelayMinutes(StandbyBucket.ACTIVE, 30))
        assertEquals(120, SUT.effectiveDelayMinutes(StandbyBucket.WORKING_SET, 5))
    }

    @Test fun `С11 unmetConstraints`() {
        val c = Constraints(
            requiresNetwork = true,
            requiresCharging = true,
            requiresStorageNotLow = true,
        )
        val d = DeviceState(hasNetwork = false, charging = true, storageLow = true)
        assertEquals(listOf("network", "storageNotLow"), SUT.unmetConstraints(c, d))
        assertEquals(emptyList<String>(), SUT.unmetConstraints(Constraints(), DeviceState()))
    }

    @Test fun `С12 childStateForParent`() {
        assertEquals(WorkState.ENQUEUED, SUT.childStateForParent(WorkState.SUCCEEDED))
        assertEquals(WorkState.CANCELLED, SUT.childStateForParent(WorkState.FAILED))
        assertEquals(WorkState.CANCELLED, SUT.childStateForParent(WorkState.CANCELLED))
        assertEquals(WorkState.BLOCKED, SUT.childStateForParent(WorkState.RUNNING))
    }

    @Test fun `С13 enqueuesNewWork`() {
        assertTrue(SUT.enqueuesNewWork(ExistingWorkPolicy.REPLACE, hasExisting = true))
        assertFalse(SUT.enqueuesNewWork(ExistingWorkPolicy.KEEP, hasExisting = true))
        assertTrue(SUT.enqueuesNewWork(ExistingWorkPolicy.KEEP, hasExisting = false))
        assertTrue(SUT.enqueuesNewWork(ExistingWorkPolicy.APPEND, hasExisting = true))
    }

    @Test fun `С14 expeditedFallsBackToRegular`() {
        assertTrue(SUT.expeditedFallsBackToRegular(usedQuotaMs = 9_000, requestMs = 2_000, quotaMs = 10_000))
        assertFalse(SUT.expeditedFallsBackToRegular(usedQuotaMs = 3_000, requestMs = 2_000, quotaMs = 10_000))
    }

    @Test fun `С15 jobSchedulerOverLimit`() {
        assertTrue(SUT.jobSchedulerOverLimit(101))
        assertFalse(SUT.jobSchedulerOverLimit(100))
        assertFalse(SUT.jobSchedulerOverLimit(50))
    }

    // ── Сложные ──

    @Test fun `СЛ16 totalBackoffMs`() {
        // LINEAR: 10000 + 20000 + 30000
        assertEquals(60_000, SUT.totalBackoffMs(BackoffPolicy.LINEAR, 10_000, 3))
        // EXPONENTIAL: 10000 + 20000 + 40000
        assertEquals(70_000, SUT.totalBackoffMs(BackoffPolicy.EXPONENTIAL, 10_000, 3))
    }

    @Test fun `СЛ17 firstTickAllConstraintsMet`() {
        val c = Constraints(requiresNetwork = true, requiresCharging = true)
        val timeline = listOf(
            DeviceState(hasNetwork = false, charging = false),
            DeviceState(hasNetwork = true, charging = false),
            DeviceState(hasNetwork = true, charging = true),   // здесь сходятся все
        )
        assertEquals(2, SUT.firstTickAllConstraintsMet(c, timeline))
        assertEquals(-1, SUT.firstTickAllConstraintsMet(c, timeline.take(2)))
    }

    @Test fun `СЛ18 runsUntilSuccess`() {
        assertEquals(3, SUT.runsUntilSuccess(listOf(WorkResult.RETRY, WorkResult.RETRY, WorkResult.SUCCESS)))
        assertEquals(1, SUT.runsUntilSuccess(listOf(WorkResult.SUCCESS)))
        assertEquals(-1, SUT.runsUntilSuccess(listOf(WorkResult.RETRY, WorkResult.FAILURE)))
        assertEquals(-1, SUT.runsUntilSuccess(listOf(WorkResult.RETRY, WorkResult.RETRY)))
    }

    @Test fun `СЛ19 coalescedWakeups`() {
        assertEquals(1, SUT.coalescedWakeups(listOf(0, 1_000, 2_000), windowMs = 5_000))
        assertEquals(2, SUT.coalescedWakeups(listOf(0, 6_000, 7_000), windowMs = 5_000))
        assertEquals(0, SUT.coalescedWakeups(emptyList(), windowMs = 5_000))
        // несортированный вход
        assertEquals(2, SUT.coalescedWakeups(listOf(7_000, 0, 6_000), windowMs = 5_000))
    }

    @Test fun `СЛ20 cancelledAfterFailure`() {
        assertEquals(2, SUT.cancelledAfterFailure(
            listOf(WorkResult.SUCCESS, WorkResult.FAILURE, WorkResult.SUCCESS, WorkResult.SUCCESS)))
        assertEquals(0, SUT.cancelledAfterFailure(listOf(WorkResult.SUCCESS, WorkResult.SUCCESS)))
        assertEquals(1, SUT.cancelledAfterFailure(listOf(WorkResult.FAILURE, WorkResult.SUCCESS)))
    }
}
