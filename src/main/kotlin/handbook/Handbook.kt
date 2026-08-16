package handbook

/**
 * Точка входа хэндбука: печатает оглавление и подсказку, как учиться.
 * Запуск: `./gradlew run`
 */
fun main() {
    val coroutines = listOf(
        "1. basics        — suspend, runBlocking, launch, delay, join, Job",
        "2. structured    — coroutineScope, async/await, параллельная декомпозиция",
        "3. cancellation  — кооперативная отмена, isActive, withTimeout, NonCancellable",
        "4. exceptions    — try/catch, SupervisorJob, CoroutineExceptionHandler",
        "5. dispatchers   — Dispatchers, CoroutineContext, withContext, limitedParallelism",
        "6. flow          — холодные потоки, операторы, backpressure",
        "7. hotflows      — StateFlow, SharedFlow, stateIn/shareIn, SharingStarted",
        "8. channels      — Channel, produce, pipelines, fan-in/fan-out",
        "9. concurrency   — разделяемое состояние, Mutex, атомики, замыкание, actor",
        "10. testing      — runTest, виртуальное время, TestDispatcher, setMain, backgroundScope",
    )

    // Модуль «Java для Android». ⬜ — тема ещё в работе (нет задач/теории).
    val java = listOf(
        "1. corelang      — примитивы/обёртки, кэш Integer, String pool, == vs equals",
        "2. oop           — интерфейсы/default, наследование vs композиция, enum, вложенные классы",
        "3. generics      — тип-параметры, границы, вариантность (PECS), стирание типов",
        "4. collections   — List/Set/Map, HashMap внутри, equals/hashCode, Comparator, LRU, fail-fast",
        "5. exceptions    — checked/unchecked, try-with-resources, suppressed, cause/chaining",
        "6. functional    — лямбды, method references, Stream API, коллекторы, Optional",
        "7. concurrency   — Thread, synchronized, volatile, JMM/happens-before, atomic/CAS, wait/notify",
        "8. concurrenthigh— ExecutorService, Future/CompletableFuture, ConcurrentHashMap, BlockingQueue, локи",
        "9. memory        — GC, достижимость, strong/soft/weak/phantom, утечки в Android + лечение",
        "10. interop      — свойства/@JvmStatic/@JvmField/@JvmOverloads/@JvmName, SAM, nullability, @Throws",
    )

    println("=".repeat(64))
    println("  Android Handbook")
    println("=".repeat(64))

    println("\n🧵 Kotlin Coroutines  (пакеты handbook.*)")
    coroutines.forEach { println("   $it") }

    println("\n☕ Java для Android   (пакеты handbook.java.*)")
    java.forEach { println("   $it") }

    println()
    println("Для каждой темы: THEORY.md → реши задачи в Tasks → прогони тесты.")
    println("Проверить тему:  ./gradlew test --tests \"handbook.java.corelang.*\"")
    println("=".repeat(64))
}
