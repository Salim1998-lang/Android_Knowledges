package handbook.android.background.solutions

import handbook.android.background.BackgroundApi
import handbook.android.background.BackoffPolicy
import handbook.android.background.Constraints
import handbook.android.background.DeviceState
import handbook.android.background.ExistingWorkPolicy
import handbook.android.background.StandbyBucket
import handbook.android.background.WorkLimits
import handbook.android.background.WorkResult
import handbook.android.background.WorkState

/** Эталонные решения темы 8. Подсмотри, если застрял с [handbook.android.background.BackgroundTasks]. */
object BackgroundSolutions {

    // ── Лёгкие ──

    fun survivesReboot(api: BackgroundApi): Boolean =
        api == BackgroundApi.WORK_MANAGER || api == BackgroundApi.JOB_SCHEDULER

    fun workerRunsOnBackgroundThread(): Boolean = true

    fun foregroundServiceNeedsNotification(): Boolean = true

    fun deferredInDoze(isForegroundService: Boolean): Boolean = !isForegroundService

    fun constraintsSatisfied(constraints: Constraints, device: DeviceState): Boolean =
        unmetConstraints(constraints, device).isEmpty()

    fun willReRun(result: WorkResult): Boolean = result == WorkResult.RETRY

    fun keepIgnoresNewWork(policy: ExistingWorkPolicy): Boolean = policy == ExistingWorkPolicy.KEEP

    fun rawThreadUnreliable(api: BackgroundApi): Boolean = api == BackgroundApi.RAW_THREAD

    // ── Средние ──

    fun backoffDelayMs(policy: BackoffPolicy, initialMs: Long, attempt: Int): Long {
        val raw = when (policy) {
            BackoffPolicy.LINEAR -> initialMs * attempt
            BackoffPolicy.EXPONENTIAL -> initialMs shl (attempt - 1)  // initialMs * 2^(attempt-1)
        }
        return raw.coerceIn(WorkLimits.MIN_BACKOFF_MS, WorkLimits.MAX_BACKOFF_MS)
    }

    fun effectiveDelayMinutes(bucket: StandbyBucket, requestedMinutes: Long): Long =
        maxOf(requestedMinutes, bucket.deferMinutes)

    fun unmetConstraints(constraints: Constraints, device: DeviceState): List<String> = buildList {
        if (constraints.requiresNetwork && !device.hasNetwork) add("network")
        if (constraints.requiresUnmeteredNetwork && !(device.hasNetwork && device.networkUnmetered)) add("unmeteredNetwork")
        if (constraints.requiresCharging && !device.charging) add("charging")
        if (constraints.requiresBatteryNotLow && device.batteryLow) add("batteryNotLow")
        if (constraints.requiresIdle && !device.idle) add("idle")
        if (constraints.requiresStorageNotLow && device.storageLow) add("storageNotLow")
    }

    fun childStateForParent(parent: WorkState): WorkState = when (parent) {
        WorkState.SUCCEEDED -> WorkState.ENQUEUED
        WorkState.FAILED, WorkState.CANCELLED -> WorkState.CANCELLED
        else -> WorkState.BLOCKED
    }

    fun enqueuesNewWork(policy: ExistingWorkPolicy, hasExisting: Boolean): Boolean = when (policy) {
        ExistingWorkPolicy.REPLACE -> true
        ExistingWorkPolicy.KEEP -> !hasExisting
        ExistingWorkPolicy.APPEND -> true
    }

    fun expeditedFallsBackToRegular(usedQuotaMs: Long, requestMs: Long, quotaMs: Long): Boolean =
        usedQuotaMs + requestMs > quotaMs

    fun jobSchedulerOverLimit(scheduledJobs: Int): Boolean = scheduledJobs > WorkLimits.JOB_SCHEDULER_MAX_JOBS

    // ── Сложные ──

    fun totalBackoffMs(policy: BackoffPolicy, initialMs: Long, attempts: Int): Long =
        (1..attempts).sumOf { backoffDelayMs(policy, initialMs, it) }

    fun firstTickAllConstraintsMet(constraints: Constraints, timeline: List<DeviceState>): Int =
        timeline.indexOfFirst { constraintsSatisfied(constraints, it) }

    fun runsUntilSuccess(results: List<WorkResult>): Int {
        var runs = 0
        for (r in results) {
            runs++
            when (r) {
                WorkResult.SUCCESS -> return runs
                WorkResult.FAILURE -> return -1
                WorkResult.RETRY -> Unit
            }
        }
        return -1  // все RETRY, успеха не было
    }

    fun coalescedWakeups(alarmTimesMs: List<Long>, windowMs: Long): Int {
        if (alarmTimesMs.isEmpty()) return 0
        val sorted = alarmTimesMs.sorted()
        var batches = 1
        var windowEnd = sorted.first() + windowMs
        for (t in sorted.drop(1)) {
            if (t >= windowEnd) {          // вне текущего окна → новый батч
                batches++
                windowEnd = t + windowMs
            }
        }
        return batches
    }

    fun cancelledAfterFailure(nodeResults: List<WorkResult>): Int {
        val idx = nodeResults.indexOfFirst { it == WorkResult.FAILURE }
        return if (idx < 0) 0 else nodeResults.size - idx - 1
    }
}
