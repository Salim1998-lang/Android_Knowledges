package handbook.cancellation

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  ТЕМА 3. ОТМЕНА И ТАЙМАУТЫ — запускаемые примеры из разбора.
 *  Запуск: main() внизу (зелёная стрелка в IDE). Рядом с каждым — вывод и ПОЧЕМУ.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * Ожидается: tick 0 / tick 1 / tick 2 / cleanup / done
 * Почему: тики на 0/100/200 мс; на 250 мс отмена бьёт в delay → CancellationException → finally
 * ("cleanup"). cancelAndJoin дожидается ребёнка → "done".
 */
fun c1_cooperativeCleanup() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i -> println("tick $i"); delay(100) }
        } finally {
            println("cleanup")
        }
    }
    delay(250)
    job.cancelAndJoin()
    println("done")
}

/**
 * Ожидается: cancelled=true
 * Почему: ensureActive() каждую итерацию проверяет отмену; после cancel ближайший вызов бросает
 * CancellationException → корутина завершается как отменённая. (CPU-цикл БЕЗ проверок отменить нельзя.)
 */
fun c2_ensureActiveCancels() = runBlocking {
    val counter = AtomicInteger(0)
    val good = launch(Dispatchers.Default) {
        while (true) { ensureActive(); counter.incrementAndGet() }
    }
    delay(50)
    good.cancelAndJoin()
    println("cancelled=${good.isCancelled}")
}

/**
 * Ожидается: count=3   ("exited loop" НЕ печатается)
 * Почему: count растёт на 0/100/200; на 250 мс отмена бьёт в delay (suspend-точка) → цикл покидается
 * ЧЕРЕЗ исключение, а не через проверку while(isActive), поэтому "exited loop" недостижимо.
 */
fun c3_suspendPointCatchesFirst() = runBlocking {
    var count = 0
    val job = launch {
        while (isActive) { count++; delay(100) }
        println("exited loop")
    }
    delay(250)
    job.cancelAndJoin()
    println("count=$count")
}

/**
 * Ожидается: work 0 / work 1 / work 2 / caught: JobCancellationException / after try / isCancelled=true
 * Почему: КАПКАН. catch(Exception) ловит и ПРОГЛАТЫВАЕТ отмену → "after try" исполняется, хотя
 * корутина отменена. isCancelled=true (cancel был вызван), но контроль потерян.
 */
fun c4_swallowCancellation() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { println("work $it"); delay(100) }
        } catch (e: Exception) {
            println("caught: ${e::class.simpleName}")
        }
        println("after try")
    }
    delay(250)
    job.cancelAndJoin()
    println("isCancelled=${job.isCancelled}")
}

/**
 * Ожидается: step 0 / step 1 / cancel caught / isCancelled=true   ("after try" НЕ печатается)
 * Почему: канонический шаблон — catch(CancellationException){ throw e }. Проброс обрывает поток
 * управления, "after try" не выполняется. Сравни с c4, где проглотили.
 */
fun c5_rethrowPattern() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { println("step $it"); delay(100) }
        } catch (e: CancellationException) {
            println("cancel caught"); throw e
        } catch (e: Exception) {
            println("error caught")
        }
        println("after try")
    }
    delay(150)
    job.cancelAndJoin()
    println("isCancelled=${job.isCancelled}")
}

/**
 * Ожидается: iter 0 / iter 1 / iter 2 / success=false / after runCatching / done isCancelled=true
 * Почему: КАПКАН runCatching — он ловит ВСЁ, включая CancellationException → success=false, и тело
 * доезжает до конца, хотя корутина отменена. "Сырой" runCatching в корутинах опасен.
 */
fun c6_runCatchingTrap() = runBlocking {
    val job = launch {
        val result = runCatching {
            repeat(1000) { println("iter $it"); delay(100) }
        }
        println("success=${result.isSuccess}")
        println("after runCatching")
    }
    delay(250)
    job.cancelAndJoin()
    println("done isCancelled=${job.isCancelled}")
}

/**
 * Ожидается: a=ok / b=null / timed out
 * Почему: a успел (100<200). b не успел (300>150) → withTimeoutOrNull вернул null. withTimeout по
 * дедлайну бросает TimeoutCancellationException → ловим.
 */
fun c7_withTimeoutBasics() = runBlocking {
    val a = withTimeoutOrNull(200) { delay(100); "ok" }
    println("a=$a")
    val b = withTimeoutOrNull(150) { delay(300); "ok" }
    println("b=$b")
    try {
        withTimeout(150) { delay(300); "never" }
    } catch (e: TimeoutCancellationException) {
        println("timed out")
    }
}

/**
 * Ожидается: i=1 / i=2 / r=null
 * Почему: delay(100) даёт тики на 100/200; третий метит на 300, но дедлайн 250 бьёт раньше →
 * withTimeoutOrNull вернул null.
 */
fun c8_timeoutInterruptsLoop() = runBlocking {
    val r = withTimeoutOrNull(250) {
        var i = 0
        repeat(5) { delay(100); i++; println("i=$i") }
        i
    }
    println("r=$r")
}

/**
 * Ожидается: inner caught / inner continues / outer timeout
 * Почему: КАПКАН. Внутренний catch(Exception) глотает TimeoutCancellationException → "inner caught",
 * "inner continues". Но корутина блока уже отменена таймаутом: возврат значения не спасает —
 * withTimeout всё равно бросает наружу → "outer timeout" (а не "outer ok").
 */
