package handbook.testing.solutions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Эталонные решения темы 9. Подсмотри, если застрял с [handbook.testing.TestingTasks]. */
object TestingSolutions {

    // ── Лёгкие ──

    suspend fun TestScope.elapsed(block: suspend () -> Unit): Long {
        val t0 = currentTime
        block()
        return currentTime - t0
    }

    suspend fun TestScope.sequentialDelays(a: Long, b: Long): Long {
        delay(a)
        delay(b)
        return currentTime
    }

    suspend fun TestScope.concurrentDelays(a: Long, b: Long): Long {
        coroutineScope {
            launch { delay(a) }
            launch { delay(b) }
        }
        return currentTime
    }

    fun TestScope.advanceAndTime(ms: Long): Long {
        advanceTimeBy(ms)
        return currentTime
    }

    fun TestScope.pendingUntilIdle(): Int {
        var x = 0
        launch { delay(100); x = 42 }
        advanceUntilIdle()
        return x
    }

    fun TestScope.runCurrentImmediate(): Pair<Int, Int> {
        var a = 0
        var b = 0
        launch { a = 1 }
        launch { delay(100); b = 1 }
        runCurrent()
        return a to b
    }

    fun TestScope.advancePartial(): Pair<Int, Int> {
        var a = 0
        var b = 0
        launch { delay(100); a = 1 }
        launch { delay(300); b = 1 }
        advanceTimeBy(150)
        return a to b
    }

    suspend fun TestScope.delayedValue(ms: Long, value: Int): Int {
        delay(ms)
        return value
    }

    // ── Средние ──

    fun TestScope.standardIsLazy(): Pair<Boolean, Boolean> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var ran = false
        launch(dispatcher) { ran = true }
        val before = ran
        runCurrent()
        return before to ran
    }

    fun TestScope.unconfinedIsEager(): Boolean {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        var ran = false
        launch(dispatcher) { ran = true }
        return ran
    }

    fun TestScope.orderedByDelay(): List<Int> {
        val order = mutableListOf<Int>()
        launch { delay(300); order.add(3) }
        launch { delay(100); order.add(1) }
        launch { delay(200); order.add(2) }
        advanceUntilIdle()
        return order
    }

    fun TestScope.currentTimeAtEachStep(): List<Long> {
        val times = mutableListOf<Long>()
        launch { repeat(3) { delay(100); times.add(currentTime) } }
        advanceUntilIdle()
        return times
    }

    suspend fun TestScope.timeoutSucceeds(): Int = withTimeout(1000) {
        delay(500)
        7
    }

    suspend fun TestScope.timeoutFails(): Boolean =
        withTimeoutOrNull(1000) { delay(2000); 1 } == null

    suspend fun TestScope.collectWithVirtualTime(): Pair<List<Int>, Long> {
        val f = flow { for (i in 1..3) { delay(100); emit(i) } }
        val t0 = currentTime
        val list = f.toList()
        return list to (currentTime - t0)
    }

    // ── Сложные ──

    suspend fun runOnMain(): String = withContext(Dispatchers.Main) { "on-main" }

    suspend fun TestScope.retryWithBackoff(failTimes: Int): Pair<Int, Long> {
        val t0 = currentTime
        var attempt = 0
        while (true) {
            attempt++
            if (attempt > failTimes) break                 // успех
            delay(100L * (1L shl (attempt - 1)))           // 100, 200, 400, ...
        }
        return attempt to (currentTime - t0)
    }

    fun TestScope.backgroundTicker(period: Long, steps: Int): List<Int> {
        val ticks = mutableListOf<Int>()
        var i = 0
        backgroundScope.launch {
            while (true) {
                delay(period)
                ticks.add(++i)
            }
        }
        repeat(steps) {
            advanceTimeBy(period)
            runCurrent()                                   // «дожать» задачу на границе интервала
        }
        return ticks.toList()
    }

    suspend fun TestScope.parallelAwaitAll(a: Long, b: Long, c: Long): Pair<List<Long>, Long> {
        val t0 = currentTime
        val results = listOf(a, b, c).map { d -> async { delay(d); d } }.awaitAll()
        return results to (currentTime - t0)
    }

    suspend fun TestScope.sequentialVsParallel(): Pair<Long, Long> {
        val t0 = currentTime
        delay(100); delay(200); delay(300)
        val seq = currentTime - t0

        val t1 = currentTime
        coroutineScope {
            launch { delay(100) }
            launch { delay(200) }
            launch { delay(300) }
        }
        val par = currentTime - t1
        return seq to par
    }
}
