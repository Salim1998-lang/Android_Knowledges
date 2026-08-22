package handbook.android.lifecycle

import handbook.android.lifecycle.solutions.LifecycleSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 9 «Потоки и жизненный цикл». Детерминированная модель связки фонового потока и жизненного
 * цикла UI-компонента: доставка результата на главный поток с проверкой состояния, отмена при
 * уничтожении, config change (Activity vs ViewModel), утечки Handler, семантика LiveData, repeatOnLifecycle.
 *
 * Чтобы гонять против эталона — замени `SUT` на [LifecycleSolutions].
 */
private typealias SUT = LifecycleTasks

class LifecycleTest {

    // ── Лёгкие ──

    @Test fun `Л1 isAtLeastStarted`() {
        assertTrue(SUT.isAtLeastStarted(LifecycleState.STARTED))
        assertTrue(SUT.isAtLeastStarted(LifecycleState.RESUMED))
        assertFalse(SUT.isAtLeastStarted(LifecycleState.CREATED))
        assertFalse(SUT.isAtLeastStarted(LifecycleState.DESTROYED))
    }

    @Test fun `Л2 mustPostToMain`() {
        assertTrue(SUT.mustPostToMain(onMainThread = false))
        assertFalse(SUT.mustPostToMain(onMainThread = true))
    }

    @Test fun `Л3 isDestroyed`() {
        assertTrue(SUT.isDestroyed(LifecycleState.DESTROYED))
        assertFalse(SUT.isDestroyed(LifecycleState.RESUMED))
    }

    @Test fun `Л4 canUseSetValue`() {
        assertTrue(SUT.canUseSetValue(onMainThread = true))
        assertFalse(SUT.canUseSetValue(onMainThread = false))
    }

    @Test fun `Л5 viewModelSurvivesConfigChange`() {
        assertTrue(SUT.viewModelSurvivesConfigChange())
    }

    @Test fun `Л6 activityRecreatedOnConfigChange`() {
        assertTrue(SUT.activityRecreatedOnConfigChange())
    }

    @Test fun `Л7 handlerLeaksActivity`() {
        assertTrue(SUT.handlerLeaksActivity(isStatic = false, hasPendingMessages = true))
        assertFalse(SUT.handlerLeaksActivity(isStatic = true, hasPendingMessages = true))
        assertFalse(SUT.handlerLeaksActivity(isStatic = false, hasPendingMessages = false))
    }

    @Test fun `Л8 postValueKeepsLatestOnly`() {
        assertTrue(SUT.postValueKeepsLatestOnly())
    }

    // ── Средние ──

    @Test fun `С9 deliverAction`() {
        assertEquals(DeliverAction.DROP, SUT.deliverAction(onMainThread = true, LifecycleState.DESTROYED))
        assertEquals(DeliverAction.DROP, SUT.deliverAction(onMainThread = false, LifecycleState.DESTROYED))
        assertEquals(DeliverAction.POST_TO_MAIN, SUT.deliverAction(onMainThread = false, LifecycleState.STARTED))
        assertEquals(DeliverAction.DELIVER, SUT.deliverAction(onMainThread = true, LifecycleState.RESUMED))
    }

    @Test fun `С10 handlerCleanupNeeded`() {
        assertTrue(SUT.handlerCleanupNeeded(hasPendingMessages = true, LifecycleState.DESTROYED))
        assertFalse(SUT.handlerCleanupNeeded(hasPendingMessages = false, LifecycleState.DESTROYED))
        assertFalse(SUT.handlerCleanupNeeded(hasPendingMessages = true, LifecycleState.STARTED))
    }

    @Test fun `С11 configChangeLosesWork`() {
        assertTrue(SUT.configChangeLosesWork(usesViewModel = false))
        assertFalse(SUT.configChangeLosesWork(usesViewModel = true))
    }

