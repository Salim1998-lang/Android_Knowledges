package handbook

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
        "5. flow          — холодные потоки, операторы, backpressure",
        "6. channels      — Channel, produce, pipelines, fan-in/fan-out",
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
