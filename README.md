# Android Handbook 🤖

Открытая **база знаний по Android**: теория, задачи с тестами и разборы. Готовые разделы — корутины
Kotlin, Java для Android (сеньор-уровень) и мобильный систем-дизайн. В работе — модуль
[**многопоточность в Android**](#-многопоточность-в-android) (сеньор-уровень).

## 🧵 Корутины Kotlin

Раздел для **глубокого изучения корутин** Kotlin. 10 тем, в каждой — подробная теория и
**~20 задач** трёх уровней сложности (лёгкие + средние + сложные) + тесты, которые проверяют
твоё решение. Всего **209 задач** с эталонными решениями.

> 📚 Репозиторий растёт в **открытую базу знаний по Android**. Помимо корутин здесь есть модуль
> [**Java для Android**](#-java-для-android) (сеньор-уровень + задачи) и раздел
> [**мобильного систем-дизайна**](docs/system-design/README.md).

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

## ☕ Java для Android

Раздел про **Java на сеньор-уровне** — то, что нужно android-разработчику и спрашивают на
собеседованиях, без легаси-мусора (AWT/Swing, апплеты, RMI, JDBC…). Устройство — как у корутин:
**10 тем**, в каждой `THEORY.md` + **20 задач** трёх уровней (Л1–Л8 / С9–С15 / СЛ16–СЛ20) + тесты.
Всего **200 задач** с эталонными решениями. Отличие: задачи и решения — на **настоящей Java**
(`src/main/java/handbook/java/`), а тема interop использует и Kotlin.

```
src/main/java/handbook/java/
  <тема>/
    THEORY.md          ← теория (читать первым)
    Tasks.java         ← 20 ЗАДАЧ: тела кидают UnsupportedOperationException("TODO ...")
    solutions/
      Solutions.java   ← эталонные решения
src/test/java/handbook/java/
  <тема>/TasksTest.java ← тесты проверяют твои реализации из Tasks.java
```

| № | Тема | Пакет | О чём |
|---|------|-------|-------|
| 1 | Язык, типы, строки | `handbook.java.corelang` | примитивы/обёртки, кэш `Integer`, переполнение, String pool/immutability/`StringBuilder`, `==` vs `equals`, `char`-арифметика, передача по значению |
| 2 | ООП и структура классов | `handbook.java.oop` | интерфейсы + `default`/`static`, абстрактные классы, наследование vs композиция, порядок инициализации, `enum` с поведением, вложенные/внутренние/анонимные классы (+ утечки в Android) |
| 3 | Дженерики | `handbook.java.generics` | тип-параметры, границы, wildcards и **вариантность** (ко-/контра-/инвариантность, PECS), стирание типов и его обходы |
| 4 | Коллекции + equals/hashCode | `handbook.java.collections` | `List`/`Set`/`Map`, устройство `HashMap`, контракт `equals`/`hashCode`, `Comparator`/`Comparable`, `TreeMap`, `LinkedHashMap`→LRU, fail-fast |
| 5 | Исключения и ресурсы | `handbook.java.exceptions` | checked/unchecked, иерархия `Throwable`, try-with-resources + suppressed, `cause`/chaining, кастомные исключения |
| 6 | Функциональщина Java 8+ | `handbook.java.functional` | функциональные интерфейсы, лямбды, method references, Stream API, коллекторы, `Optional` |
| 7 | Потоки и модель памяти (JMM) | `handbook.java.concurrency` | `Thread`/`Runnable`, `synchronized`, `volatile`, JMM/happens-before, atomic/CAS, `wait`/`notify`, дедлоки, `ThreadLocal` |
| 8 | java.util.concurrent | `handbook.java.concurrenthigh` | `ExecutorService`, `Future`/`CompletableFuture`, `ConcurrentHashMap`, `CopyOnWriteArrayList`, `BlockingQueue`, `CountDownLatch`/`Semaphore`/`CyclicBarrier`, `ReentrantLock`/`ReadWriteLock` |
| 9 | Память, GC и ссылки | `handbook.java.memory` | достижимость и GC, `strong`/`soft`/`weak`/`phantom`, `WeakHashMap`/`ReferenceQueue`, утечки в Android (`this$0`, статик, `Handler`) и лечение |
| 10 | Interop Java ↔ Kotlin | `handbook.java.interop` | свойства↔геттеры, `object`/`companion`/`@JvmStatic`, `@JvmField`, top-level/`@JvmName`, `@JvmOverloads`, `data class`, SAM/`fun interface`, nullability/platform types, `@Throws`, function-типы |

Проверить тему Java:
```bash
./gradlew test --tests "handbook.java.corelang.*"
```

## 📱 Многопоточность в Android

Раздел про **многопоточность на сеньор-уровне именно в Android** — то, что даёт не JVM, а сам
фреймворк, и что спрашивают на собеседованиях. Примитивы JMM/локов/пулов не дублируем: они уже
разобраны в модуле [Java для Android](#-java-для-android) (темы 7–8), сюда ссылаемся. Устройство —
как у корутин: **10 тем**, в каждой `THEORY.md` + **~20 задач** трёх уровней (Л1–Л8 / С9–С15 /
СЛ16–СЛ20) + тесты. Задачи детерминированно моделируют механику Android-рантайма на чистой JVM
(без эмулятора/Robolectric).

> Модуль **в работе**: готовы темы 1–8, остальные добавляются по порядку (см. статус в таблице).

```
src/main/kotlin/handbook/android/
  <тема>/
    THEORY.md          ← теория (читать первым)
    Model.kt           ← учебная модель Android-примитивов (провайдится, не TODO)
    Tasks.kt           ← 20 ЗАДАЧ: функции с KDoc и телом TODO()
    solutions/
      Solutions.kt     ← эталонные решения
src/test/kotlin/handbook/android/
  <тема>/<Тема>Test.kt ← тесты проверяют твои реализации из Tasks.kt
```

| № | Тема | Пакет | О чём (senior) | Статус |
|---|------|-------|----------------|:---:|
| 1 | Главный поток и цикл событий | `handbook.android.looper` | `Looper`/`Handler`/`MessageQueue`, `when`-очередь, `postDelayed`, `removeCallbacks`, sync-барьер, `IdleHandler`, thread confinement, `quitSafely`, head-of-line blocking → jank/ANR | ✅ |
| 2 | Фоновые Handler-потоки | `handbook.android.handlerthread` | `HandlerThread` = поток + `Looper`, сериализация и thread confinement (без локов), `quit`/`quitSafely`, round-trip на главный поток, общий `Looper` для многих `Handler`, backpressure, шардирование, конвейеры, пинг-понг, отмена работы после смерти владельца | ✅ |
| 3 | Пулы потоков в Android | `handbook.android.pools` | `ThreadPoolExecutor` изнутри: связка core/max/очередь (подвох безграничной очереди), sizing CPU- vs IO-bound (формула Гётца), `ThreadFactory` (имена + приоритет фона), rejection-политики (Abort/CallerRuns/Discard), `shutdown` vs `shutdownNow`, прогрев, изоляция пулов, потолок параллелизма | ✅ |
| 4 | Приоритеты и планирование | `handbook.android.priority` | nice vs `Thread` priority, `Process.THREAD_PRIORITY_*`, веса CFS и доля CPU, планирование через vruntime, background-cgroup, строгий приоритет и starvation, инверсия приоритетов + наследование (в т.ч. транзитивное), RT-классы, джанк из-за приоритета фона | ✅ |
| 5 | Producer-consumer и backpressure | `handbook.android.queues` | `BlockingQueue` (put/take/offer/poll/drainTo, poison-pill), ограниченная очередь как backpressure, fan-in/fan-out, throttle/debounce/sample, конфляция (latest-wins), drop-oldest/latest, батчинг по кол-ву/времени, credit-based (request-N), композиция операторов | ✅ |
| 6 | ANR, кадры и jank | `handbook.android.anr` | бюджет кадра / refresh rate (16/11/8 мс), vsync и фазы `Choreographer`, dropped frames и каскад презентаций, метрики jank (janky %, перцентили, worst hitch, streak), пороги ANR, задержка ввода от бэклога главного потока, `StrictMode` (детект диск/сеть, penaltyDeath) | ✅ |
| 7 | Binder и IPC-треды | `handbook.android.binder` | пул Binder-тредов (~16) и его исчерпание, входящие транзакции/AIDL-колбэки на Binder-треде (не главном) → post на UI, sync (блокирует, риск ANR) vs `oneway` (async, сериализация к одному биндеру, без return), реентрантность и thread migration, реентрантный дедлок с локом, наследование приоритета через IPC (`min(nice)`), буфер ~1 МБ / `TransactionTooLargeException` / async-половина, `DeadObjectException`/`linkToDeath`, распределённый дедлок (цикл sync-вызовов) | ✅ |
| 8 | Фоновое выполнение и гарантии | `handbook.android.background` | почему голый `Thread` не даёт гарантий, что переживает reboot (WorkManager/JobScheduler), Doze и App Standby buckets (deferral), `Worker.doWork()` на фоновом потоке, constraints (сеть/зарядка/idle) и их ожидание во времени, backoff `LINEAR`/`EXPONENTIAL` с клампом и суммарная задержка, `Result.retry()`/ретраи до успеха, уникальная работа (`REPLACE`/`KEEP`/`APPEND`), цепочки и каскадная отмена потомков, foreground service (+нотификация), expedited-квоты, лимит JobScheduler, коалесинг будильников | ✅ |
| 9 | Потоки и жизненный цикл | `handbook.android.lifecycle` | публикация на главный поток, отмена при уничтожении, гонки config change, утечки `Handler` | ⬜ |
| 10 | Диагностика многопоточности | `handbook.android.diagnostics` | `StrictMode`, Perfetto/systrace, thread dumps, детект дедлоков, чтение ANR-трейсов | ⬜ |

Проверить тему Android:
```bash
./gradlew test --tests "handbook.android.looper.*"
```

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
