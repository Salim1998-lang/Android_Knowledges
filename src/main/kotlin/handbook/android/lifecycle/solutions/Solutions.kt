package handbook.android.lifecycle.solutions

import handbook.android.lifecycle.DeliverAction
import handbook.android.lifecycle.LifecycleState
import handbook.android.lifecycle.LiveEvent
import handbook.android.lifecycle.ScopeKind
import handbook.android.lifecycle.isAtLeast

/** Эталонные решения темы 9. Подсмотри, если застрял с [handbook.android.lifecycle.LifecycleTasks]. */
object LifecycleSolutions {

    // ── Лёгкие ──

    fun isAtLeastStarted(state: LifecycleState): Boolean = state.isAtLeast(LifecycleState.STARTED)

    fun mustPostToMain(onMainThread: Boolean): Boolean = !onMainThread

    fun isDestroyed(state: LifecycleState): Boolean = state == LifecycleState.DESTROYED

    fun canUseSetValue(onMainThread: Boolean): Boolean = onMainThread

    fun viewModelSurvivesConfigChange(): Boolean = true

    fun activityRecreatedOnConfigChange(): Boolean = true

    fun handlerLeaksActivity(isStatic: Boolean, hasPendingMessages: Boolean): Boolean =
        !isStatic && hasPendingMessages

    fun postValueKeepsLatestOnly(): Boolean = true

    // ── Средние ──

    fun deliverAction(onMainThread: Boolean, state: LifecycleState): DeliverAction = when {
        state == LifecycleState.DESTROYED -> DeliverAction.DROP
        !onMainThread -> DeliverAction.POST_TO_MAIN
        else -> DeliverAction.DELIVER
    }

    fun handlerCleanupNeeded(hasPendingMessages: Boolean, state: LifecycleState): Boolean =
        state == LifecycleState.DESTROYED && hasPendingMessages

    fun configChangeLosesWork(usesViewModel: Boolean): Boolean = !usesViewModel

    fun liveDataUpdateMethod(onMainThread: Boolean): String = if (onMainThread) "setValue" else "postValue"

    fun scopeCancelledByConfigChange(scope: ScopeKind): Boolean = scope == ScopeKind.LIFECYCLE

    fun repeatOnLifecycleActive(state: LifecycleState): Boolean = state.isAtLeast(LifecycleState.STARTED)

    fun weakRefCallbackFires(referentAlive: Boolean): Boolean = referentAlive

    // ── Сложные ──

    fun lastObservedValue(events: List<LiveEvent>): Int? {
        var lastValue: Int? = null
        var active = false
        var deliveredCurrent = true   // нечего доставлять, пока не было значения
        var lastDelivered: Int? = null
        for (event in events) when (event) {
            is LiveEvent.SetValue -> {
                lastValue = event.value
                deliveredCurrent = false
                if (active) { lastDelivered = event.value; deliveredCurrent = true }
            }
            is LiveEvent.SetActive -> {
                active = event.active
                if (active && !deliveredCurrent) {   // sticky: доставить текущее при активации
                    lastDelivered = lastValue
                    deliveredCurrent = true
                }
            }
        }
        return lastDelivered
    }

    fun handlerLeakWindowMs(postDelayMs: Long, destroyAtMs: Long, cleanedOnDestroy: Boolean): Long = when {
        cleanedOnDestroy -> 0L
        postDelayMs > destroyAtMs -> postDelayMs - destroyAtMs
        else -> 0L
    }

    fun collectionRestarts(states: List<LifecycleState>): Int {
        var restarts = 0
        var wasActive = false
        for (state in states) {
            val active = state.isAtLeast(LifecycleState.STARTED)
            if (active && !wasActive) restarts++
            wasActive = active
        }
        return restarts
    }

    fun lifecycleScopeDeliversResult(workDurationMs: Long, destroyAtMs: Long): Boolean =
        workDurationMs <= destroyAtMs

    fun staleCallbacksAfterDestroy(callbackTimesMs: List<Long>, destroyAtMs: Long, cleaned: Boolean): Int =
        if (cleaned) 0 else callbackTimesMs.count { it >= destroyAtMs }
}
