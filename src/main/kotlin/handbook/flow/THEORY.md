# Тема 5. Flow

## 1. Что такое Flow и зачем он

`Flow<T>` — **холодный асинхронный поток** значений. Если `suspend`-функция возвращает **одно**
значение позже, то `Flow` возвращает **много** значений во времени. Это `suspend`-аналог
`Sequence`: значения появляются постепенно, а между ними код может приостанавливаться.

Примеры: строки лога по мере поступления, обновления цены, страницы пагинации, события UI.

## 2. «Холодный» = ленивый

Код внутри `flow { }` **не выполняется**, пока кто-то не начнёт собирать (`collect`). Каждый новый
`collect` запускает поток **заново** с нуля:

```kotlin
fun numbers(): Flow<Int> = flow {
    println("старт")     // выполнится при КАЖДОМ collect
    for (i in 1..3) {
        delay(100)
        emit(i)          // излучаем значение
    }
}

numbers().collect { println(it) }   // старт, 1, 2, 3
numbers().collect { println(it) }   // старт, 1, 2, 3 — заново
```

Сравни: `Channel` (тема 6) — **горячий**, значения «летят» независимо от подписчиков.

## 3. Построение потоков

```kotlin
flow { emit(1); emit(2) }        // ручная сборка
flowOf(1, 2, 3)                  // из фиксированных значений
listOf(1, 2, 3).asFlow()         // из коллекции
(1..100).asFlow()                // из диапазона
channelFlow { send(x) }          // из нескольких корутин (можно параллельно)
```

Внутри `flow { }` **нельзя** менять контекст напрямую (`withContext` → ошибка). Для смены
диспетчера — оператор `flowOn` (см. §7).

## 4. Промежуточные операторы (ленивые, возвращают Flow)

Ничего не запускают сами — только описывают преобразование:

```kotlin
flow.map { it * 2 }
    .filter { it > 3 }
    .take(10)                       // первые 10 и заверши
    .drop(2)                        // пропустить первые 2
    .onEach { log(it) }             // побочный эффект на каждый
    .distinctUntilChanged()         // убрать подряд идущие дубликаты
    .runningReduce { a, b -> a + b } // нарастающий аккумулятор
    .transform { emit(it); emit(-it) } // 0..N выходных на каждый входной
```

Есть и `suspend`-версии предикатов: `filter`, `map` могут вызывать `suspend`-функции внутри.

## 5. Терминальные операторы (запускают сбор, `suspend`)

```kotlin
flow.collect { ... }               // основной способ потребления
val list = flow.toList()
val set  = flow.toSet()
val first = flow.first()
val firstOrNull = flow.firstOrNull()
val count = flow.count()
val sum  = flow.reduce { a, b -> a + b }
val acc  = flow.fold(0) { a, b -> a + b }
```

## 6. Аккумуляция: `scan` / `runningReduce`

`scan(initial) { acc, x -> ... }` излучает **каждое** промежуточное состояние (включая initial):

```kotlin
flowOf(1, 2, 3).scan(0) { acc, x -> acc + x }.toList()   // [0, 1, 3, 6]
flowOf(1, 2, 3).runningReduce { acc, x -> acc + x }.toList() // [1, 3, 6]
```

Часто `scan(0){..}.drop(1)` — чтобы не излучать начальное значение.

## 7. Контекст выполнения: `flowOn`

Тело `flow { }` по умолчанию исполняется в контексте **сборщика**. Сменить диспетчер для
**вышестоящей** (upstream) части — `flowOn`:

```kotlin
flow { emit(readFromDisk()) }   // тяжёлый IO
    .map { parse(it) }
    .flowOn(Dispatchers.IO)     // всё ВЫШЕ flowOn — на IO
    .collect { render(it) }     // collect — в исходном контексте (напр. Main)
```

`flowOn` влияет только на то, что **выше** него по цепочке.

## 8. Backpressure: `buffer`, `conflate`, `collectLatest`

Когда производитель быстрее потребителя:
- `buffer(n)` — буфер: producer и collector работают параллельно, не ждут друг друга;
- `conflate()` — потреблять только **последнее**, промежуточные пропускать;
- `collectLatest { }` — при новом значении **отменять** обработку предыдущего.

```kotlin
fastFlow.conflate().collect { slowRender(it) }        // рисуем только актуальное
fastFlow.collectLatest { slowRender(it) }             // прервать устаревший рендер
```

