# Тема 8. `java.util.concurrent`

> Читаешь теорию → решаешь 20 задач в `Tasks.java` → гоняешь тесты:
> `./gradlew test --tests "handbook.java.concurrenthigh.*"`. Застрял — сверься с
> [`solutions/Solutions.java`](solutions/Solutions.java).

Тема 7 дала «сырые» примитивы (`Thread`, `synchronized`, `volatile`, CAS). `java.util.concurrent`
(автор — Дуг Ли) даёт готовые высокоуровневые кирпичи: пулы потоков, `Future`/`CompletableFuture`,
потокобезопасные коллекции, очереди, синхронизаторы и явные локи. В реальном коде используют именно их,
а не ручные потоки.

---

## 1. Пулы потоков: `ExecutorService`

Создавать `new Thread()` на каждую задачу дорого и неуправляемо. **Пул** переиспользует ограниченное
число потоков:

```java
ExecutorService ex = Executors.newFixedThreadPool(4);
ex.submit(() -> doWork());           // Runnable — «выстрелил и забыл»
Future<Integer> f = ex.submit(() -> compute());  // Callable — с результатом
int result = f.get();                // блокирует, пока не готово
ex.shutdown();                       // ОБЯЗАТЕЛЬНО: иначе JVM/тест не завершатся
ex.awaitTermination(5, TimeUnit.SECONDS);
```

- `Executors.newFixedThreadPool(n)`, `newCachedThreadPool()`, `newSingleThreadExecutor()`,
  `newVirtualThreadPerTaskExecutor()` (Java 21).
- `submit(Callable)` → `Future`; `invokeAll(tasks)` → список `Future` (ждёт все).
- Всегда закрывай пул: `shutdown()` (мягко) или `shutdownNow()` (прерывая).

> Под капотом — `ThreadPoolExecutor` (размер пула, очередь задач, политика отказа). Понимать его
> параметры полезно, но в 90% случаев хватает фабрик `Executors`.

## 2. `Future` и `Callable`

`Callable<T>` — как `Runnable`, но **возвращает** значение и **может бросить** checked-исключение.
`Future<T>` — «обещание результата»:

```java
Future<Integer> f = ex.submit(() -> 6 * 7);
f.get();                       // 42 (блокирует)
f.get(100, TimeUnit.MILLISECONDS);  // с таймаутом → TimeoutException
f.cancel(true);                // отменить (прервать)
f.isDone(); f.isCancelled();
```

`f.get()` пробрасывает ошибку задачи как `ExecutionException` (причина внутри — `getCause()`).

## 3. `CompletableFuture` — асинхронные конвейеры

`Future` умеет только блокирующий `get()`. `CompletableFuture` строит **неблокирующие цепочки**:

```java
CompletableFuture.supplyAsync(() -> load())      // асинхронно
    .thenApply(data -> transform(data))          // преобразовать результат
    .thenCompose(x -> anotherAsync(x))           // «flatMap»: вложенный async
    .thenCombine(otherFuture, (a, b) -> a + b)   // объединить два
    .exceptionally(ex -> fallback)               // восстановиться после ошибки
    .thenAccept(result -> use(result));          // потребить

CompletableFuture.allOf(f1, f2, f3).join();      // дождаться всех
```

Это Java-аналог «промисов»/`async-await`; в Android с ним пересекаются RxJava и корутины.

## 4. Потокобезопасные коллекции

Обычные `ArrayList`/`HashMap` **не** потокобезопасны. Вместо ручной синхронизации — готовые:

- **`ConcurrentHashMap`** — конкурентная мапа. Атомарные операции без внешних локов:

  ```java
  map.merge(key, 1, Integer::sum);              // атомарный счётчик
  map.computeIfAbsent(key, k -> expensive(k));  // вычислить РОВНО раз на ключ (потокобезопасно)
  map.compute(key, (k, v) -> ...);
  ```

  Идеально для конкурентного подсчёта и кэша-мемоизации.

- **`CopyOnWriteArrayList`** — при записи копирует массив целиком; чтение/итерация — по неизменяемому
  **снимку**, без блокировок и без `ConcurrentModificationException`. Хорош, когда читают ЧАСТО, а
  пишут РЕДКО (например, список слушателей).

## 5. Очереди: `BlockingQueue`

`BlockingQueue` — потокобезопасная очередь, где `put` **ждёт** место, а `take` **ждёт** элемент.
Готовый фундамент producer-consumer (не нужно вручную `wait`/`notify`):

```java
BlockingQueue<Task> q = new LinkedBlockingQueue<>();     // или ArrayBlockingQueue(capacity)
q.put(task);        // блокирует, если заполнена
Task t = q.take();  // блокирует, если пуста
q.poll(1, TimeUnit.SECONDS);   // с таймаутом
```

Паттерн **poison pill**: чтобы завершить консьюмеров, продюсер кладёт особый маркер-стоп (по одному на
каждого), увидев который консьюмер выходит.

## 6. Синхронизаторы

- **`CountDownLatch`** — «дождаться N событий»: одноразовый счётчик. `await()` ждёт, `countDown()`
  уменьшает. Пример: главный поток ждёт, пока N воркеров стартуют/завершатся.
- **`CyclicBarrier`** — «собраться всем в точке»: N потоков зовут `await()`, барьер отпускает их
  одновременно, когда пришли все. В отличие от латча — **переиспользуемый** (для фаз/раундов).
- **`Semaphore`** — счётчик разрешений: `acquire()`/`release()` ограничивают число одновременно
  работающих (например, не больше 3 запросов к ресурсу сразу).

## 7. Явные локи: `Lock` и `ReadWriteLock`

`java.util.concurrent.locks` — более гибкая альтернатива `synchronized`:

```java
ReentrantLock lock = new ReentrantLock();
lock.lock();
try { /* критическая секция */ } finally { lock.unlock(); }   // unlock ВСЕГДА в finally
```

Преимущества над `synchronized`: `tryLock()` (в т.ч. с таймаутом — против дедлоков), прерываемый
захват, честность (fairness).

**`ReadWriteLock`** — раздельные блокировки: много **читателей** параллельно ИЛИ один **писатель**
эксклюзивно. Выигрывает при «читают часто, пишут редко»:

```java
rwLock.readLock().lock();   // много читателей одновременно
rwLock.writeLock().lock();  // эксклюзивно
```

## 8. Атомики и сумматоры

Из темы 7 — `AtomicInteger`/`AtomicLong`/`AtomicReference` (CAS без блокировок). Для высококонкурентных
счётчиков есть `LongAdder`/`LongAccumulator` — они разносят инкременты по ячейкам и суммируют лениво,
что быстрее `AtomicLong` под сильной конкуренцией.

> **Контекст Android.** Прикладной async-код обычно на корутинах (`Dispatchers.IO` — это пул потоков
> под капотом) или RxJava. Но `ExecutorService`, `ConcurrentHashMap`, `BlockingQueue`, локи и
> синхронизаторы повсеместны в библиотеках, SDK и в самом рантайме — читать и применять их надо уметь.
> `WorkManager`/`HandlerThread` тоже про управление фоновыми потоками.

---

## Что дальше

Реши 20 задач в [`Tasks.java`](Tasks.java): Л1–Л8, С9–С15, СЛ16–СЛ20. Особое внимание —
`CompletableFuture` (С9–С10, СЛ17, СЛ20), `ConcurrentHashMap` (Л5, С11) и синхронизаторам (Л6, С12,
СЛ19). Прогон: `./gradlew test --tests "handbook.java.concurrenthigh.*"`.
