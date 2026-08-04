package handbook.structured

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.system.measureTimeMillis

/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  ТЕМА 2. СТРУКТУРНАЯ КОНКУРЕНТНОСТЬ — запускаемые примеры из разбора.
 *  Запуск: main() внизу (зелёная стрелка в IDE). Рядом с каждым — вывод и ПОЧЕМУ.
 * ═══════════════════════════════════════════════════════════════════════════
 */

// Вспомогательные "загрузки" для примеров про параллелизм.
private suspend fun loadA(): Int { delay(100); return 1 }
private suspend fun loadB(): Int { delay(100); return 2 }

/**
 * Ожидается: start / scope body end / child B done / child A done / after scope
 * Почему: launch НЕ блокирует — тело scope доходит до конца ("scope body end") до детей.
 * B (delay 50) раньше A (delay 100). "after scope" — только после барьера coroutineScope.
 */
fun s1_scopeWaitsChildren() = runBlocking {
    println("start")
    coroutineScope {
        launch { delay(100); println("child A done") }
        launch { delay(50); println("child B done") }
        println("scope body end")
    }
    println("after scope")
}

/**
 * Ожидается: 1 / 3 / 2 / 4 / 5: 10 / 6
 * Почему: вложенный coroutineScope приостанавливает тело родителя до своего ребёнка (delay 70).
 * Пока тело висит — параллельно тикают async(50→"2") и launch(20→"3"). Порядок по времени:
 * 3(20), 2(50), 4(70) → затем тело оживает, d.await() уже готов = 10 → "5: 10", после барьера "6".
 */
fun s2_nestedScopeOrder() = runBlocking {
    println("1")
    coroutineScope {
        val d = async { delay(50); println("2"); 10 }
        launch { delay(20); println("3") }
        coroutineScope { launch { delay(70); println("4") } }
        println("5: ${d.await()}")
    }
    println("6")
}

/**
 * Ожидается: done 100 / done 200 / done 300 / result = [300, 100, 200]
 * Почему: все async стартуют сразу → печатают по возрастанию delay (100,200,300).
 * Но awaitAll СОХРАНЯЕТ порядок исходного списка [300,100,200], а не порядок завершения.
 */
fun s3_awaitAllPreservesOrder() = runBlocking {
    val deferreds = listOf(300, 100, 200).map { ms ->
        async { delay(ms.toLong()); println("done $ms"); ms }
    }
    println("result = ${deferreds.awaitAll()}")
}

/**
 * Ожидается: started / caught: B failed / end   (программа завершается нормально)
 * Почему: в coroutineScope падение async b (на 30 мс) СРАЗУ отменяет сиблинга a (тот не доходит
 * до "A ok"). coroutineScope пробрасывает исключение → ловит try/catch. "1" (сумма) нигде не печатается.
 */
fun s4_asyncFailFastCaught() = runBlocking {
    try {
        coroutineScope {
            val a = async { delay(100); println("A ok"); 1 }
            val b = async<Int> { delay(30); throw RuntimeException("B failed") }
            println("started")
            a.await() + b.await()
        }
    } catch (e: Exception) {
        println("caught: ${e.message}")
    }
    println("end")
}

/**
 * Ожидается: sequential ~200 ms, parallel ~100 ms
 * Почему: await() сразу после каждого async → вторая задача стартует после первой (100+100).
 * Все async заранее, потом await → задачи идут вместе → max(100,100).
 */
fun s5_sequentialVsParallel() = runBlocking {
    val t1 = measureTimeMillis {
        val a = async { loadA() }.await()
        val b = async { loadB() }.await()
        a + b
    }
    println("sequential ~$t1 ms")

    val t2 = measureTimeMillis {
        val a = async { loadA() }
        val b = async { loadB() }
        a.await() + b.await()
    }
    println("parallel ~$t2 ms")
}

/**
 * Ожидается: x / b / a / body: 3 / t=~150
 * Почему: три ребёнка стартуют разом; печати по возрастанию delay: x(50), b(100), a(150).
 * a.await() держит тело до 150; a печатает "a" ДО возврата значения. Общее время = max = 150.
 */
fun s6_sideEffectsAndTiming() = runBlocking {
    val t = measureTimeMillis {
        coroutineScope {
            val a = async { delay(150); println("a"); 1 }
            launch { delay(50); println("x") }
            val b = async { delay(100); println("b"); 2 }
            println("body: ${a.await() + b.await()}")
        }
    }
    println("t=~$t")
}

/**
 * Ожидается: caught a: a died / b ok / ra=-1, rb=2 / end
 * Почему: в supervisorScope падение async a НЕ отменяет b. Ошибку a ловим у a.await() → ra=-1.
 * b доживает до конца → "b ok", rb=2.
 */
fun s7_supervisorIsolatesFailure() = runBlocking {
    supervisorScope {
        val a = async<Int> { delay(30); throw RuntimeException("a died") }
        val b = async { delay(60); println("b ok"); 2 }
        val ra = try { a.await() } catch (e: Exception) { println("caught a: ${e.message}"); -1 }
        val rb = b.await()
        println("ra=$ra, rb=$rb")
    }
    println("end")
}

/**
 * Ожидается: task 1 done / task 2 done / task 3 done / task 4 done / ~200 ms
 * Почему: Semaphore(2) пускает по 2 одновременно. 4 задачи по 100 мс → 2 волны × 100 = 200 мс.
 */
fun s8_semaphoreLimit() = runBlocking {
    val gate = Semaphore(2)
    val t = measureTimeMillis {
        (1..4).map { i ->
            async { gate.withPermit { delay(100); println("task $i done") } }
        }.awaitAll()
    }
    println("~$t ms")
}

private fun section(title: String) = println("\n──────── $title ────────")

fun main() {
    section("s1 scopeWaitsChildren");        s1_scopeWaitsChildren()
    section("s2 nestedScopeOrder");          s2_nestedScopeOrder()
    section("s3 awaitAllPreservesOrder");    s3_awaitAllPreservesOrder()
    section("s4 asyncFailFastCaught");       s4_asyncFailFastCaught()
    section("s5 sequentialVsParallel");      s5_sequentialVsParallel()
    section("s6 sideEffectsAndTiming");      s6_sideEffectsAndTiming()
    section("s7 supervisorIsolatesFailure"); s7_supervisorIsolatesFailure()
    section("s8 semaphoreLimit");            s8_semaphoreLimit()
    println("\n✔ Тема 2 — все примеры отработали.")
}