## 9. Обработка ошибок: `catch`, `onCompletion`, `retry`

```kotlin
flow.onStart { emit(loading) }
    .catch { e -> emit(fallback) }     // ловит ошибки ВЫШЕ по потоку
    .onCompletion { cause -> log(cause) } // вызовется и при успехе, и при ошибке
    .retry(3) { it is IOException }    // повторить upstream при ошибке
```

Важно: `catch` перехватывает исключения только **выше** себя по цепочке (upstream). Ошибку в
`collect { }` он не поймает — её оборачивай обычным `try/catch`.

## 10. Комбинирование потоков

```kotlin
a.zip(b) { x, y -> x + y }         // попарно; ждёт по значению из каждого
a.combine(b) { x, y -> x + y }     // на любое новое значение любого — свежая пара
a.flatMapConcat { f(it) }          // последовательно разворачивать
a.flatMapMerge { f(it) }           // параллельно разворачивать и сливать
a.flatMapLatest { f(it) }          // при новом входном — отменить прошлый под-поток
merge(a, b)                        // слить два потока
```

## 11. StateFlow / SharedFlow (горячие) — краткое знакомство

- `StateFlow<T>` — «горячий» поток-состояние с текущим значением (`.value`), как наблюдаемая
  переменная. Всегда есть значение, новым подписчикам сразу отдаётся последнее.
- `SharedFlow<T>` — «горячий» broadcast событий на многих подписчиков.

Их наполняют через `MutableStateFlow(initial)` / `MutableSharedFlow()`. Это основа для UI-состояния.
(Подробно — за пределами базовой темы; знай, что они есть.)

## 12. Ментальная модель

- `Flow` = рецепт, который выполняется на каждый `collect`.
- Промежуточные операторы = ленивые преобразования рецепта.
- Терминальный оператор = «готовить»: только тут всё запускается.
- `flowOn` — где готовить; `buffer/conflate/collectLatest` — что делать при перегрузке;
  `catch/retry/onCompletion` — что при ошибках.

## 13. Мост с колбэк-API: `callbackFlow` / `channelFlow`

Обычный `flow { }` эмитит из **одной** корутины и не годится, когда значения приходят «извне» —
из колбэка слушателя (клик, локация, сокет) или из **нескольких** корутин сразу. Для этого есть
строители на канале.

**`callbackFlow`** — обернуть колбэк-API в холодный `Flow`. Три обязательные части:

```kotlin
fun locationUpdates(client: LocationClient): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback {
        override fun onLocation(loc: Location) { trySend(loc) }   // 1) колбэк → поток
        override fun onDone() { close() }                         // 2) конец → close()
    }
    client.register(callback)
    awaitClose { client.unregister(callback) }                   // 3) очистка при отмене сбора
}
```

- `trySend(x)` — неблокирующая отправка из колбэка (сам колбэк не `suspend`);
- `close()` — завершить поток;
- `awaitClose { }` — **обязателен**: приостанавливает строитель, пока идёт сбор, и выполняет
  очистку (отписку), когда сбор отменён/завершён. Без него — утечка слушателя (и падение в рантайме).

**`channelFlow`** — когда эмитить нужно из **нескольких** корутин конкурентно (чего `flow { }`
запрещает). Внутри доступны `launch` и `send`:

```kotlin
fun merge(a: Flow<Int>, b: Flow<Int>): Flow<Int> = channelFlow {
    launch { a.collect { send(it) } }
    launch { b.collect { send(it) } }
}   // оба источника собираются параллельно, шлют в один поток
```

> 🧠 **Когда что.** Один источник, эмиссия из одного места → обычный `flow { }`. Колбэк/слушатель
> извне → `callbackFlow` + `awaitClose`. Несколько конкурентных продюсеров → `channelFlow`.

---

## Задачи (`Tasks.kt`) — 22 штуки

**Лёгкие (1–8):** `flow`/`flowOf`/`asFlow`, `map`/`filter`/`take`/`drop`/`toList`/`count`.
**Средние (9–15):** `scan`/`runningReduce`, `transform`, `distinctUntilChanged`, `zip`, `onEach`.
**Сложные (16–20):** кастомный `chunked`, `catch`+фолбэк, `retry`, `flatMapConcat`, дебаунс-подобное.
**Мост с колбэк-API (21–22):** `callbackFlow` + `awaitClose`, `channelFlow` с конкурентными продюсерами.

Эталон — в [`solutions/Solutions.kt`](solutions/Solutions.kt).