    @Test fun `С12 liveDataUpdateMethod`() {
        assertEquals("setValue", SUT.liveDataUpdateMethod(onMainThread = true))
        assertEquals("postValue", SUT.liveDataUpdateMethod(onMainThread = false))
    }

    @Test fun `С13 scopeCancelledByConfigChange`() {
        assertTrue(SUT.scopeCancelledByConfigChange(ScopeKind.LIFECYCLE))
        assertFalse(SUT.scopeCancelledByConfigChange(ScopeKind.VIEW_MODEL))
    }

    @Test fun `С14 repeatOnLifecycleActive`() {
        assertTrue(SUT.repeatOnLifecycleActive(LifecycleState.STARTED))
        assertTrue(SUT.repeatOnLifecycleActive(LifecycleState.RESUMED))
        assertFalse(SUT.repeatOnLifecycleActive(LifecycleState.CREATED))
    }

    @Test fun `С15 weakRefCallbackFires`() {
        assertTrue(SUT.weakRefCallbackFires(referentAlive = true))
        assertFalse(SUT.weakRefCallbackFires(referentAlive = false))
    }

    // ── Сложные ──

    @Test fun `СЛ16 lastObservedValue`() {
        // значение установлено пока неактивен → доставится при активации (sticky)
        assertEquals(1, SUT.lastObservedValue(listOf(
            LiveEvent.SetValue(1), LiveEvent.SetActive(true))))
        // активные обновления доставляются сразу, видно последнее
        assertEquals(6, SUT.lastObservedValue(listOf(
            LiveEvent.SetActive(true), LiveEvent.SetValue(5), LiveEvent.SetValue(6))))
        // неактивные обновления коалесируются → последнее
        assertEquals(2, SUT.lastObservedValue(listOf(
            LiveEvent.SetValue(1), LiveEvent.SetValue(2), LiveEvent.SetActive(true))))
        // значение пришло после деактивации → не доставлено
        assertNull(SUT.lastObservedValue(listOf(
            LiveEvent.SetActive(true), LiveEvent.SetActive(false), LiveEvent.SetValue(9))))
    }

    @Test fun `СЛ17 handlerLeakWindowMs`() {
        assertEquals(8_000, SUT.handlerLeakWindowMs(postDelayMs = 10_000, destroyAtMs = 2_000, cleanedOnDestroy = false))
        assertEquals(0, SUT.handlerLeakWindowMs(postDelayMs = 10_000, destroyAtMs = 2_000, cleanedOnDestroy = true))
        assertEquals(0, SUT.handlerLeakWindowMs(postDelayMs = 1_000, destroyAtMs = 2_000, cleanedOnDestroy = false))
    }

    @Test fun `СЛ18 collectionRestarts`() {
        assertEquals(2, SUT.collectionRestarts(listOf(
            LifecycleState.CREATED, LifecycleState.STARTED, LifecycleState.CREATED, LifecycleState.STARTED)))
        assertEquals(1, SUT.collectionRestarts(listOf(LifecycleState.STARTED, LifecycleState.RESUMED)))
        assertEquals(0, SUT.collectionRestarts(listOf(LifecycleState.CREATED, LifecycleState.DESTROYED)))
    }

    @Test fun `СЛ19 lifecycleScopeDeliversResult`() {
        assertTrue(SUT.lifecycleScopeDeliversResult(workDurationMs = 100, destroyAtMs = 500))
        assertFalse(SUT.lifecycleScopeDeliversResult(workDurationMs = 600, destroyAtMs = 500))
    }

    @Test fun `СЛ20 staleCallbacksAfterDestroy`() {
        assertEquals(2, SUT.staleCallbacksAfterDestroy(listOf(100, 200, 300), destroyAtMs = 150, cleaned = false))
        assertEquals(0, SUT.staleCallbacksAfterDestroy(listOf(100, 200, 300), destroyAtMs = 150, cleaned = true))
        assertEquals(0, SUT.staleCallbacksAfterDestroy(listOf(100, 120), destroyAtMs = 150, cleaned = false))
    }
}
