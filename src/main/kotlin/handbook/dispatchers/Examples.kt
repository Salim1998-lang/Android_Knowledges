package handbook.dispatchers

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  ТЕМА 5. ДИСПЕТЧЕРЫ И CoroutineContext — запускаемые примеры из разбора.
 *  Запуск: main() внизу (зелёная стрелка в IDE). Рядом с каждым — вывод и ПОЧЕМУ.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * Ожидается: name = CoroutineName(B) / disp = Dispatchers.Default / count = 2 / after minusKey = null
 * Почему: CoroutineContext — набор элементов по Key. Правый операнд `+` перекрывает по ключу,
 * поэтому "A" и "B" — ОДИН элемент (последний выигрывает). fold считает элементы: Dispatcher + Name = 2.
 * minusKey(CoroutineName) убирает единственный Name → [CoroutineName] == null.
 */
fun d1_contextAnatomy() {
    val ctx: CoroutineContext = Dispatchers.Default + CoroutineName("A") + CoroutineName("B")
    println("name = ${ctx[CoroutineName]}")
    println("disp = ${ctx[CoroutineDispatcher]}")
    println("count = ${ctx.fold(0) { acc, _ -> acc + 1 }}")
    println("after minusKey = ${ctx.minusKey(CoroutineName)[CoroutineName]}")
}

/**
 * Ожидается: disp = Dispatchers.Default / name = child
 * Почему: контекст ребёнка = контекст родителя + аргументы билдера + новый Job.
 * Диспетчер НЕ переопределяли → протёк Default. CoroutineName переопределили в launch → "child".
 */
fun d2_inheritanceAndOverride() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default + CoroutineName("parent"))
    scope.launch(CoroutineName("child")) {
        println("disp = ${coroutineContext[CoroutineDispatcher]}")
        println("name = ${coroutineContext[CoroutineName]?.name}")
    }.join()
    scope.cancel()
}

/**
 * Ожидается: orphan survived / end   ("normal child" НЕ печатается)
 * Почему: launch(Job()) заменяет Job на новый корень → orphan больше НЕ потомок parent.
 * parent.cancel() отменяет обычного ребёнка ("normal child" гибнет), но не orphan.
 * orphan приходится join'ить вручную — structured concurrency порвана.
 */
fun d3_brokenHierarchy() = runBlocking {
    lateinit var orphan: Job
    val parent = launch {
        orphan = launch(Job()) { delay(100); println("orphan survived") }
        launch { delay(100); println("normal child") }
    }
    delay(20)
    parent.cancel()
    orphan.join()
    println("end")
}

/**
 * Ожидается: A <main> / C <main> / B <DefaultExecutor>
 * Почему: Unconfined стартует СИНХРОННО в текущем потоке до первой приостановки → "A" на main
 * печатается раньше "C". После delay корутину будит DefaultExecutor → "B" уже на другом потоке.
 * Поток "плавает" между точками приостановки.
 */
fun d4_unconfinedFloatingThread() = runBlocking {
    launch(Dispatchers.Unconfined) {
        println("A ${Thread.currentThread().name}")
        delay(50)
        println("B ${Thread.currentThread().name}")
    }
    println("C ${Thread.currentThread().name}")
}

/**
 * Ожидается: counter = 1000   (детерминированно, без потерянных обновлений)
 * Почему: limitedParallelism(1) = single-thread confinement — в любой момент выполняется максимум
 * одна корутина, поэтому НЕПРИОСТАНАВЛИВАЮЩАЯСЯ критическая секция (counter++) сериализована и гонок нет.
 * Это замена Mutex без блокировок — но ТОЛЬКО для секций без suspend (см. d5b).
 */
fun d5_limitedParallelismSerializes() = runBlocking {
    val d = Dispatchers.Default.limitedParallelism(1)
    var counter = 0 // намеренно НЕ atomic
    coroutineScope {
        repeat(1000) {
            launch(d) { counter++ } // критическая секция без приостановки
        }
    }
    println("counter = $counter")
}

