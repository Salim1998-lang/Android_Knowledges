# Тема 4. Обработка исключений

## 1. Главная идея

Исключения в корутинах — не как в обычном коде. Здесь ошибка **распространяется по дереву
корутин** и по умолчанию работает по принципу *fail-fast*: упал один — отменяются связанные.
Понимание этого — про надёжность: где ловить, где изолировать, что не глотать.

## 2. Как всплывает ошибка: `launch` vs `async`

- **`launch`**: исключение всплывает **немедленно**, вверх к родителю, отменяя его и сиблингов.
  Если не поймано внутри — уходит в `CoroutineExceptionHandler` корня (или крашит).
- **`async`**: исключение **откладывается** и хранится в `Deferred`. Оно бросается при `await()`.
  Но! Ошибка **также** отменяет родительский scope (если это `coroutineScope`, а не супервизор).

```kotlin
// launch: ошибка всплывает сама
launch { error("boom") }             // → отменит родителя

// async: ошибку нужно забрать через await
val d = async { error("boom") }
d.await()                            // ← здесь бросится
```

> Оборотная сторона `async`: если `Deferred` **никто не `await`-ит**, его исключение может
> **потеряться**. В `coroutineScope` оно всё равно всплывёт к родителю (отменит scope), но в
> `supervisorScope` или у корневого `GlobalScope.async` — молча осядет в `Deferred`. Взял `async`
> ради результата — обязательно забери его через `await()`.

## 3. `coroutineScope` перебрасывает ошибку — её можно поймать

`coroutineScope { }` при падении любого ребёнка отменяет остальных детей и **пробрасывает**
исключение из билдера. Значит, обычный `try/catch` вокруг `coroutineScope` работает:

```kotlin
try {
    coroutineScope {
        launch { error("boom") }    // отменит scope
        launch { longWork() }       // будет отменён
    }
} catch (e: IllegalStateException) {
    // попадём сюда: coroutineScope перебросил ошибку
}
```

> `try/catch` **вокруг `launch { }`** ошибку не поймает — она всплывает не туда. Ловить нужно
> либо **внутри** корутины, либо вокруг `coroutineScope`/`await()`.

## 4. `CancellationException` — не ошибка (повтор из темы 3, критично)

`CancellationException` = нормальная отмена. Она:
- не идёт в `CoroutineExceptionHandler`;
- не отменяет родителя;
- **должна пробрасываться**, а не глотаться.

Отсюда золотое правило перехвата в корутинах:

```kotlin
try { work() }
catch (e: CancellationException) { throw e }   // пробросить!
catch (e: Throwable) { handle(e) }             // остальное — обработать
```

## 5. `runCatching` в корутинах опасен

Стандартный `runCatching { }` ловит **всё**, включая `CancellationException` — и тем самым
ломает отмену. В корутинах пиши «корутино-безопасный» вариант:

```kotlin
suspend fun <T> coRunCatching(block: suspend () -> T): Result<T> =
    try { Result.success(block()) }
    catch (e: CancellationException) { throw e }
    catch (e: Throwable) { Result.failure(e) }
```

## 6. `supervisorScope` / `SupervisorJob` — изоляция ошибок

Иногда дети должны быть **независимы**: падение одного не должно рушить остальных (несколько
виджетов, независимые загрузки). Тогда — `supervisorScope`:

```kotlin
supervisorScope {
    launch { widgetA() }   // упадёт — не тронет B
    launch { widgetB() }
}
```

Правила супервизора:
- ошибка **`launch`-ребёнка** НЕ всплывает к родителю — она идёт в `CoroutineExceptionHandler`
  этого ребёнка (или в дефолтный);
- ошибка **`async`-ребёнка** по-прежнему доставляется через `await()` (и не рушит сиблингов);
- отмена родителя вниз к детям — работает как обычно.

Отличие направлений:
- обычный `Job`: ошибка идёт **вверх** (child → parent) И **вниз** (parent → children).
- `SupervisorJob`: ошибка идёт только **вниз**. Вверх (от ребёнка к родителю) — не идёт.

