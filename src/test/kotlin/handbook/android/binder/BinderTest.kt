package handbook.android.binder

import handbook.android.binder.solutions.BinderSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 7 «Binder и IPC-треды». Детерминированная модель threading-модели IPC: пул Binder-тредов,
 * sync vs oneway, поток исполнения колбэков, реентрантность и её дедлоки, наследование приоритета,
 * буфер транзакций, межпроцессный дедлок (цикл sync-вызовов).
 *
 * Чтобы гонять против эталона — замени `SUT` на [BinderSolutions].
 */
private typealias SUT = BinderTasks

class BinderTest {

    // ── Лёгкие ──

    @Test fun `Л1 binderPoolMax`() {
        assertEquals(16, SUT.binderPoolMax())
    }

    @Test fun `Л2 incomingCallThread — не главный`() {
        assertEquals(ThreadKind.BINDER, SUT.incomingCallThread())
    }

    @Test fun `Л3 callbackNeedsMainPost`() {
        assertTrue(SUT.callbackNeedsMainPost(touchesUi = true))
        assertFalse(SUT.callbackNeedsMainPost(touchesUi = false))
    }

    @Test fun `Л4 syncBlocksCaller`() {
        assertTrue(SUT.syncBlocksCaller(TransactionKind.SYNC))
        assertFalse(SUT.syncBlocksCaller(TransactionKind.ONEWAY))
    }

    @Test fun `Л5 onewayReturnsValue`() {
        assertFalse(SUT.onewayReturnsValue())
    }

    @Test fun `Л6 transactionTooLarge`() {
        assertTrue(SUT.transactionTooLarge(2 * 1024 * 1024))
        assertFalse(SUT.transactionTooLarge(1024))
    }

    @Test fun `Л7 totalBufferExceeded`() {
        assertTrue(SUT.totalBufferExceeded(listOf(600 * 1024, 600 * 1024)))
        assertFalse(SUT.totalBufferExceeded(listOf(100 * 1024, 100 * 1024)))
    }

    @Test fun `Л8 deadObjectOnDeath`() {
        assertTrue(SUT.deadObjectOnDeath(remoteAlive = false))
        assertFalse(SUT.deadObjectOnDeath(remoteAlive = true))
    }

    // ── Средние ──

    @Test fun `С9 concurrentTransactions`() {
        assertEquals(16, SUT.concurrentTransactions(incoming = 30, poolSize = 16))
        assertEquals(5, SUT.concurrentTransactions(incoming = 5, poolSize = 16))
    }

    @Test fun `С10 poolExhausted`() {
        assertTrue(SUT.poolExhausted(incoming = 30, poolSize = 16))
        assertFalse(SUT.poolExhausted(incoming = 10, poolSize = 16))
    }

    @Test fun `С11 onewaySerialized`() {
        val txns = listOf(
            Transaction("a", TransactionKind.ONEWAY),
            Transaction("b", TransactionKind.ONEWAY),
            Transaction("c", TransactionKind.ONEWAY),
        )
        assertEquals(listOf("a", "b", "c"), SUT.onewaySerialized(txns))
    }

    @Test fun `С12 mainBlockedMs`() {
        assertEquals(200, SUT.mainBlockedMs(200, TransactionKind.SYNC))
        assertEquals(0, SUT.mainBlockedMs(200, TransactionKind.ONEWAY))
    }

    @Test fun `С13 blockingCallAnr`() {
        assertTrue(SUT.blockingCallAnr(6_000))
        assertFalse(SUT.blockingCallAnr(200))
    }

    @Test fun `С14 reentrantOnSameThread`() {
        assertTrue(SUT.reentrantOnSameThread())
    }

    @Test fun `С15 inheritedNiceAcrossBinder`() {
        assertEquals(-4, SUT.inheritedNiceAcrossBinder(callerNice = -4, calleeNice = 10))
        assertEquals(-8, SUT.inheritedNiceAcrossBinder(callerNice = 0, calleeNice = -8))
    }

    // ── Сложные ──

    @Test fun `СЛ16 poolExhaustionHang`() {
        assertTrue(SUT.poolExhaustionHang(poolSize = 16, blockedThreads = 16))
        assertFalse(SUT.poolExhaustionHang(poolSize = 16, blockedThreads = 8))
    }

    @Test fun `СЛ17 reentrancyDeadlock`() {
        assertTrue(SUT.reentrancyDeadlock(holdsLock = true, reentrantLock = false))
        assertFalse(SUT.reentrancyDeadlock(holdsLock = true, reentrantLock = true))
        assertFalse(SUT.reentrancyDeadlock(holdsLock = false, reentrantLock = false))
    }

    @Test fun `СЛ18 onewayBufferOverflow`() {
        assertTrue(SUT.onewayBufferOverflow(listOf(300 * 1024, 300 * 1024)))
        assertFalse(SUT.onewayBufferOverflow(listOf(100 * 1024, 100 * 1024)))
    }

    @Test fun `СЛ19 needsCallbackSync`() {
        assertTrue(SUT.needsCallbackSync(inbound = 5, poolSize = 16))
        assertFalse(SUT.needsCallbackSync(inbound = 1, poolSize = 16))
    }

    @Test fun `СЛ20 detectCallCycle`() {
        assertTrue(SUT.detectCallCycle(listOf("A" to "B", "B" to "C", "C" to "A")))
        assertFalse(SUT.detectCallCycle(listOf("A" to "B", "B" to "C")))
    }
}
