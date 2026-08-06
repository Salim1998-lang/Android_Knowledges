# Тема 7. Горячие потоки: `StateFlow` / `SharedFlow`

Тема 6 разбирала **холодные** `Flow`: код выполняется заново на каждый `collect`, у каждого
подписчика свой прогон. Здесь — **горячие** потоки: они живут независимо от подписчиков и вещают
значения **всем** сразу. Это основа UI-состояния и шины событий в Android.

## 1. Холодный vs горячий — в чём разница

| | Холодный (`Flow`, `flow { }`) | Горячий (`StateFlow`/`SharedFlow`) |
|-|-------------------------------|-------------------------------------|
| Когда работает | только во время `collect`, заново на каждого | всегда «живой», независимо от подписчиков |
| Подписчики | у каждого свой прогон | делят один поток значений (broadcast) |
| Значение «сейчас» | нет | у `StateFlow` — есть (`.value`) |
| Аналогия | рецепт | радиоэфир |

> 🧠 **Одной фразой.** Холодный — «плеер по запросу» (каждому своё с начала). Горячий — «прямой
> эфир» (все слышат то, что идёт сейчас).

## 2. `StateFlow` — наблюдаемое состояние

`StateFlow<T>` — горячий поток с **всегда актуальным значением**. По сути — реактивная переменная:
новый подписчик мгновенно получает **текущее** значение, затем все последующие изменения.

```kotlin
val state = MutableStateFlow(0)     // начальное значение обязательно
state.value                          // прочитать текущее
state.value = 1                      // записать (эмитит подписчикам)
state.update { it + 1 }              // атомарное read-modify-write (без гонок)
state.compareAndSet(expect = 2, update = 3)
```

Два свойства, которые постоянно спрашивают на собесе:
- **Конфляция.** `StateFlow` хранит только **последнее** значение. Если подписчик медленный, он
  пропустит промежуточные и получит самое свежее — старые «схлопываются».
- **Дедупликация.** Установка **того же** значения (`==`) подписчиков **не будит** — как
  встроенный `distinctUntilChanged`.

## 3. `SharedFlow` — шина событий

`SharedFlow<T>` — горячий broadcast **без** понятия «текущего значения». Для событий, которые должны
случиться (навигация, тост, «показать диалог»), а не для состояния.

```kotlin
val events = MutableSharedFlow<String>(
    replay = 0,                                  // сколько последних отдавать новым подписчикам
    extraBufferCapacity = 64,                    // буфер сверх replay
    onBufferOverflow = BufferOverflow.SUSPEND,   // что делать при переполнении
)
events.emit("clicked")       // suspend: ждёт место при переполнении
events.tryEmit("clicked")    // не-suspend: вернёт false, если места нет
events.replayCache           // последние replay значений
```

- **`replay`** — сколько последних значений получит новый подписчик (у `SharedFlow` по умолчанию 0 —
  опоздавший не увидит прошлых событий).
- **`onBufferOverflow`** — `SUSPEND` (ждать), `DROP_OLDEST` (выкинуть старое), `DROP_LATEST`
  (выкинуть новое). `tryEmit` + `DROP_OLDEST` — типичная «неблокирующая» шина.

> 🧠 **Правило выбора.** Нужно «какое сейчас значение» (состояние экрана, выбранный таб) →
> **`StateFlow`**. Нужно «случилось событие» (одноразовое, без текущего значения) → **`SharedFlow`**.

## 4. `MutableXxx` наружу отдают только для чтения

Изнутри (ViewModel) пишут в `MutableStateFlow`/`MutableSharedFlow`, а наружу отдают **read-only**
проекцию, чтобы UI не мог менять состояние:

```kotlin
private val _state = MutableStateFlow(UiState.Loading)
val state: StateFlow<UiState> = _state.asStateFlow()      // наружу — только чтение
```

## 5. Из холодного в горячий: `stateIn` / `shareIn`

Часто источник — холодный `Flow` (из репозитория), а UI нужен горячий, разделяемый на многих
подписчиков поток. Конвертируют операторами `stateIn`/`shareIn`, привязывая к **scope**:

```kotlin
val uiState: StateFlow<List<Item>> = repository.itemsFlow()
    .map { it.toUi() }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),   // когда «включать» апстрим
        initialValue = emptyList(),
    )
```

- **`stateIn`** → `StateFlow` (нужно `initialValue`). **`shareIn`** → `SharedFlow` (нужен `replay`).
- **scope** определяет время жизни: пока scope жив, поток разделяется, а не пересобирается на каждого.

## 6. `SharingStarted` — когда включать апстрим

| Стратегия | Когда апстрим активен |
|-----------|-----------------------|
| `Eagerly` | сразу и до конца scope (даже без подписчиков) |
| `Lazily` | с первого подписчика и до конца scope |
| `WhileSubscribed(stopTimeout)` | пока есть подписчики (+ таймаут после ухода последнего) |

`WhileSubscribed(5_000)` — стандарт для Android: апстрим работает, только когда на экране есть
наблюдатель, и переживает поворот экрана (5 секунд «grace period»).

## 7. Почему нужен scope и `backgroundScope` в тестах

Горячий поток **бесконечен** — его `collect` никогда не завершится сам. Поэтому:
- в проде он привязан к **scope** (`viewModelScope`), который его отменит;
- в тесте коллектор запускают в **`backgroundScope`** (тема 10), иначе `runTest` повиснет, ожидая
  завершения.

```kotlin
@Test fun x() = runTest {
    val state = MutableStateFlow(0)
    val seen = mutableListOf<Int>()
    backgroundScope.launch { state.collect { seen.add(it) } }   // не блокирует runTest
    runCurrent()                                                // дать коллектору подписаться/отработать
    state.value = 1; runCurrent()
}
```

## 8. `StateFlow` vs `LiveData` (для Android-контекста)

`StateFlow` — «корутинная» замена `LiveData`: не привязан к главному потоку, комбинируется
операторами `Flow`, требует явного scope и (в UI) сбора через `repeatOnLifecycle`. `LiveData` проще
и знает про жизненный цикл сам, но беднее и завязан на Android.

## 9. Чек-лист

- Состояние с текущим значением → `StateFlow` (конфляция + дедупликация).
- События без «текущего» → `SharedFlow` (настрой `replay`/`onBufferOverflow`).
- Наружу — `asStateFlow()`/`asSharedFlow()` (read-only).
- Холодный источник → горячий: `stateIn`/`shareIn` + `SharingStarted` + scope.
- Android по умолчанию: `WhileSubscribed(5_000)`.

---

## Задачи (`Tasks.kt`) — 20 штук

**Лёгкие (1–8):** `MutableStateFlow`/`.value`/`update`/`compareAndSet`/`asStateFlow`,
`MutableSharedFlow(replay)`/`emit`.
**Средние (9–15):** `replayCache`, `onBufferOverflow = DROP_OLDEST`, `subscriptionCount`, `first`,
`stateIn`/`shareIn`, комбинирование `StateFlow`.
**Сложные (16–20):** дедуплицирующий сбор `StateFlow`, `runningReduce` + `stateIn`, `take` из
`SharedFlow`, трансформация `StateFlow` (`map` + `stateIn`), ленивое `WhileSubscribed`.

Эталон — в [`solutions/Solutions.kt`](solutions/Solutions.kt).
