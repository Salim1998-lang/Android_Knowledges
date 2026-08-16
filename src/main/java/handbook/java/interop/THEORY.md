# Тема 10. Interop Java ↔ Kotlin

> Читаешь теорию → решаешь 20 задач в `Tasks.java` → гоняешь тесты:
> `./gradlew test --tests "handbook.java.interop.*"`. Застрял — сверься с
> [`solutions/Solutions.java`](solutions/Solutions.java). Kotlin-API дан в
> [`KotlinApi.kt`](../../../kotlin/handbook/java/interop/KotlinApi.kt).

Финальная тема: как Java и Kotlin видят друг друга. Для android-разработчика это ежедневность —
кодовые базы смешанные, а Java-код (легаси, SDK, аннотации) должен корректно взаимодействовать с
Kotlin. Разбираем: свойства и `@JvmField`, `object`/`companion` и `@JvmStatic`, top-level и `@JvmName`,
значения по умолчанию и `@JvmOverloads`, `data class`, SAM/`fun interface`, nullability, `@Throws`,
function-типы.

---

## 1. Свойства → геттеры/сеттеры

Kotlin-свойство компилируется в методы, которые и видит Java:

```kotlin
class Config { var name: String = "default" }   // Kotlin
```
```java
Config c = new Config();
c.setName("x");            // var → get/set
String s = c.getName();    // val → только геттер
```

**`@JvmField`** убирает геттер и открывает поле напрямую (когда нужен именно field-доступ, напр. для
Java-библиотек, или ради производительности):

```kotlin
class Holder { @JvmField val value = 42; val prop = 7 }
```
```java
holder.value;        // прямое поле (@JvmField)
holder.getProp();    // обычное свойство — через геттер
```

## 2. `object` и `companion object`

- **`object Registry`** (синглтон) → в Java через статическое поле `INSTANCE`:

  ```java
  Registry.INSTANCE.ping();
  ```

- **`companion object`** → его члены по умолчанию видны через вложенный `Companion`:

  ```java
  Factory.Companion.createDefault();
  ```

  Пометь метод/свойство companion `@JvmStatic` — и он станет НАСТОЯЩИМ статическим членом класса:

  ```kotlin
  class Factory { companion object { @JvmStatic fun create() = "static" } }
  ```
  ```java
  Factory.create();     // как обычный static — без .Companion
  ```

## 3. Top-level функции/константы и `@JvmName`

Top-level объявления Kotlin складываются в синтетический класс `<ИмяФайла>Kt`. `@file:JvmName("X")`
переименовывает его:

```kotlin
@file:JvmName("KotlinApi")
fun topLevelGreet(name: String) = "Hi, $name"
const val VERSION = "1.0"
```
```java
KotlinApi.topLevelGreet("Bob");   // вместо KotlinApiKt
KotlinApi.VERSION;                 // const → public static final
```

**Extension-функция** становится статическим методом, где получатель — первый параметр:

```kotlin
fun String.exclaim() = this + "!"
```
```java
KotlinApi.exclaim("wow");   // "wow!"
```

`@JvmName` на отдельной функции меняет её имя для Java (напр., чтобы обойти mangling или дать удобное
имя):

```kotlin
@JvmName("renamedForJava") fun originalKotlinName() = "renamed"
```

## 4. Значения по умолчанию и `@JvmOverloads`

У Java нет параметров по умолчанию. Kotlin-функция с дефолтами видна из Java **только в полной форме** —
если не пометить `@JvmOverloads`, который генерирует перегрузки для каждого опущенного аргумента:

```kotlin
@JvmOverloads fun greet(name: String, greeting: String = "Hello") = "$greeting, $name"
```
```java
KotlinApi.greet("Kate");             // "Hello, Kate"  (перегрузка от @JvmOverloads)
KotlinApi.greet("Kate", "Hi");       // "Hi, Kate"
```

## 5. `data class`

Генерирует геттеры, `equals`/`hashCode`, `toString`, `componentN`, `copy`. Из Java удобно доступны
геттеры и `equals`; `copy(...)` с дефолтами — неудобен (нет именованных аргументов):

