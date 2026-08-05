plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "handbook"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    // Тема 9 «Тестирование корутин» использует TestScope/StandardTestDispatcher прямо в учебном
    // коде (Tasks.kt/Solutions.kt в src/main), поэтому тест-библиотека нужна на main-классе, а не
    // только в тестах. Для тестов она остаётся видимой (implementation попадает и в test-classpath).
    implementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Некоторые тест-утилиты (advanceTimeBy, currentTime) и API каналов помечены как экспериментальные.
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

application {
    // Точка входа: печатает оглавление хэндбука.
    mainClass.set("handbook.HandbookKt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
