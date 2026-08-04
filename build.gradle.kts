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

    testImplementation(libs.kotlinx.coroutines.test)
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
