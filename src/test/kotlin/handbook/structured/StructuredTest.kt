package handbook.structured

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * Тесты темы 2. Помимо результата проверяют ВИРТУАЛЬНОЕ ВРЕМЯ: раз тема про `async`-параллелизм,
 * общее время должно равняться МАКСИМУМУ задержек, а не их сумме. Так последовательная реализация
 * (await сразу после запуска, без второго async) не пройдёт по времени.
 */
class StructuredTest {

    // ── Лёгкие ──

    @Test
    fun `Л1 awaitBoth`() = runTest {
        val t0 = currentTime
        assertEquals("a" to 7, StructuredTasks.awaitBoth({ delay(100); "a" }, { delay(100); 7 }))
        assertEquals(100, currentTime - t0, "две delay(100) параллельно → 100, а не 200")
    }

    @Test
    fun `Л2 parallelSum параллельно`() = runTest {
        val t0 = currentTime
        val sum = StructuredTasks.parallelSum({ delay(1000); 2 }, { delay(1000); 3 })
        assertEquals(5, sum)
        assertEquals(1000, currentTime - t0, "параллельно → 1000, последовательно было бы 2000")
    }

    @Test
    fun `Л3 parallelProduct`() = runTest {
        assertEquals(12, StructuredTasks.parallelProduct({ 3 }, { 4 }))
    }

    @Test
    fun `Л4 combineAsync`() = runTest {
        assertEquals("a1", StructuredTasks.combineAsync({ "a" }, { 1 }) { x, y -> "$x$y" })
    }

    @Test
    fun `Л5 parallelMax`() = runTest {
        assertEquals(9, StructuredTasks.parallelMax({ 9 }, { 4 }))
    }

    @Test
    fun `Л6 runAllParallel`() = runTest {
        val counter = AtomicInteger(0)
        val t0 = currentTime
        StructuredTasks.runAllParallel(List(5) { { delay(10); counter.incrementAndGet(); Unit } })
        assertEquals(5, counter.get())
        assertEquals(10, currentTime - t0, "5 блоков по delay(10) параллельно → 10, а не 50")
    }

    @Test
    fun `Л7 asyncDouble`() = runTest {
        assertEquals(42, StructuredTasks.asyncDouble { 21 })
    }

    @Test
    fun `Л8 tripleParallel`() = runTest {
        assertEquals(Triple(1, "b", true), StructuredTasks.tripleParallel({ 1 }, { "b" }, { true }))
    }

    // ── Средние ──

    @Test
    fun `С9 loadAll сохраняет порядок`() = runTest {
        val t0 = currentTime
        val r = StructuredTasks.loadAll(listOf(1, 2, 3, 4)) { id -> delay((5 - id) * 10L); "item-$id" }
        assertEquals(listOf("item-1", "item-2", "item-3", "item-4"), r)
        assertEquals(40, currentTime - t0, "загрузки параллельны → max задержек (id=1 → 40)")
    }

    @Test
    fun `С10 parallelSumList`() = runTest {
        assertEquals(10, StructuredTasks.parallelSumList(listOf({ 1 }, { 2 }, { 3 }, { 4 })))
    }

    @Test
    fun `С11 parallelMap параллельно и с порядком`() = runTest {
        val t0 = currentTime
        // delay убывает с ростом i → элементы завершаются в ОБРАТНОМ порядке.
        // Решение, собирающее результаты по мере завершения, вернёт [9,4,1] и упадёт.
        val r = StructuredTasks.parallelMap(listOf(1, 2, 3)) { i -> delay((4 - i) * 10L); i * i }
        assertEquals(listOf(1, 4, 9), r, "порядок результатов = порядок items, а не завершения")
        assertEquals(30, currentTime - t0, "transform'ы параллельны → max delay (i=1 → 30), не сумма")
    }

