package handbook.android.pools.solutions

import handbook.android.pools.Gate
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Эталонные решения темы 3. Подсмотри, если застрял с [handbook.android.pools.PoolsTasks]. */
object PoolsSolutions {

    private fun ExecutorService.finish() {
        shutdown()
        awaitTermination(5, TimeUnit.SECONDS)
    }

    // ── Лёгкие ──

    fun cpuBoundPoolSize(cores: Int): Int = cores + 1

    fun ioBoundPoolSize(cores: Int, waitMs: Long, computeMs: Long): Int =
        Math.round(cores * (1.0 + waitMs.toDouble() / computeMs)).toInt()

    fun threadNames(prefix: String, count: Int): List<String> {
        val n = AtomicInteger(0)
        val factory = ThreadFactory { r -> Thread(r, "$prefix-${n.incrementAndGet()}") }
        return (1..count).map { factory.newThread { }.name }
    }

    fun factoryPriority(priority: Int): Int {
        val factory = ThreadFactory { r -> Thread(r).apply { this.priority = priority } }
        return factory.newThread { }.priority
    }

    fun daemonFlag(daemon: Boolean): Boolean {
        val factory = ThreadFactory { r -> Thread(r).apply { isDaemon = daemon } }
        return factory.newThread { }.isDaemon
    }

    fun fixedPoolRunsAll(taskCount: Int): Int {
        val pool = Executors.newFixedThreadPool(4)
        val counter = AtomicInteger(0)
        repeat(taskCount) { pool.execute { counter.incrementAndGet() } }
        pool.finish()
        return counter.get()
    }

    fun futureValue(x: Int): Int {
        val pool = Executors.newSingleThreadExecutor()
        val future = pool.submit(Callable { x * 2 })
        val result = future.get()
        pool.finish()
        return result
    }

    fun invokeAllInOrder(inputs: List<Int>): List<Int> {
        val pool = Executors.newFixedThreadPool(4)
        val futures = pool.invokeAll(inputs.map { x -> Callable { x * x } })
        val result = futures.map { it.get() }
        pool.finish()
        return result
    }

    // ── Средние ──

    fun activeCoreThreads(): Int {
        val pool = ThreadPoolExecutor(3, 5, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        val gate = Gate(3)
        repeat(3) { pool.execute(gate.task()) }
        gate.awaitStarted()
        val size = pool.poolSize
        gate.release()
        pool.finish()
        return size
    }

    fun unboundedQueueCapsAtCore(): Int {
        // core=2, max=8, но очередь БЕЗГРАНИЧНА → до max дело не дойдёт никогда.
        val pool = ThreadPoolExecutor(2, 8, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        val gate = Gate(2)
        repeat(10) { pool.execute(gate.task()) }
        gate.awaitStarted()
        val size = pool.poolSize    // == 2
        gate.release()
        pool.finish()
        return size
    }

    fun boundedQueueReachesMax(): Int {
        // core=2, max=4, очередь на 2. 6 задач: 2 в core, 2 в очередь, 2 подняли пул до max=4.
        val pool = ThreadPoolExecutor(2, 4, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(2))
        val gate = Gate(4)
        repeat(6) { pool.execute(gate.task()) }
        gate.awaitStarted()
        val size = pool.poolSize    // == 4
        gate.release()
        pool.finish()
        return size
    }

    fun rejectsWhenSaturated(): Boolean {
        // core=2, max=4, очередь 2 → ёмкость 6. 7-я задача → RejectedExecutionException (AbortPolicy).
        val pool = ThreadPoolExecutor(2, 4, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(2))
        val gate = Gate(4)
        repeat(6) { pool.execute(gate.task()) }
        gate.awaitStarted()
        val rejected = try {
            pool.execute(gate.task())
            false
        } catch (e: RejectedExecutionException) {
            true
        }
        gate.release()
        pool.finish()
        return rejected
    }

    fun callerRunsOnCaller(): Boolean {
        val pool = ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(1),
            ThreadPoolExecutor.CallerRunsPolicy(),
        )
        val gate = Gate(1)
        pool.execute(gate.task())   // занял единственный поток
        gate.awaitStarted()
        pool.execute(gate.task())   // встал в очередь (ёмкость 1)
        val ranOn = arrayOfNulls<Thread>(1)
        pool.execute { ranOn[0] = Thread.currentThread() }  // пул насыщен → выполнится на ВЫЗЫВАЮЩЕМ
        val onCaller = ranOn[0] === Thread.currentThread()
        gate.release()
        pool.finish()
        return onCaller
    }

    fun discardDropsOverflow(): Int {
        val completed = AtomicInteger(0)
        val pool = ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(1),
            ThreadPoolExecutor.DiscardPolicy(),
        )
        val gate = Gate(1)
        pool.execute(gate.task { completed.incrementAndGet() }) // занял поток
        gate.awaitStarted()
        pool.execute(gate.task { completed.incrementAndGet() }) // в очередь
        pool.execute(gate.task { completed.incrementAndGet() }) // отброшена молча
        gate.release()
        pool.finish()
        return completed.get()      // == 2
    }

    fun shutdownNowReturnsPending(): Int {
        val pool = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        val gate = Gate(1)
        pool.execute(gate.task())   // занял поток и блокируется
        gate.awaitStarted()
        repeat(3) { pool.execute { } }   // 3 задачи ждут в очереди
        val pending = pool.shutdownNow() // прерывает текущую, возвращает НЕзапущенные
        pool.awaitTermination(5, TimeUnit.SECONDS)
        return pending.size         // == 3
    }

    // ── Сложные ──

    fun prestartCoreThreads(core: Int): Int {
        val pool = ThreadPoolExecutor(core, core * 2, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        pool.prestartAllCoreThreads()   // прогреть пул заранее, ещё до задач
        val size = pool.poolSize        // == core
        pool.finish()
        return size
    }

    fun singleThreadPreservesOrder(n: Int): List<Int> {
        val pool = Executors.newSingleThreadExecutor()
        val out: MutableList<Int> = Collections.synchronizedList(mutableListOf())
        repeat(n) { i -> pool.execute { out.add(i) } }
        pool.finish()
        return out.toList()
    }

    fun callerRunsNoLoss(n: Int): Int {
        val completed = AtomicInteger(0)
        val pool = ThreadPoolExecutor(
            2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(2),
            ThreadPoolExecutor.CallerRunsPolicy(),
        )
        repeat(n) { pool.execute { completed.incrementAndGet() } }
        pool.finish()
        return completed.get()      // == n, ни одна не потеряна (перелив исполняется на вызывающем)
    }

    fun poolIsolation(): Boolean {
        val io = Executors.newFixedThreadPool(2)
        val cpu = Executors.newFixedThreadPool(2)
        val gate = Gate(2)
        repeat(2) { io.execute(gate.task()) }   // полностью заняли io-пул
        gate.awaitStarted()
        val cpuResult = cpu.submit(Callable { 42 }).get()  // cpu-пул НЕ голодает
        gate.release()
        io.finish(); cpu.finish()
        return cpuResult == 42
    }

    fun observedParallelism(k: Int): Int {
        val pool = Executors.newFixedThreadPool(k)
        val barrier = CyclicBarrier(k)
        val active = AtomicInteger(0)
        val maxSeen = AtomicInteger(0)
        repeat(k) {
            pool.execute {
                val now = active.incrementAndGet()
                maxSeen.updateAndGet { m -> maxOf(m, now) }
                barrier.await()             // держим все k задачи одновременно
                active.decrementAndGet()
            }
        }
        pool.finish()
        return maxSeen.get()        // == k
    }
}
