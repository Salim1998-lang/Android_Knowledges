package handbook.android.pools

import handbook.android.pools.solutions.PoolsSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

/**
 * Тесты темы 3 «Пулы потоков». Настраивают НАСТОЯЩИЙ `ThreadPoolExecutor` и наблюдают его поведение
 * детерминированно (гейт [Gate] занимает ровно N потоков, `CyclicBarrier` держит K одновременно).
 * Проверяют sizing, `ThreadFactory`, связку core/max/очередь, rejection-политики и изоляцию пулов.
 *
 * Чтобы гонять против эталона — замени `SUT` на [PoolsSolutions].
 */
private typealias SUT = PoolsTasks

class PoolsTest {

    // ── Лёгкие ──

    @Test fun `Л1 cpuBoundPoolSize`() {
        assertEquals(9, SUT.cpuBoundPoolSize(8))
    }

    @Test fun `Л2 ioBoundPoolSize — формула Гётца`() {
        assertEquals(40, SUT.ioBoundPoolSize(cores = 4, waitMs = 90, computeMs = 10)) // 4*(1+9)
        assertEquals(8, SUT.ioBoundPoolSize(cores = 8, waitMs = 0, computeMs = 10))   // чистый CPU
    }

    @Test fun `Л3 threadNames`() {
        assertEquals(listOf("io-1", "io-2", "io-3"), SUT.threadNames("io", 3))
    }

    @Test fun `Л4 factoryPriority`() {
        assertEquals(Thread.MIN_PRIORITY, SUT.factoryPriority(Thread.MIN_PRIORITY))
    }

    @Test fun `Л5 daemonFlag`() {
        assertTrue(SUT.daemonFlag(true))
    }

    @Test fun `Л6 fixedPoolRunsAll`() {
        assertEquals(100, SUT.fixedPoolRunsAll(100))
    }

    @Test fun `Л7 futureValue`() {
        assertEquals(42, SUT.futureValue(21))
    }

    @Test fun `Л8 invokeAllInOrder`() {
        assertEquals(listOf(1, 4, 9, 16), SUT.invokeAllInOrder(listOf(1, 2, 3, 4)))
    }

    // ── Средние ──

    @RepeatedTest(15) fun `С9 activeCoreThreads`() {
        assertEquals(3, SUT.activeCoreThreads())
    }

    @RepeatedTest(15) fun `С10 unboundedQueueCapsAtCore — подвох`() {
        assertEquals(2, SUT.unboundedQueueCapsAtCore())
    }

    @RepeatedTest(15) fun `С11 boundedQueueReachesMax`() {
        assertEquals(4, SUT.boundedQueueReachesMax())
    }

    @RepeatedTest(15) fun `С12 rejectsWhenSaturated`() {
        assertTrue(SUT.rejectsWhenSaturated())
    }

    @RepeatedTest(15) fun `С13 callerRunsOnCaller`() {
        assertTrue(SUT.callerRunsOnCaller())
    }

    @RepeatedTest(15) fun `С14 discardDropsOverflow`() {
        assertEquals(2, SUT.discardDropsOverflow())
    }

    @RepeatedTest(15) fun `С15 shutdownNowReturnsPending`() {
        assertEquals(3, SUT.shutdownNowReturnsPending())
    }

    // ── Сложные ──

    @RepeatedTest(10) fun `СЛ16 prestartCoreThreads`() {
        assertEquals(4, SUT.prestartCoreThreads(4))
    }

    @Test fun `СЛ17 singleThreadPreservesOrder`() {
        assertEquals((0 until 50).toList(), SUT.singleThreadPreservesOrder(50))
    }

    @RepeatedTest(10) fun `СЛ18 callerRunsNoLoss`() {
        assertEquals(200, SUT.callerRunsNoLoss(200))
    }

    @RepeatedTest(15) fun `СЛ19 poolIsolation`() {
        assertTrue(SUT.poolIsolation())
    }

    @RepeatedTest(15) fun `СЛ20 observedParallelism`() {
        assertEquals(4, SUT.observedParallelism(4))
    }
}
