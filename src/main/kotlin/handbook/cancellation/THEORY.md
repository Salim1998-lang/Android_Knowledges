# Тема 3. Отмена и таймауты

## 1. Почему отмена — это отдельная большая тема

Отмена в корутинах — не «убить поток», а **кооперативный протокол**. Понять его критично: 90%
«странных» багов с корутинами (зависания, утечки, «почему не останавливается», «почему ресурс не
закрылся») — про неправильную отмену.

## 2. Отмена кооперативна

Корутину нельзя остановить насильно. Отмена работает так: `cancel()` переводит `Job` в состояние
«отменяется» и **бросает `CancellationException` в ближайшей suspend-точке**. Код сам обязан
«сотрудничать» — то есть регулярно попадать в suspend-точки или проверять флаг.

Все `suspend`-функции из `kotlinx.coroutines` (`delay`, `yield`, `await`, ...) — **cancellable**:
при отмене они бросают `CancellationException`. А вот тяжёлый CPU-цикл без suspend-точек отменить
**нельзя** — он не заметит отмену:

```kotlin
// ❌ НЕ реагирует на отмену — досчитает до конца
val job = launch(Dispatchers.Default) {
    var i = 0
    while (i < 1_000_000_000) { i++ }   // ни одной suspend-точки
}
job.cancelAndJoin()   // будет ждать, пока цикл сам не закончится
```

## 3. Как сделать цикл отменяемым

Три инструмента (в `CoroutineScope`/suspend-контексте):

```kotlin
while (isActive) { ... }        // isActive: Boolean — флаг активности
ensureActive()                  // бросит CancellationException, если отменён
yield()                         // уступить поток + проверить отмену (bonus: даёт работать другим)
```

```kotlin
// ✅ Кооперативная версия
val job = launch(Dispatchers.Default) {
    var i = 0
    while (i < 1_000_000_000) {
        ensureActive()   // отмена сработает здесь
        i++
    }
}
```

- `isActive` — дёшево, для «горячих» циклов.
- `ensureActive()` — то же, но сразу бросает (не нужно самому городить `if (!isActive) throw`).
- `yield()` — если хочешь ещё и дать другим корутинам шанс поработать между итерациями.

## 4. `CancellationException` — это НЕ ошибка

`CancellationException` означает **нормальную, ожидаемую отмену**:
- она **не** всплывает как крах приложения;
- она **не** отменяет родителя (в отличие от любого другого исключения);
- поэтому её **нельзя молча глотать** — иначе корутина «проглотит» собственную отмену и продолжит
  жить, что ломает всю модель.

```kotlin
try {
    work()
} catch (e: CancellationException) {
    throw e                       // ⚠️ обязательно пробросить!
} catch (e: Exception) {
    handle(e)                     // ловим только «настоящие» ошибки
}
```

> Тот же капкан в `runCatching { }`: он ловит **всё**, включая `CancellationException`. В корутинах
> «сырой» `runCatching` опасен — см. тему 4.

## 5. Таймауты: `withTimeout` и `withTimeoutOrNull`

```kotlin
// Бросает TimeoutCancellationException, если не успели за 2 с
val data = withTimeout(2000) { loadData() }

// Возвращает null вместо исключения
val data = withTimeoutOrNull(2000) { loadData() } ?: Data.EMPTY
```

`TimeoutCancellationException` — подкласс `CancellationException`. То есть по истечении таймаута
блок **отменяется** ровно по тем же кооперативным правилам: если внутри нет suspend-точек, таймаут
не сработает вовремя.

## 6. Очистка ресурсов при отмене

`finally` выполнится и при нормальном завершении, и при отмене:

```kotlin
val resource = open()
try {
    use(resource)
} finally {
    resource.close()   // выполнится всегда
}
```

**Ловушка:** после отмены корутина уже в состоянии «отменяется», и **любой новый suspend-вызов в
`finally` немедленно бросит `CancellationException`**. То есть `delay(...)`, сетевой запрос,
suspend-логирование в `finally` — не выполнятся:

```kotlin
try { work() }
finally {
    delay(100)         // ❌ мгновенно бросит CancellationException — не выполнится
    log.flushSuspend()
}
```

Решение — `withContext(NonCancellable)`: временно делает участок неотменяемым:

```kotlin
try { work() }
finally {
    withContext(NonCancellable) {
        delay(100)          // ✅ выполнится
        log.flushSuspend()  // ✅ выполнится
    }
}
```

Используй `NonCancellable` **только** для короткой обязательной очистки — не для основной работы
(иначе отмена перестанет работать).

## 7. `suspendCancellableCoroutine` — мост с колбэк-API

Оборачивая колбэчный/блокирующий API в корутину, бери `suspendCancellableCoroutine` (а не
`suspendCoroutine`), чтобы поддержать отмену:

```kotlin
suspend fun await(call: Call): Response = suspendCancellableCoroutine { cont ->
    call.enqueue(onSuccess = { cont.resume(it) }, onError = { cont.resumeWithException(it) })
    cont.invokeOnCancellation { call.cancel() }   // отмена корутины → отмена запроса
}
```

## 8. Отмена распространяется по дереву

- Отмена родителя → отменяются все дети.
- `job.cancelAndJoin()` — отменить и дождаться фактического завершения (важно: `cancel()` только
  запускает отмену, но не ждёт её завершения).
- Отмена одного ребёнка (обычной `CancellationException`) **не** трогает родителя и сиблингов.

## 9. Тестирование отмены

В `runTest`:
- `advanceTimeBy(ms)` — промотать виртуальное время (сработают таймеры/таймауты);
- `job.cancelAndJoin()` — отменить и дождаться;
- проверяй `job.isCancelled`, счётчики «тиков», выполнилась ли очистка.

## 10. Чек-лист

- Длинные CPU-циклы → `ensureActive()` / `isActive` / `yield()`.
- Поймал `CancellationException` → пробрось его.
- Обязательная очистка после отмены → `withContext(NonCancellable)`.
- Нужен дедлайн → `withTimeout` / `withTimeoutOrNull`.
- Оборачиваешь колбэк → `suspendCancellableCoroutine` + `invokeOnCancellation`.
- Отменяешь и ждёшь → `cancelAndJoin()`.

---

## Задачи (`Tasks.kt`) — 20 штук

**Лёгкие (1–8):** `withTimeout(OrNull)`, `isActive`/`ensureActive`/`yield`, `cancelAndJoin`.
**Средние (9–15):** кооперативные циклы, таймаут с фолбэком, подсчёт тиков до отмены.
**Сложные (16–20):** `NonCancellable`-очистка, гонка с дедлайном, ретрай с таймаутом на попытку.

Эталон — в [`solutions/Solutions.kt`](solutions/Solutions.kt).
