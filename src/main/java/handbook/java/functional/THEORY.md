# Тема 6. Функциональщина Java 8+

> Читаешь теорию → решаешь 20 задач в `Tasks.java` → гоняешь тесты:
> `./gradlew test --tests "handbook.java.functional.*"`. Застрял — сверься с
> [`solutions/Solutions.java`](solutions/Solutions.java).

Java 8 принесла лямбды, method references, `Stream API` и `Optional`. Для android-разработчика это
нужно, чтобы читать/писать legacy Java-код (в Kotlin те же идеи выражают иначе), и чтобы понимать, во
что превращаются коллекции-конвейеры. Всё это доступно и на старых Android через **desugaring**.

---

## 1. Функциональные интерфейсы и лямбды

**Функциональный интерфейс** — интерфейс с ровно одним абстрактным методом. Его реализацию можно
записать **лямбдой** — компактной анонимной функцией:

```java
Predicate<Integer> isPositive = n -> n > 0;    // boolean test(Integer)
isPositive.test(5);                             // true
```

Стандартные функциональные интерфейсы из `java.util.function`:

| Интерфейс | Метод | Смысл |
|-----------|-------|-------|
| `Predicate<T>` | `boolean test(T)` | условие |
| `Function<T,R>` | `R apply(T)` | преобразование T→R |
| `Consumer<T>` | `void accept(T)` | побочный эффект |
| `Supplier<T>` | `T get()` | поставщик значения |
| `BiFunction<T,U,R>` | `R apply(T,U)` | два аргумента |
| `UnaryOperator<T>` | `T apply(T)` | T→T |

Функцию можно **передавать как параметр** — это и есть «функции первого класса»:

```java
static int countMatching(List<Integer> nums, Predicate<Integer> p) {
    return (int) nums.stream().filter(p).count();
}
```

Лямбда **захватывает** переменные окружения, но только **effectively final** (как анонимные классы,
тема 2).

## 2. Method references

Если лямбда просто вызывает существующий метод — её заменяют **ссылкой на метод** (короче и читаемее):

| Вид | Пример | Эквивалент лямбды |
|-----|--------|-------------------|
| статический | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| метод экземпляра (по типу) | `String::toUpperCase` | `s -> s.toUpperCase()` |
| метод конкретного объекта | `list::add` | `x -> list.add(x)` |
| конструктор | `ArrayList::new` | `() -> new ArrayList<>()` |

## 3. Stream API

**Stream** — конвейер обработки последовательности. Не хранит данные и не меняет источник, а описывает
шаги. Структура: **источник → промежуточные операции (ленивые) → терминальная операция**.

```java
List<Integer> result = nums.stream()   // источник
    .filter(n -> n % 2 == 0)           // промежуточная (ленивая)
    .map(n -> n * 2)                   // промежуточная (ленивая)
    .collect(Collectors.toList());     // терминальная — запускает конвейер
```

**Промежуточные** (возвращают Stream, ленивые): `map`, `filter`, `distinct`, `sorted`, `limit`,
`skip`, `flatMap`, `peek`. **Терминальные** (запускают обработку): `collect`, `forEach`, `count`,
`reduce`, `findFirst`, `anyMatch`/`allMatch`/`noneMatch`, `min`/`max`, `toList`.

Ленивость: без терминальной операции ничего не выполнится; конвейер обрабатывает элементы «по одному»
насквозь, а не слоями.

### `flatMap`

Разворачивает поток потоков в один поток — для вложенных структур:

```java
List<List<Integer>> nested = ...;
nested.stream().flatMap(List::stream).collect(Collectors.toList());  // [[1,2],[3]] → [1,2,3]
```

### `reduce`

Сворачивает поток в одно значение: начальное (identity) + аккумулятор:

```java
int product = nums.stream().reduce(1, (a, b) -> a * b);   // 1*n1*n2*...
```

Для чисел удобнее примитивные потоки: `mapToInt(...).sum()`, `.average()`, `.max()`,
`.summaryStatistics()` — без автоупаковки.

## 4. Коллекторы (`Collectors`)

Терминальная операция `collect` с готовыми сборщиками:

```java
.collect(Collectors.toList());                 // в List
.collect(Collectors.toSet());                  // в Set
.collect(Collectors.toMap(w -> w, String::length));      // в Map (ключи уникальны!)
.collect(Collectors.joining(", "));            // склейка строк
.collect(Collectors.groupingBy(String::length));        // Map<длина, List<слово>>
.collect(Collectors.counting());               // downstream: количество
```

**`groupingBy` с downstream-коллектором** — группировка + агрегат в значениях:

```java
// буква → сколько слов на неё начинается
groupingBy(s -> s.charAt(0), Collectors.counting());        // Map<Character, Long>
// длина → слова в верхнем регистре
groupingBy(String::length, Collectors.mapping(String::toUpperCase, Collectors.toList()));
```

`toMap` бросит исключение при дубликате ключа — для таких случаев есть перегрузка с merge-функцией.

## 5. `Optional` — вместо `null`

`Optional<T>` — контейнер «значение есть / значения нет», делающий отсутствие **явным** в типе и
защищающий от `NullPointerException`:

```java
Optional<Integer> first = nums.stream().filter(n -> n % 2 == 0).findFirst();
int value = first.orElse(-1);                 // значение или запасное
first.ifPresent(v -> System.out.println(v));  // действие, если есть
int len = maybe.map(String::length).orElse(0);// map применится только при наличии значения
```

Методы: `of`/`ofNullable`/`empty`, `isPresent`/`isEmpty`, `get` (опасен — кинет, если пусто),
`orElse`/`orElseGet`/`orElseThrow`, `map`/`flatMap`/`filter`, `ifPresent`.

Правила хорошего тона: `Optional` — для **возвращаемых значений**, где отсутствие ожидаемо; **не**
делай из него поля/параметры и не вызывай `get()` без проверки.

> **Контекст Kotlin/Android.** В Kotlin роль `Optional` играет **nullable-тип** (`T?`) + оператор
> `?.`/`?:` — компактнее и без обёртки-аллокации. Stream API соответствует
> `sequence`/`list.map{}.filter{}`. Но Java-код (и многие Android-API, RxJava) активно используют
> `Optional`/`Stream` — читать надо уметь. На старых Android эти API работают через **desugaring**
> (D8/R8 переписывает байткод).

---

## Что дальше

Реши 20 задач в [`Tasks.java`](Tasks.java): Л1–Л8, С9–С15, СЛ16–СЛ20. Особое внимание — коллекторам
(С10–С12, СЛ19–СЛ20) и `Optional` (С13–С14, СЛ17). Прогон:
`./gradlew test --tests "handbook.java.functional.*"`.