    @Test
    fun `С12 sumHalvesInParallel`() = runTest {
        assertEquals(21, StructuredTasks.sumHalvesInParallel(listOf(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun `С13 parallelFilter параллельно и с порядком`() = runTest {
        val t0 = currentTime
        // delay убывает с ростом i → чётные завершаются в порядке 6,4,2.
        // Решение с launch + общий mutableList (добавление по мере завершения) вернёт [6,4,2] → упадёт.
        val r = StructuredTasks.parallelFilter((1..6).toList()) { i -> delay((7 - i) * 10L); i % 2 == 0 }
        assertEquals(listOf(2, 4, 6), r, "порядок = порядок входа, а не порядок завершения launch")
        assertEquals(60, currentTime - t0, "предикаты параллельны → max delay (i=1 → 60), не сумма 210")
    }

    @Test
    fun `С14 parallelCount параллельно`() = runTest {
        val t0 = currentTime
        val n = StructuredTasks.parallelCount((1..6).toList()) { delay(10); it % 2 == 0 }
        assertEquals(3, n)
        assertEquals(10, currentTime - t0, "предикаты параллельны → 10, последовательно было бы 60")
    }

    @Test
    fun `С15 awaitAllPreservesOrder`() = runTest {
        val t0 = currentTime
        assertEquals(listOf(0, 1, 2, 3), StructuredTasks.awaitAllPreservesOrder(listOf(40, 10, 30, 20)))
        assertEquals(40, currentTime - t0, "параллельно → max задержек = 40")
    }

    // ── Сложные ──

    @Test
    fun `СЛ16 mapConcurrent лимит`() = runTest {
        val inFlight = AtomicInteger(0)
        val maxSeen = AtomicInteger(0)
        val t0 = currentTime
        val r = StructuredTasks.mapConcurrent((1..10).toList(), concurrency = 3) { x ->
            val now = inFlight.incrementAndGet()
            maxSeen.updateAndGet { maxOf(it, now) }
            delay(10)
            inFlight.decrementAndGet()
            x * x
        }
        assertEquals((1..10).map { it * it }, r)
        assertTrue(maxSeen.get() <= 3, "одновременно ${maxSeen.get()} > 3")
        // 10 задач по 3 за раз → 4 волны × delay(10). Полный параллелизм дал бы 10, последовательный — 100.
        assertEquals(40, currentTime - t0, "лимит 3 → 4 волны по 10 мс = 40")
    }

    @Test
    fun `СЛ17 chunkedParallel внутри чанка параллельно, чанки последовательно`() = runTest {
        val inFlight = AtomicInteger(0)
        val maxSeen = AtomicInteger(0)
        val t0 = currentTime
        // delay убывает с ростом x → внутри чанка элементы ЗАВЕРШАЮТСЯ в обратном порядке.
        // Решение, пишущее в общий список по мере завершения async, выдаст [30,20,10,60,50,40].
        val r = StructuredTasks.chunkedParallel((1..6).toList(), chunkSize = 3) { x ->
            val now = inFlight.incrementAndGet()
            maxSeen.updateAndGet { maxOf(it, now) }
            delay((7 - x) * 10L)
            inFlight.decrementAndGet()
            x * 10
        }
        assertEquals(listOf(10, 20, 30, 40, 50, 60), r, "порядок = порядок входа, а не завершения")
        // Внутри чанка 3 элемента параллельно → одновременно 3. «Чанки параллельно, внутри
        // последовательно» дало бы maxSeen=2 (по одному на чанк) → тест отвергнет.
        assertEquals(3, maxSeen.get(), "внутри чанка (размер 3) все 3 transform идут одновременно")
        // Чанки последовательны: max(60,50,40)=60, затем max(30,20,10)=30 → 90.
        // Параллельные чанки дали бы max(60,30)=60 → тест отвергнет.
        assertEquals(90, currentTime - t0, "чанки последовательны → 60 + 30 = 90")
    }

    @Test
    fun `СЛ18 nestedParallelSum`() = runTest {
        val matrix = listOf(listOf(1, 2, 3), listOf(4, 5), listOf(6))
        val t0 = currentTime
        assertEquals(21, StructuredTasks.nestedParallelSum(matrix))
        // Строки параллельны, внутри строки delay(1) на элемент → время = самая длинная строка (3).
        assertEquals(3, currentTime - t0, "строки параллельны → max длины строки = 3")
    }

    @Test
    fun `СЛ19 parallelMapCatching изолирует ошибки`() = runTest {
        val r = StructuredTasks.parallelMapCatching(listOf(1, 2, 3, 4)) { x ->
            if (x % 2 == 0) error("even-$x") else x * 10
        }
        assertEquals(10, r[0].getOrNull())
        assertTrue(r[1].isFailure)
        assertEquals("even-2", r[1].exceptionOrNull()?.message)
        assertEquals(30, r[2].getOrNull())
        assertTrue(r[3].isFailure)
    }

    @Test
    fun `СЛ20 windowedSums`() = runTest {
        assertEquals(listOf(6, 9, 12), StructuredTasks.windowedSums(listOf(1, 2, 3, 4, 5), window = 3))
    }

    // ── Дополнительные ──

    @Test
    fun `Д21 runLazySequentially строго по очереди`() = runTest {
        val inFlight = AtomicInteger(0)
        val maxSeen = AtomicInteger(0)
        val t0 = currentTime
        val blocks = (1..4).map { i ->
            suspend {
                val now = inFlight.incrementAndGet()
                maxSeen.updateAndGet { maxOf(it, now) }
                delay(10)
                inFlight.decrementAndGet()
                i
            }
        }
        val r = StructuredTasks.runLazySequentially(blocks)
        assertEquals(listOf(1, 2, 3, 4), r)
        // DEFAULT-async запустил бы все разом → maxSeen=4, время=10. LAZY + await по очереди → 1 и 40.
        assertEquals(1, maxSeen.get(), "LAZY + последовательный await → одновременно максимум 1 блок")
        assertEquals(40, currentTime - t0, "4 блока строго по очереди → 4×10 = 40, а не 10")
    }

    @Test
    fun `Д22 bothOn последовательно на контексте`() = runTest {
        val t0 = currentTime
        val r = StructuredTasks.bothOn(EmptyCoroutineContext, { delay(100); "a" }, { delay(100); 2 })
        assertEquals("a" to 2, r)
        // withContext гоняет блоки по очереди → 100+100. Параллельное async-решение дало бы 100 → упадёт.
        assertEquals(200, currentTime - t0, "withContext последовательно → 200, не 100")
    }

    @Test
    fun `Д23 runStructured дожидается всех детей`() = runTest {
        val counter = AtomicInteger(0)
        val t0 = currentTime
        StructuredTasks.runStructured(List(5) { { delay(10); counter.incrementAndGet(); Unit } })
        // Отвязанные дети (launch(Job())/GlobalScope) → функция вернётся раньше, counter ещё 0 → упадёт.
        assertEquals(5, counter.get(), "функция обязана дождаться всех детей перед возвратом")
        assertEquals(10, currentTime - t0, "дети параллельны → 10, не 50")
    }

    @Test
    fun test() = runTest {
        var n = 0
        launch { delay(100); n = 1 }

        advanceTimeBy(100)
        println("a: t=$currentTime n=$n")
        runCurrent()
        println("b: t=$currentTime n=$n")
    }

}


fun main() = runBlocking {
    var counter = 0
    val jobs = List(1000) {
        launch(Dispatchers.Default) {
            repeat(3) {
                counter++
                yield()
            }
        }
    }
    jobs.forEach { it.join() }
    print(counter)
}

// 1 4 2 5 3
