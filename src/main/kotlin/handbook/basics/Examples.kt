package handbook.basics

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  ТЕМА 1. ОСНОВЫ КОРУТИН — запускаемые примеры из разбора (teacher-skill).
 *  Запуск: main() внизу (зелёная стрелка в IDE). Рядом с каждым — вывод и ПОЧЕМУ.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * Ожидается: 1 4 2 3 5
 * Почему: "1","4" синхронно (launch лишь ставит ребёнка в очередь). Первая suspend-точка родителя —
 * delay(20) — освобождает поток → ребёнок: "2", delay(10). На 10 мс "3", на 20 мс "5".
 */
fun b1_suspendPointOrder() = runBlocking {
    print("1")
    launch { print("2"); delay(10); print("3") }
    print("4")
    delay(20)
    print("5")
    println()
}

/**
 * Ожидается: 5 1 3 2 4
 * Почему: "5" синхронно; затем launch1: "1", yield уступает launch2: "3", yield возвращает
 * launch1: "2", потом launch2: "4". yield заставляет корутины чередоваться в точке уступки.
 */
fun b2_yieldInterleave() = runBlocking {
    launch { print("1"); yield(); print("2") }
    launch { print("3"); yield(); print("4") }
    print("5")
    println()
}

/**
 * Ожидается: 1 4 5 2 3   (сравни с b1: 1 4 2 3 5)
 * Почему: Thread.sleep(20) БЛОКИРУЕТ поток — это НЕ suspend-точка, ребёнок не может стартовать в паузе.
 * Поэтому "5" печатается до ребёнка. После тела runBlocking ждёт ребёнка → "2","3".
 */
fun b3_threadSleepBlocks() = runBlocking {
    print("1")
    launch { print("2"); delay(10); print("3") }
    print("4")
    @Suppress("BlockingMethodInNonBlockingContext")
    Thread.sleep(20)
    print("5")
    println()
}

/**
 * Ожидается: A B C D
 * Почему: launch(LAZY) не стартует, пока не вызовешь join()/start(). join() запускает и ждёт:
 * "B", delay, "C". Только потом "D".
 */
fun b4_lazyJoin() = runBlocking {
    val job = launch(start = CoroutineStart.LAZY) { print("B"); delay(10); print("C") }
    print("A")
    job.join()
    print("D")
    println()
}

/**
 * Ожидается: P Q X Y R
 * Почему: launch LAZY не стартует во время delay(50) (в отличие от обычного launch) → "P","Q".
 * job.join() запускает ленивую → "X",delay,"Y", затем "R".
 */
fun b5_lazyNotStartedDuringDelay() = runBlocking {
    val job = launch(start = CoroutineStart.LAZY) { print("X"); delay(20); print("Y") }
    print("P")
    delay(50)
    print("Q")
    job.join()
    print("R")
    println()
}

/**
 * Ожидается: act1:true / W / act2:false comp:true
 * Почему: до join корутина активна (isActive=true). join прокручивает ребёнка → "W" (на 30 мс).
 * После — завершена: isActive=false, isCompleted=true.
 */
fun b6_jobFlags() = runBlocking {
    val job = launch { delay(30); println("W") }
    println("act1:${job.isActive}")
    job.join()
    println("act2:${job.isActive} comp:${job.isCompleted}")
}

/**
 * Ожидается: 0 1 2 / ~100 ms
 * Почему: три delay(100) идут конкурентно (поток свободен на каждой паузе) → ~100, не 300.
 */
fun b7_delayConcurrent() = runBlocking {
    val time = measureTimeMillis {
        val jobs = List(3) { i -> launch { delay(100); print("$i ") } }
        jobs.forEach { it.join() }
    }
    println("| ~$time ms")
}

/**
 * Ожидается: ~200 ms
 * Почему: обе delay(100) перекрываются (100 мс), затем два Thread.sleep(50) сериализуют поток
 * (50+50). Итог 100 + 100 = 200.
 */
fun b8_sleepSerializes() = runBlocking {
    val time = measureTimeMillis {
        val a = launch { delay(100); @Suppress("BlockingMethodInNonBlockingContext") Thread.sleep(50) }
        val b = launch { delay(100); @Suppress("BlockingMethodInNonBlockingContext") Thread.sleep(50) }
        a.join(); b.join()
    }
    println("~$time ms")
}

/**
 * Ожидается: 1
 * Почему: read-modify-write разорван suspend-точкой. Все 1000 корутин читают counter(=0) ДО delay,
 * на 1 мс пишут tmp+1 = 1. Чередование через delay затирает результат.
 */
fun b9_raceViaSuspendPoint() = runBlocking {
    var counter = 0
    val jobs = List(1000) {
        launch { val tmp = counter; delay(1); counter = tmp + 1 }
    }
    jobs.forEach { it.join() }
    println(counter)
}

/**
 * Ожидается: 1000
 * Почему: counter++ БЕЗ suspend-точки внутри → на однопоточном диспетчере атомарен, чередования нет.
 */
fun b10_atomicNoSuspend() = runBlocking {
    var counter = 0
    val jobs = List(1000) { launch { counter++ } }
    jobs.forEach { it.join() }
    println(counter)
}

/**
 * Ожидается: 3000
 * Почему: yield() — suspend-точка ПОСЛЕ завершённого counter++. Инкремент атомарен, чередование
 * корутин его не рвёт → 1000 × 3.
 */
fun b11_yieldAfterIncrement() = runBlocking {
    var counter = 0
    val jobs = List(1000) {
        launch { repeat(3) { counter++; yield() } }
    }
    jobs.forEach { it.join() }
    println(counter)
}

/*
 * ── Подтема 5: runTest и виртуальное время ──
 * Эти примеры используют kotlinx-coroutines-test (доступен только в тестах), поэтому здесь
 * не запускаются. Рабочий аналог с currentTime/advanceTimeBy см. в
 * src/test/kotlin/handbook/basics/BasicsTest.kt. Ключевые факты, которые мы разбирали:
 *   advanceTimeBy(100) выполняет задачи, запланированные СТРОГО раньше 100 (задача ровно на 100
 *   ещё не сработает — её добьёт runCurrent()); advanceUntilIdle() домотает всё; currentTime —
 *   виртуальные миллисекунды.
 */

private fun section(title: String) = println("\n──────── $title ────────")

fun main() {
    section("b1 suspendPointOrder");         b1_suspendPointOrder()
    section("b2 yieldInterleave");           b2_yieldInterleave()
    section("b3 threadSleepBlocks");         b3_threadSleepBlocks()
    section("b4 lazyJoin");                  b4_lazyJoin()
    section("b5 lazyNotStartedDuringDelay"); b5_lazyNotStartedDuringDelay()
    section("b6 jobFlags");                  b6_jobFlags()
    section("b7 delayConcurrent");           b7_delayConcurrent()
    section("b8 sleepSerializes");           b8_sleepSerializes()
    section("b9 raceViaSuspendPoint");       b9_raceViaSuspendPoint()
    section("b10 atomicNoSuspend");          b10_atomicNoSuspend()
    section("b11 yieldAfterIncrement");      b11_yieldAfterIncrement()
    println("\n✔ Тема 1 — все примеры отработали.")
}
