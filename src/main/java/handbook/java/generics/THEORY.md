# Тема 3. Дженерики

> Читаешь теорию → решаешь 20 задач в `Tasks.java` → гоняешь тесты:
> `./gradlew test --tests "handbook.java.generics.*"`. Застрял — сверься с
> [`solutions/Solutions.java`](solutions/Solutions.java).

Дженерики — про типобезопасность на этапе компиляции без дублирования кода. Разбираемся, зачем они,
что такое границы и wildcards, **что такое вариантность (ковариантность / контравариантность /
инвариантность)** и почему во время выполнения дженериков… нет (стирание типов).

---

## 1. Зачем дженерики

До дженериков коллекции хранили `Object` — и приходилось руками кастовать, ловя `ClassCastException`
в рантайме. Дженерики переносят проверку типов на **компиляцию**:

```java
List<String> names = new ArrayList<>();
names.add("Ann");
String s = names.get(0);      // без каста; положить сюда Integer компилятор не даст
```

## 2. Обобщённые методы и классы

**Тип-параметр** `<T>` объявляют перед возвращаемым типом (у метода) или после имени (у класса):

```java
static <T> T firstOf(List<T> list) { return list.get(0); }   // обобщённый метод

class Box<T> {                 // обобщённый класс
    private final T value;
    Box(T value) { this.value = value; }
    T get() { return value; }
}
```

Параметров может быть несколько: `class Pair<A, B>`, `Map<K, V>`. Имена по соглашению: `T` (type),
`E` (element), `K`/`V` (key/value), `R` (result).

## 3. Границы типа (bounded type parameters)

`<T extends Bound>` требует, чтобы `T` был подтипом `Bound` — тогда внутри можно вызывать методы
границы:

```java
static <T extends Comparable<T>> T maxOf(List<T> list) {
    T best = list.get(0);
    for (T x : list) if (x.compareTo(best) > 0) best = x;   // compareTo доступен благодаря границе
    return best;
}
```

`extends` в границе работает и для классов, и для интерфейсов; можно несколько:
`<T extends Number & Comparable<T>>`.

## 4. Вариантность: ключевая идея темы

**Вопрос вариантности:** если `Integer` — подтип `Number`, то как связаны `List<Integer>` и
`List<Number>`? Ответ зависит от вида контейнера.

### Инвариантность (дженерики по умолчанию)

`List<Integer>` **НЕ является** `List<Number>` и наоборот. Дженерики **инвариантны**. Это нарочно —
ради безопасности:

```java
List<Integer> ints = new ArrayList<>();
// List<Number> nums = ints;      // ОШИБКА КОМПИЛЯЦИИ (и хорошо!)
// nums.add(3.14);                // иначе бы положили Double в List<Integer>
```

### Ковариантность массивов — и почему это плохо

Массивы, в отличие от дженериков, **ковариантны**: `Integer[]` считается `Object[]`. Звучит удобно,
но дырявит типобезопасность — ошибка всплывает лишь в рантайме:

```java
Object[] arr = new Integer[2];    // компилируется: массивы ковариантны
arr[0] = "не число";              // компилируется... но бросает ArrayStoreException В РАНТАЙМЕ
```

Дженерики сделали инвариантными именно чтобы такой баг ловился на компиляции, а не падал у
пользователя. (Это задача С12.)

### Ковариантность и контравариантность на стороне использования — wildcards

Иногда гибкость всё же нужна. Java даёт её через **wildcards** (`?`) — это **вариантность на месте
использования** (use-site variance):

- **`? extends T` — ковариантность (producer).** «Какой-то подтип `T`». Из такой коллекции можно
  **читать** как `T`, но **нельзя писать** (компилятор не знает точный подтип):

  ```java
  double sum(List<? extends Number> list) {     // примет List<Integer>, List<Double>…
      double s = 0;
      for (Number n : list) s += n.doubleValue(); // читаем — ок
      // list.add(1);                             // писать НЕЛЬЗЯ
      return s;
  }
  ```

- **`? super T` — контравариантность (consumer).** «Какой-то супертип `T`». В такую коллекцию можно
  **писать** `T`, но **читать** только как `Object`:

  ```java
  void addInts(List<? super Integer> dst, int n) {  // примет List<Integer>, List<Number>, List<Object>
      for (int i = 1; i <= n; i++) dst.add(i);       // пишем Integer — ок
  }
  ```

- **`?` — неограниченный wildcard.** Тип не важен вовсе (`List<?>`, `Collection<?>`) — читаем как
  `Object`, писать (кроме `null`) нельзя.

### Мнемоника PECS

> **P**roducer **E**xtends, **C**onsumer **S**uper.

Если структура **отдаёт** тебе `T` — `? extends T`. Если **принимает** `T` — `? super T`. Классика —
копирование:

```java
static <T> void copyAll(List<? extends T> src, List<? super T> dst) {  // src produces, dst consumes
    for (T t : src) dst.add(t);
}
```

> В Kotlin вариантность объявляют **на месте определения** (declaration-site): `out T` ≈ `? extends T`
> (ковариантно, как `List<out T>`), `in T` ≈ `? super T` (контравариантно). Идея та же — просто там
> её задаёт автор класса, а в Java каждый вызывающий пишет wildcard сам.

## 5. Стирание типов (type erasure)

Дженерики существуют **только в компиляторе**. В байткоде параметры типа **стираются**: `List<String>`
и `List<Integer>` в рантайме — один и тот же `List` (точнее `ArrayList`):

```java
List<String> a = new ArrayList<>();
List<Integer> b = new ArrayList<>();
a.getClass() == b.getClass();     // true — один рантайм-класс (СЛ16)
```

Следствия (что **нельзя** из-за стирания):

- `new T[]` и `new T()` — тип неизвестен в рантайме. Массив создают снаружи (шаблон) или через
  `Array.newInstance`; объект — через фабрику/`Supplier`.

  ```java
  static <T> T[] toArray(List<T> list, T[] template) { return list.toArray(template); }
  ```

- `o instanceof T` — нельзя. Вместо этого передают **токен типа** `Class<T>`:

  ```java
  static <T> T castOrNull(Object o, Class<T> type) {
      return type.isInstance(o) ? type.cast(o) : null;
  }
  ```

- нельзя иметь `catch (MyException<String> e)`, статические поля типа `T`, перегрузку, различающуюся
  только параметром типа (`foo(List<String>)` и `foo(List<Integer>)` — один метод после стирания).

- **Wildcard capture.** Метод с `List<?>` иногда должен «поймать» неизвестный тип во внутренний
  параметр — делегируя приватному обобщённому помощнику:

  ```java
  public static void reverse(List<?> list) { reverseHelper(list); }      // <?> captured...
  private static <T> void reverseHelper(List<T> list) { /* работаем с T */ }
  ```

> Стирание — причина, по которой в Android/Java для рантайм-информации о типе используют `Class<T>`,
> `TypeToken` (Gson) или `reified`-параметры в Kotlin (там компилятор подставляет тип на месте вызова).

---

## Что дальше

Реши 20 задач в [`Tasks.java`](Tasks.java): Л1–Л8, С9–С15, СЛ16–СЛ20. Особое внимание — вариантности
(С9–С12) и стиранию (СЛ16–СЛ19). Прогон: `./gradlew test --tests "handbook.java.generics.*"`.