fun c9_swallowTimeoutInside() = runBlocking {
    try {
        withTimeout(150) {
            try { delay(300) } catch (e: Exception) { println("inner caught") }
            println("inner continues")
            "done"
        }
        println("outer ok")
    } catch (e: TimeoutCancellationException) {
        println("outer timeout")
    }
}

/**
 * Ожидается: work 0 / work 1 / finally start / done   ("after delay (naive)" НЕ печатается)
 * Почему: после отмены новый suspend-вызов (delay) в finally мгновенно бросает CancellationException,
 * поэтому код после него не выполняется. Синхронный "finally start" — выполнился.
 */
fun c10_suspendInFinallySwallowed() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { println("work $it"); delay(100) }
        } finally {
            println("finally start")
            delay(50)
            println("after delay (naive)")
        }
    }
    delay(150)
    job.cancelAndJoin()
    println("done")
}

/**
 * Ожидается: tick 0 / tick 1 / tick 2 / cleanup start / cleanup done / end
 * Почему: withContext(NonCancellable) делает участок неотменяемым → delay(50) реально исполняется,
 * "cleanup done" печатается. cancelAndJoin ждёт весь finally → "end" строго последним.
 */
fun c11_nonCancellableCleanup() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { println("tick $it"); delay(100) }
        } finally {
            withContext(NonCancellable) {
                println("cleanup start"); delay(50); println("cleanup done")
            }
        }
    }
    delay(250)
    job.cancelAndJoin()
    println("end")
}

/**
 * Ожидается: run 0 / A / E   (B, C, D НЕ печатаются)
 * Почему: NonCancellable защищает только код ВНУТРИ себя — но до него надо дойти. Первый же голый
 * delay(30) в finally (вне NonCancellable) бросает CancellationException и обрывает остаток finally,
 * так что до блока с "C" управление не доходит.
 */
fun c12_partialNonCancellable() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { delay(100); println("run $it") }
        } finally {
            println("A")
            delay(30)
            println("B")
            withContext(NonCancellable) { delay(30); println("C") }
            println("D")
        }
    }
    delay(150)
    job.cancelAndJoin()
    println("E")
}

/**
 * Ожидается: child A cleanup / child B cleanup / parent cancelled
 * Почему: отмена родителя каскадит вниз — оба ребёнка отменяются, каждый доигрывает finally.
 * "parent body done" недостижимо. cancelAndJoin ждёт всё поддерево → "parent cancelled" последним.
 */
fun c13_cancelCascades() = runBlocking {
    val parent = launch {
        launch { try { repeat(1000) { delay(100) } } finally { println("child A cleanup") } }
        launch { try { repeat(1000) { delay(100) } } finally { println("child B cleanup") } }
        delay(1000)
        println("parent body done")
    }
    delay(250)
    parent.cancelAndJoin()
    println("parent cancelled")
}

/**
 * Ожидается: B tick 0 / A cleanup / A cancelled, B untouched / B tick 1 / B tick 2 / scope done
 * Почему: "B tick 0" на 100 мс — раньше отмены a (на 150 мс). Отмена ОДНОГО ребёнка (a) обычной
 * CancellationException не трогает сиблинга b — тот доигрывает тики. coroutineScope ждёт b → "scope done".
 */
fun c14_cancelOneChild() = runBlocking {
    coroutineScope {
        val a = launch {
            try { repeat(1000) { delay(100) } } finally { println("A cleanup") }
        }
        launch { repeat(3) { delay(100); println("B tick $it") } }
        delay(150)
        a.cancelAndJoin()
        println("A cancelled, B untouched")
    }
    println("scope done")
}

/**
 * Ожидается: both launched / sibling cleanup / caught: boom / end
 * Почему: "both launched" синхронно. На 150 мс ребёнок бросает RuntimeException (НЕ отмену) → fail-fast:
 * coroutineScope отменяет сиблинга (его finally → "sibling cleanup") и пробрасывает ошибку → catch.
 * Контраст с c14: там CancellationException одного ребёнка сиблинга не трогала.
 */
fun c15_siblingFailFast() = runBlocking {
    try {
        coroutineScope {
            launch {
                try { repeat(1000) { delay(100) } } finally { println("sibling cleanup") }
            }
            launch { delay(150); throw RuntimeException("boom") }
            println("both launched")
        }
    } catch (e: RuntimeException) {
        println("caught: ${e.message}")
    }
    println("end")
}

private fun section(title: String) = println("\n──────── $title ────────")

fun main() {
    section("c1 cooperativeCleanup");         c1_cooperativeCleanup()
    section("c2 ensureActiveCancels");        c2_ensureActiveCancels()
    section("c3 suspendPointCatchesFirst");   c3_suspendPointCatchesFirst()
    section("c4 swallowCancellation");        c4_swallowCancellation()
    section("c5 rethrowPattern");             c5_rethrowPattern()
    section("c6 runCatchingTrap");            c6_runCatchingTrap()
    section("c7 withTimeoutBasics");          c7_withTimeoutBasics()
    section("c8 timeoutInterruptsLoop");      c8_timeoutInterruptsLoop()
    section("c9 swallowTimeoutInside");       c9_swallowTimeoutInside()
    section("c10 suspendInFinallySwallowed"); c10_suspendInFinallySwallowed()
    section("c11 nonCancellableCleanup");     c11_nonCancellableCleanup()
    section("c12 partialNonCancellable");     c12_partialNonCancellable()
    section("c13 cancelCascades");            c13_cancelCascades()
    section("c14 cancelOneChild");            c14_cancelOneChild()
    section("c15 siblingFailFast");           c15_siblingFailFast()
    println("\n✔ Тема 3 — все примеры отработали.")
}
