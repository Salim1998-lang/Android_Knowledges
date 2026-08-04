package handbook.basics.solutions

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/** Эталонные решения темы 1. Подсмотри, если застрял с [handbook.basics.BasicsTasks]. */
object BasicsSolutions {

    suspend fun delayedValue(value: Int): Int {
        delay(100)
        return value
    }

    suspend fun greet(name: String): String {
        delay(50)
        return "Привет, $name!"
    }

    suspend fun sumAfterDelay(a: Int, b: Int): Int {
        delay(10)
        return a + b
    }

    suspend fun sequentialSum(a: suspend () -> Int, b: suspend () -> Int): Int {
        val x = a()
        val y = b()
        return x + y
    }

    suspend fun doubled(x: Int): Int {
        delay(10)
        return 2 * x
    }

    suspend fun repeatValue(value: String, times: Int): List<String> {
        val result = mutableListOf<String>()
        repeat(times) {
            delay(5)
            result.add(value)
        }
        return result
    }

    suspend fun countdown(n: Int): List<Int> {
        val result = mutableListOf<Int>()
        for (i in n downTo 1) {
            delay(5)
            result.add(i)
        }
        return result
    }

    suspend fun runInChild(action: () -> Unit) = coroutineScope {
        val job = launch { action() }
        job.join()
    }

    suspend fun collectInLaunchOrder(n: Int): List<Int> = coroutineScope {
        val completed = mutableListOf<Int>()
        for (i in 0 until n) {
            launch {
                delay((n - i) * 10L)
                completed.add(i)
            }
        }
        completed
    }

    suspend fun launchN(n: Int, onEach: (Int) -> Unit): Int = coroutineScope {
        for (i in 0 until n) {
            launch { onEach(i) }
        }
        n
    }

    suspend fun sequentialChain(start: Int, ops: List<suspend (Int) -> Int>): Int {
        var acc = start
        for (op in ops) acc = op(acc)
        return acc
    }

    suspend fun delayedSum(values: List<Int>): Int {
        var sum = 0
        for (v in values) {
            delay(5)
            sum += v
        }
        return sum
    }

    suspend fun buildGreetings(names: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (name in names) result.add(greet(name))
        return result
    }

    suspend fun counterWithLaunches(n: Int): Int {
        var counter = 0
        coroutineScope {
            repeat(n) {
                launch {
                    delay(1)
                    counter++
                }
            }
        }
        return counter
    }

    suspend fun orderedByDelay(delays: List<Long>): List<Int> = coroutineScope {
        val order = mutableListOf<Int>()
        delays.forEachIndexed { index, d ->
            launch {
                delay(d)
                order.add(index)
            }
        }
        order
    }

    suspend fun <T> retry(times: Int, delayMs: Long = 20, block: suspend () -> T): T {
        require(times >= 1) { "times must be >= 1" }
        var last: Throwable? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                last = e
                if (attempt < times - 1) delay(delayMs)
            }
        }
        throw last!!
    }

    suspend fun pollUntil(target: Int, produce: suspend () -> Int): Int {
        var attempts = 0
        while (true) {
            attempts++
            if (produce() == target) return attempts
            delay(5)
        }
    }

    suspend fun factorial(n: Int): Long {
        require(n >= 0)
        if (n <= 1) return 1
        delay(1)
        return n * factorial(n - 1)
    }

    suspend fun lazyStart(trigger: Boolean, onStart: () -> Unit): Boolean = coroutineScope {
        val job = launch(start = CoroutineStart.LAZY) { onStart() }
        if (trigger) {
            job.join()
            true
        } else {
            job.cancel()
            false
        }
    }

    suspend fun accumulate(n: Int, step: Int): Int {
        var sum = 0
        repeat(n) {
            delay(1)
            sum += step
        }
        return sum
    }

    suspend fun roundRobin(n: Int): List<Int> = coroutineScope {
        val res = mutableListOf<Int>()
        repeat(n) { i ->
            launch {
                res.add(i)
                yield()
                res.add(i)
            }
        }
        res
    }

    suspend fun lazyJobFlags(): Pair<Triple<Boolean, Boolean, Boolean>, Triple<Boolean, Boolean, Boolean>> =
        coroutineScope {
            val job = launch(start = CoroutineStart.LAZY) { delay(10) }
            val new = Triple(job.isActive, job.isCompleted, job.isCancelled)
            job.cancelAndJoin()
            val cancelled = Triple(job.isActive, job.isCompleted, job.isCancelled)
            new to cancelled
        }

    suspend fun queueBeforeSuspend(): List<String> = coroutineScope {
        val log = mutableListOf<String>()
        launch { log.add("child") }
        log.add("parent-before")
        yield()
        log.add("parent-after")
        log
    }
}
