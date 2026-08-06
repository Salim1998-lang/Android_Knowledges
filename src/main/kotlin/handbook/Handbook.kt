package handbook

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Точка входа хэндбука: печатает оглавление и подсказку, как учиться.
 * Запуск: `./gradlew run`
 */
fun main() {
    val topics = listOf(
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

    println("=".repeat(60))
    println("  Kotlin Coroutines Handbook")
    println("=".repeat(60))
    println("Темы (изучай по порядку):\n")
    topics.forEach { println("  $it") }
    println()
    println("Для каждой темы: THEORY.md → реши задачи в Tasks.kt → прогони тесты.")
    println("Проверить тему:  ./gradlew test --tests \"handbook.basics.*\"")
    println("=".repeat(60))
}

fun numbers(): Flow<Int> = flow {
    println("старт")     // выполнится при КАЖДОМ collect
    for (i in 1..3) {
        delay(100)
        emit(i)          // излучаем значение
    }
}
