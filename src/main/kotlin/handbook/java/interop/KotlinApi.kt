@file:JvmName("KotlinApi")

package handbook.java.interop

import java.io.IOException

/**
 * Kotlin-API для темы 10 (дан целиком). Java-задачи в `Tasks.java` вызывают эти объявления и
 * показывают, КАК Kotlin выглядит из Java. Top-level функции/константы собраны в класс `KotlinApi`
 * (из-за `@file:JvmName`), поэтому из Java это `KotlinApi.xxx(...)`.
 */

/** const → в Java статическая финальная константа: `KotlinApi.VERSION`. */
const val VERSION: String = "1.0"

/** Top-level функция → статический метод `KotlinApi.topLevelGreet(...)`. */
fun topLevelGreet(name: String): String = "Hi, $name"

/** Extension-функция → статический метод с получателем первым аргументом: `KotlinApi.exclaim(s)`. */
fun String.exclaim(): String = this + "!"

/** Значение по умолчанию + @JvmOverloads → в Java доступны обе перегрузки: greet(name) и greet(name, greeting). */
@JvmOverloads
fun greet(name: String, greeting: String = "Hello"): String = "$greeting, $name"

/** Non-null параметр → Java при передаче null получит NullPointerException на границе. */
fun requireLength(s: String): Int = s.length

/** Nullable параметр → Java может передать null безопасно. */
fun nullableLength(s: String?): Int = s?.length ?: -1

/** Nullable-возврат → в Java аннотирован @Nullable, требует проверки. */
fun findOrNull(key: String): String? = if (key == "known") "found" else null

/** @JvmName меняет имя, видимое из Java. */
@JvmName("renamedForJava")
fun originalKotlinName(): String = "renamed"

/** @Throws делает checked-исключение видимым для Java (иначе Java не смогла бы его поймать). */
@Throws(IOException::class)
fun risky(fail: Boolean) {
    if (fail) throw IOException("boom")
}

/** Kotlin function type (Int)->Int → в Java это kotlin.jvm.functions.Function1. */
fun mapWith(x: Int, f: (Int) -> Int): Int = f(x)

/** () -> Unit → в Java это Function0<Unit>, invoke должен вернуть Unit.INSTANCE. */
fun runCallback(cb: () -> Unit) {
    cb()
}

/** Свойство → в Java пара getName()/setName(). */
class Config {
    var name: String = "default"
}

/** @JvmField → прямое поле; обычное свойство → геттер. */
class Holder {
    @JvmField val value: Int = 42
    val prop: Int = 7
}

/** object → синглтон, в Java доступен через INSTANCE. */
object Registry {
    fun ping(): String = "pong"
}

/** companion: @JvmStatic → Factory.create(); обычный метод companion → Factory.Companion.createDefault(). */
class Factory {
    companion object {
        @JvmStatic fun create(): String = "static"
        fun createDefault(): String = "companion"
    }
}

/** data class → сгенерированы getX()/getY(), equals()/hashCode(), toString(). */
data class Point(val x: Int, val y: Int)

/** fun interface (SAM) → из Java можно передать лямбду. */
fun interface IntTransformer {
    fun apply(x: Int): Int
}

fun applyTransform(x: Int, t: IntTransformer): Int = t.apply(x)