/**
 * Ожидается: start 0 / start 1 / start 2 / end 0 / end 1 / end 2
 * Почему: ВАЖНАЯ тонкость. limitedParallelism(1) сериализует ВЫПОЛНЕНИЕ, но delay — точка приостановки,
 * которая ОСВОБОЖДАЕТ единственный слот. Поэтому каждая корутина доходит до delay, отпускает слот,
 * и стартует следующая → все "start" раньше всех "end". Confinement НЕ даёт взаимного исключения
 * через suspend — для этого всё равно нужен Mutex.
 */
fun d5b_confinementDoesNotSpanSuspension() = runBlocking {
    val d = Dispatchers.Default.limitedParallelism(1)
    coroutineScope {
        repeat(3) { i ->
            launch(d) { println("start $i"); delay(30); println("end $i") }
        }
    }
}

/**
 * Ожидается: CEH: boom
 * Почему: async здесь — РЕБЁНОК внутри launch, а не корневой. Его падение сразу летит вверх,
 * отменяет родительскую launch (корневую для скоупа), и срабатывает CEH из контекста скоупа.
 * Правило «async прячет исключение до await()» верно ТОЛЬКО для корневого async.
 */
fun d6_childAsyncPropagatesToCeh() = runBlocking {
    val handler = CoroutineExceptionHandler { _, e -> println("CEH: ${e.message}") }
    val scope = CoroutineScope(Job() + handler)
    scope.launch {
        async<Int> { throw RuntimeException("boom") }
    }.join()
}

/**
 * Ожидается: CEH: child1 / child2 alive
 * Почему: supervisorScope делает детей КОРНЕВЫМИ. Падение child1 не пробрасывается к брату/родителю —
 * оно необработанное на корневом уровне, поэтому ищется CEH в контексте ребёнка (унаследован от скоупа)
 * и срабатывает. child2 при этом выживает.
 */
fun d7_supervisorChildrenAreRoots() = runBlocking {
    val handler = CoroutineExceptionHandler { _, e -> println("CEH: ${e.message}") }
    val scope = CoroutineScope(Job() + handler)
    scope.launch {
        supervisorScope {
            launch { throw RuntimeException("child1") }
            launch { delay(50); println("child2 alive") }
        }
    }.join()
}

/**
 * Ожидается: withContext sequential ~200 ms / async parallel ~100 ms
 * Почему: withContext последователен по определению (100 + 100). Параллельность даёт не async сам по
 * себе, а ОТЛОЖЕННЫЙ await: сначала стартуем обе корутины, потом собираем → max(100, 100).
 */
fun d8_parallelVsSequential() = runBlocking {
    val seq = measureTimeMillis {
        val a = withContext(Dispatchers.IO) { delay(100); 1 }
        val b = withContext(Dispatchers.IO) { delay(100); 2 }
        a + b
    }
    println("withContext sequential ~$seq ms")

    val par = measureTimeMillis {
        coroutineScope {
            val a = async(Dispatchers.IO) { delay(100); 1 }
            val b = async(Dispatchers.IO) { delay(100); 2 }
            a.await() + b.await()
        }
    }
    println("async parallel ~$par ms")
}

private fun section(title: String) = println("\n──────── $title ────────")

fun main() {
    section("d1 contextAnatomy");              d1_contextAnatomy()
    section("d2 inheritanceAndOverride");      d2_inheritanceAndOverride()
    section("d3 brokenHierarchy");             d3_brokenHierarchy()
    section("d4 unconfinedFloatingThread");    d4_unconfinedFloatingThread()
    section("d5 limitedParallelismSerializes"); d5_limitedParallelismSerializes()
    section("d5b confinementDoesNotSpanSuspension"); d5b_confinementDoesNotSpanSuspension()
    section("d6 childAsyncPropagatesToCeh");   d6_childAsyncPropagatesToCeh()
    section("d7 supervisorChildrenAreRoots");  d7_supervisorChildrenAreRoots()
    section("d8 parallelVsSequential");        d8_parallelVsSequential()
    println("\n✔ Тема 5 — все примеры отработали.")
}