### Ловушка: изоляцию даёт `supervisorScope`, а НЕ `SupervisorJob()` в контексте

Нельзя получить супервизор-семантику, подсунув `SupervisorJob()` в `coroutineScope`/`withContext` —
эти билдеры создают **свой обычный `Job`** как родителя детей:

```kotlin
// ❌ НЕ изолирует: у coroutineScope собственный обычный Job
coroutineScope {
    launch { error("boom") }   // падение отменит scope И сиблинга
    launch { work() }
}
// SupervisorJob в родительском контексте на это не влияет — coroutineScope ставит свой Job.
```

Изоляция работает, только когда **непосредственный родитель** детей — `SupervisorJob`. Это и
делает `supervisorScope`:

```kotlin
// ✅ прямой родитель детей — SupervisorJob
supervisorScope {
    launch { error("boom") }   // сиблинга НЕ трогает
    launch { work() }
}
```

И даже с супервизором изолируются лишь его **прямые** дети: `launch { launch { error("x") } }`
внутри `supervisorScope` уронит внешний `launch` — внутренний привязан к обычному `Job` внешнего,
а не к супервизору.

## 7. `CoroutineExceptionHandler` — «последний рубеж»

Обработчик необработанных исключений **корневой** корутины. Работает только для `launch`
(не для `async` — там ошибку забирают через `await`):

```kotlin
val handler = CoroutineExceptionHandler { _, e -> log.error("caught", e) }
val scope = CoroutineScope(SupervisorJob() + handler)
scope.launch { error("boom") }   // e попадёт в handler, приложение не упадёт
```

Ставить его имеет смысл на **корне** scope (или на корневом `launch`). На вложенных корутинах он
игнорируется — они делегируют обработку родителю.

## 8. Несколько ошибок сразу

Если после первой ошибки при отмене падают ещё и другие (например, в `finally`), они
**прикрепляются** к первой как `suppressed`:

```kotlin
try { ... } catch (e: Exception) {
    e.suppressed.forEach { println("подавлено: $it") }
}
```

Надёжный источник `suppressed` — **try-with-resources** (`use`): если и основной блок, и `close()`
бросают, ошибка `close()` сохраняется как `suppressed` у основной (см. задачу Д21). А вот
агрегирование вторичных исключений **при отмене корутин** зависит от версии/пути и не гарантировано —
на практике `e.suppressed` там часто пуст, так что не полагайся на него.

## 9. `awaitAll` и ошибки

`listOf(d1, d2, ...).awaitAll()` падает на **первой** же ошибке и отменяет остальные — это
fail-fast для батча. Если нужно «собрать всё, включая ошибки» — оборачивай каждый `await()` в
`coRunCatching` под `supervisorScope` (получишь `List<Result<T>>`).

## 10. Шпаргалка «где что ловить»

| Хочу | Как |
|------|-----|
| поймать ошибку параллельного блока | `try { coroutineScope { ... } }` или `try { d.await() }` |
| чтобы падение одного не рушило других | `supervisorScope { }` |
| собрать успехи и ошибки отдельно | `supervisorScope` + `coRunCatching` → `List<Result>` |
| не потерять необработанную ошибку `launch` | `CoroutineExceptionHandler` на корне |
| не сломать отмену | всегда `catch (CancellationException) { throw }` |

---

## Задачи (`Tasks.kt`) — 21 штука

**Лёгкие (1–8):** корутино-безопасный `runCatching`, `getOrElse`/`recover`, первый успешный.
**Средние (9–15):** `supervisorScope`-изоляция, разделение успехов/ошибок, `CoroutineExceptionHandler`.
**Сложные (16–20):** fail-fast `awaitAll`, ретрай поверх `Result`, частичные результаты, `CoroutineExceptionHandler`.
**Дополнительные (21):** `suppressed`-исключения через try-with-resources (`use`).

Эталон — в [`solutions/Solutions.kt`](solutions/Solutions.kt).
