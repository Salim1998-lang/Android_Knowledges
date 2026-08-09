# Android Handbook 🤖

Открытая **база знаний по Android**: теория, задачи с тестами и разборы. Материал накапливается
темами — начиная с корутин Kotlin и мобильного систем-дизайна.

## 🧵 Корутины Kotlin

Раздел для **глубокого изучения корутин** Kotlin. 10 тем, в каждой — подробная теория и
**~20 задач** трёх уровней сложности (лёгкие + средние + сложные) + тесты, которые проверяют
твоё решение. Всего **209 задач** с эталонными решениями.

> 📚 Репозиторий растёт в **открытую базу знаний по Android**. Помимо корутин здесь появляются и
> другие темы — начиная с [**мобильного систем-дизайна**](docs/system-design/README.md).

## Как это устроено

```
src/main/kotlin/handbook/
  <тема>/
    THEORY.md          ← теория с примерами (читать первым)
    Tasks.kt           ← 20 ЗАДАЧ: функции с KDoc и телом TODO(), которые ты реализуешь
    solutions/
      Solutions.kt     ← эталонные решения всех 20 задач (подсмотреть, если застрял)
src/test/kotlin/handbook/
  <тема>/<Тема>Test.kt ← тесты проверяют твои реализации из Tasks.kt
```

Нумерация задач в `Tasks.kt` и тестах: **Л1–Л8** (лёгкие), **С9–С15** (средние), **СЛ16–СЛ20**
(сложные).

## Рабочий цикл

1. Читаешь `THEORY.md` темы.
2. Открываешь `Tasks.kt` — там функции с описанием в KDoc и телом `TODO()`.
3. Реализуешь функцию.
4. Запускаешь тесты темы — зелёные значит решено верно:
   ```bash
   ./gradlew test --tests "handbook.basics.*"
   ```
5. Застрял → сверься с `solutions/Solutions.kt`.

Запустить **все** тесты сразу:
```bash
./gradlew test
```
> Изначально ВСЕ тесты КРАСНЫЕ с `NotImplementedError` — это норма: каждый тест делает зелёным
> твоя реализация в `Tasks.kt`. Начни с темы `basics` и иди по порядку. Застрял — в каждой теме
> есть `solutions/Solutions.kt` с эталоном.

Напечатать оглавление:
```bash
./gradlew run
```

## Порядок изучения тем

| № | Тема | Пакет | О чём |
|---|------|-------|-------|
| 1 | Основы | `handbook.basics` | `suspend`, `runBlocking`, `launch`, `delay`, `join`, `Job` |
| 2 | Структурная конкурентность | `handbook.structured` | `coroutineScope`, `async`/`await`, параллельная декомпозиция |
| 3 | Отмена и таймауты | `handbook.cancellation` | кооперативная отмена, `isActive`, `withTimeout`, `NonCancellable` |
| 4 | Обработка исключений | `handbook.exceptions` | `try/catch`, `SupervisorJob`, `CoroutineExceptionHandler` |
| 5 | Диспетчеры и контекст | `handbook.dispatchers` | `Dispatchers` (IO/Default/Main), `CoroutineContext`, `withContext`, наследование, `limitedParallelism`, `CoroutineName` |
| 6 | Flow | `handbook.flow` | холодные потоки, операторы, `map`/`filter`/`flatMap`, backpressure |
| 7 | Горячие потоки | `handbook.hotflows` | `StateFlow`/`SharedFlow`, `replay`/`onBufferOverflow`, `stateIn`/`shareIn`, `SharingStarted`, конфляция vs холодный `Flow` |
| 8 | Каналы | `handbook.channels` | `Channel`, `produce`, pipelines, fan-in / fan-out |
| 9 | Разделяемое состояние | `handbook.concurrency` | гонки, `Mutex`, атомики/CAS, замыкание на поток, actor, lock striping |
| 10 | Тестирование корутин | `handbook.testing` | `runTest`, виртуальное время, `currentTime`/`advanceTimeBy`, `TestDispatcher` (Standard/Unconfined), `setMain`, `backgroundScope` |

## 🏗️ База знаний: Систем-дизайн

Отдельный раздел о том, как проектировать мобильные приложения целиком и проходить собеседования по
мобильному систем-дизайну. Только теория и разборы (Markdown), без задач с тестами.

- [Введение и фреймворк «ТАВДИ»](docs/system-design/README.md) — 5 шагов любого систем-дизайн собеса.
- [Разбор: Трейдинговая платформа](docs/system-design/01-trading-platform.md) — REST + WebSocket,
  `BigDecimal` для денег, графики через WebView, буферизация real-time обновлений. + вопросы для
  самопроверки.
- [Разбор: Лента новостей](docs/system-design/02-news-feed.md) — чистый REST + JSON, курсорная
  пагинация, SSOT и офлайн, оптимистичные обновления, HTML + нативный рендеринг, плавная прокрутка.
  + вопросы для самопроверки.
- [Разбор: Мессенджер](docs/system-design/03-messenger.md) — гибрид HTTP + WebSocket, real-time
  (опрос / долгий опрос / SSE / WS), жизненный цикл сообщения, гибридная сортировка, очередь исходящих
  с повторами, SQLite, пуш-уведомления. + вопросы для самопроверки.
- [Разбор: Библиотека пагинации](docs/system-design/04-pagination-library.md) — дизайн библиотеки, а не
  приложения: дженерик `Paginator<T>`, offset/cursor, DI, двойной кэш память+диск, приоритеты запросов,
  модульность под async-фреймворки, SemVer/CalVer. + вопросы для самопроверки.

## Требования / запуск

- JDK 21 (в IntelliJ выбран автоматически через toolchain). Из CLI при новом Java укажи JDK 21:
  ```bash
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test
  ```
- Библиотеки (см. `gradle/libs.versions.toml`):
  - `kotlinx-coroutines-core` — сами корутины.
  - `kotlinx-coroutines-test` — `runTest`, виртуальное время, `TestDispatcher`.

Удачи! Начни с [`handbook/basics/THEORY.md`](src/main/kotlin/handbook/basics/THEORY.md).