```java
Point p = new Point(3, 4);
p.getX(); p.getY();
new Point(1, 2).equals(new Point(1, 2));   // true
```

## 6. SAM и `fun interface`

Java **функциональный интерфейс** можно реализовать Kotlin-лямбдой (SAM-conversion), и наоборот —
Kotlin **`fun interface`** принимает Java-лямбду:

```kotlin
fun interface IntTransformer { fun apply(x: Int): Int }
fun applyTransform(x: Int, t: IntTransformer) = t.apply(x)
```
```java
KotlinApi.applyTransform(7, v -> v * 2);   // Java-лямбда → SAM
```

## 7. Nullability

Kotlin по nullability-аннотациям Java (`@Nullable`/`@NonNull`) выводит типы; в обратную сторону
Kotlin помечает свои типы для Java:

- **non-null параметр** Kotlin вставляет проверку на границе: Java, передав `null`, получит
  `NullPointerException` в точке вызова (fail-fast, а не глубже):

  ```java
  KotlinApi.requireLength(null);   // NullPointerException на границе
  ```

- **nullable параметр/возврат** (`String?`) Java видит как `@Nullable` — `null` допустим, а результат
  надо проверять:

  ```java
  KotlinApi.nullableLength(null);   // ок, вернёт -1
  String r = KotlinApi.findOrNull("x");   // может быть null
  ```

> **Platform types.** Когда Kotlin вызывает НЕаннотированный Java-код, тип приходит как «платформенный»
> (`String!`) — Kotlin не знает, nullable он или нет, и не форсирует проверку. Это опасно: можно
> получить NPE позже. Поэтому аннотируй Java-API (`@Nullable`/`@NonNull`, `androidx.annotation.*`) —
> тогда Kotlin проверит на этапе компиляции.

## 8. Checked-исключения и `@Throws`

В Kotlin **нет checked-исключений** — метод ничего не «объявляет». Поэтому Java не может поймать
checked-исключение из Kotlin (компилятор скажет «оно не бросается»). **`@Throws`** добавляет `throws` в
сигнатуру для Java:

```kotlin
@Throws(IOException::class) fun risky(fail: Boolean) { if (fail) throw IOException() }
```
```java
try { KotlinApi.risky(true); } catch (IOException e) { /* теперь ловится */ }
```

## 9. Function-типы

Kotlin function type `(A) -> R` в байткоде — это `kotlin.jvm.functions.FunctionN`. Из Java их видно как
`Function1<A, R>` и т.п., а `() -> Unit` — как `Function0<Unit>` (invoke обязан вернуть `Unit.INSTANCE`):

```java
KotlinApi.mapWith(5, new Function1<Integer, Integer>() {
    public Integer invoke(Integer v) { return v * 3; }
});
KotlinApi.runCallback(new Function0<Unit>() {
    public Unit invoke() { /* ... */ return Unit.INSTANCE; }
});
```

Это многословно — поэтому для API, вызываемых из Java, предпочитают `fun interface` (SAM) вместо голых
function-типов.

## 10. Чек-лист «Kotlin-API, дружелюбный к Java»

`@JvmStatic` (companion/object) · `@JvmField` (поля) · `@JvmOverloads` (дефолты) · `@JvmName` (имена) ·
`@Throws` (checked) · `fun interface` вместо `(T)->R` · nullability-аннотации на границах.

> Обратно: чтобы Kotlin комфортно вызывал твой Java-код — аннотируй его `@Nullable`/`@NonNull`, давай
> осмысленные имена геттерам (`getX`/`isX` → свойства в Kotlin), избегай platform-types-ловушек.

---

## Что дальше

Реши 20 задач в [`Tasks.java`](Tasks.java): Л1–Л8, С9–С15, СЛ16–СЛ20. Особое внимание — `@JvmStatic`/
`@JvmField` (Л2, Л4–Л5), nullability (С14, СЛ18), `@Throws` (СЛ16) и function-типам (СЛ19–СЛ20). Прогон:
`./gradlew test --tests "handbook.java.interop.*"`. Это финальная тема модуля — поздравляю! 🎉
