package handbook.java.concurrenthigh.solutions;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/** Эталонные решения темы 8. Подсмотри, если застрял с {@link handbook.java.concurrenthigh.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /**
     * Л1. Выполни {@link Callable} в пуле и верни результат. Подсказка: {@code ex.submit(task).get()}.
     * Не забудь {@code ex.shutdown()}.
     */
    public static int runOnExecutor(Callable<Integer> task) throws Exception {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> future = ex.submit(task);
            return future.get();
        } finally {
            ex.shutdown();
        }
    }

    /**
     * Л2. Возведи каждый элемент в квадрат ПАРАЛЛЕЛЬНО: отправь задачи в пул, собери {@link Future}
     * и забери результаты по порядку. Порядок результатов = порядок входа.
     */
    public static List<Integer> squareAll(List<Integer> nums) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int n : nums) {
                int v = n;
                futures.add(ex.submit(() -> v * v));
            }
            List<Integer> result = new ArrayList<>();
            for (Future<Integer> f : futures) result.add(f.get());
            return result;
        } finally {
            ex.shutdown();
        }
    }

    /** Л3. Выполни все {@link Callable} через {@code invokeAll} и верни сумму результатов. */
    public static int invokeAllSum(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(4);
        try {
            int sum = 0;
            for (Future<Integer> f : ex.invokeAll(tasks)) sum += f.get();
            return sum;
        } finally {
            ex.shutdown();
        }
    }

    /**
     * Л4. Отправь {@code tasks} задач в пул на {@code threads} потоков, каждая увеличивает счётчик.
     * Дождись завершения пула ({@code shutdown} + {@code awaitTermination}) и верни итог.
     */
    public static int poolTaskCount(int threads, int tasks) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < tasks; i++) {
            ex.submit(() -> {
                counter.incrementAndGet();
            });
        }
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);
        return counter.get();
    }

    /**
     * Л5. Посчитай частоту слов ПАРАЛЛЕЛЬНО через {@link ConcurrentHashMap#merge}: каждое слово
     * обрабатывается своей задачей. {@code merge} атомарен — гонок нет.
     */
    public static Map<String, Integer> countWords(List<String> words) throws Exception {
        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        ExecutorService ex = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (String w : words) {
                futures.add(ex.submit(() -> {
                    counts.merge(w, 1, Integer::sum);
                }));
            }
            for (Future<?> f : futures) f.get();
            return counts;
        } finally {
            ex.shutdown();
        }
    }

    /**
     * Л6. {@link CountDownLatch}: {@code n} воркеров делают работу и зовут {@code countDown()}; главный
     * поток ждёт {@code await()}, пока счётчик не дойдёт до нуля. Верни число выполнивших (== {@code n}).
     */
    public static int latchWaitsForWorkers(int n) throws Exception {
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger done = new AtomicInteger();
        ExecutorService ex = Executors.newFixedThreadPool(n);
        for (int i = 0; i < n; i++) {
            ex.submit(() -> {
                done.incrementAndGet();
                latch.countDown();
            });
        }
        latch.await();
        ex.shutdown();
        return done.get();
    }

    /**
     * Л7. Передай элементы через {@link BlockingQueue} (producer кладёт {@code put}, consumer забирает
     * {@code take}) и верни то, что забрал consumer. Очередь сама блокирует при переполнении/пустоте.
     */
    public static List<Integer> queueTransfer(List<Integer> items) throws Exception {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(4);
        List<Integer> consumed = new ArrayList<>();
        Thread producer = new Thread(() -> {
            try {
                for (int x : items) queue.put(x);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < items.size(); i++) consumed.add(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        return consumed;
    }

    /**
     * Л8. {@link CopyOnWriteArrayList}: итератор работает по СНИМКУ, поэтому изменение списка во время
     * обхода НЕ бросает {@link ConcurrentModificationException}. Пройди по списку, добавляя элементы,
     * и верни {@code true}, если исключения не было.
     */
    public static boolean cowNoConcurrentModification() {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        try {
            for (Integer x : list) {
                list.add(x);   // меняем во время обхода — снимок итератора не затрагивается
            }
            return true;
        } catch (ConcurrentModificationException e) {
            return false;
        }
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /** С9. {@link CompletableFuture}: асинхронно верни {@code x}, затем удвой. Подсказка: {@code supplyAsync(...).thenApply(...)}. */
    public static int asyncDouble(int x) throws Exception {
        return CompletableFuture.supplyAsync(() -> x).thenApply(v -> v * 2).get();
    }

    /** С10. Скомбинируй ДВА асинхронных значения в сумму. Подсказка: {@code fa.thenCombine(fb, (a,b) -> a+b)}. */
    public static int combineAsync(int a, int b) throws Exception {
        CompletableFuture<Integer> fa = CompletableFuture.supplyAsync(() -> a);
        CompletableFuture<Integer> fb = CompletableFuture.supplyAsync(() -> b);
        return fa.thenCombine(fb, (x, y) -> x + y).get();
    }

    /**
     * С11. Потокобезопасный мемоизирующий кэш: {@code get} вычисляет значение через {@code compute}
     * не более ОДНОГО раза на ключ, даже под конкуренцией. Подсказка: {@link ConcurrentHashMap#computeIfAbsent}
     * (атомарен на ключ). Реализуй {@code get}.
     */
    public static final class Memoizer<K, V> {
        private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
        private final Function<K, V> compute;
        public Memoizer(Function<K, V> compute) { this.compute = compute; }
        public V get(K key) {
            return cache.computeIfAbsent(key, compute);
        }
    }

    /**
     * С12. {@link Semaphore} ограничивает число ОДНОВРЕМЕННО работающих задач до {@code permits}. Запусти
     * {@code tasks} задач, замеряя максимум одновременно активных. Верни этот максимум (должен быть {@code <= permits}).
     */
    public static int maxConcurrent(int permits, int tasks) throws Exception {
        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger max = new AtomicInteger(0);
        ExecutorService ex = Executors.newFixedThreadPool(tasks);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                futures.add(ex.submit(() -> {
                    semaphore.acquire();
                    try {
                        int now = active.incrementAndGet();
                        max.updateAndGet(m -> Math.max(m, now));
                        Thread.sleep(20);
                        active.decrementAndGet();
                    } finally {
                        semaphore.release();
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) f.get();
            return max.get();
        } finally {
            ex.shutdown();
        }
    }

    /**
     * С13. {@link Future#get(long, TimeUnit)} с таймаутом: запусти долгую задачу и верни {@code true},
     * если поймал {@link TimeoutException} (после чего отмени задачу). Так ставят дедлайн на операцию.
     */
    public static boolean futureTimesOut() throws Exception {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<Integer> future = ex.submit(() -> {
            Thread.sleep(10_000);
            return 1;
        });
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            return false;
        } catch (TimeoutException e) {
            future.cancel(true);
            return true;
        } finally {
            ex.shutdownNow();
        }
    }

    /**
     * С14. Параллельная сумma массива через пул: каждая часть считается своей {@link Callable}, возвращающей
     * частичную сумму; главный складывает результаты {@link Future}. Верни сумму всего массива.
     */
    public static long parallelSum(int[] data, int threads) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            int chunk = (data.length + threads - 1) / threads;
            for (int i = 0; i < threads; i++) {
                int start = i * chunk;
                int end = Math.min(start + chunk, data.length);
                if (start >= end) break;
                futures.add(ex.submit(() -> {
                    long sum = 0;
                    for (int k = start; k < end; k++) sum += data[k];
                    return sum;
                }));
            }
            long total = 0;
            for (Future<Long> f : futures) total += f.get();
            return total;
        } finally {
            ex.shutdown();
        }
    }

    /**
     * С15. {@link ReentrantLock} — явная замена {@code synchronized}: {@code lock()} … {@code unlock()} в
     * {@code finally}. Защити общий счётчик под конкуренцией. Ожидается {@code threads*perThread}.
     */
    public static int lockedCount(int threads, int perThread) throws Exception {
        ReentrantLock lock = new ReentrantLock();
        int[] count = {0};
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    lock.lock();
                    try {
                        count[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        return count[0];
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Producer-consumer с «отравленной пилюлей» (poison pill): продюсер кладёт все элементы, затем по
     * одному маркеру-стопу на каждого консьюмера; консьюмеры суммируют, пока не встретят маркер. Верни
     * суммарно потреблённое. Подсказка: {@link LinkedBlockingQueue} + {@link CountDownLatch}.
     */
    public static long poisonPillPipeline(List<Integer> items) throws Exception {
        int consumers = 3;
        Integer poison = Integer.MIN_VALUE;
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        AtomicLong sum = new AtomicLong(0);
        CountDownLatch done = new CountDownLatch(consumers);
        ExecutorService ex = Executors.newFixedThreadPool(consumers + 1);
        try {
            ex.submit(() -> {
                for (int x : items) queue.put(x);
                for (int i = 0; i < consumers; i++) queue.put(poison);
                return null;
            });
            for (int i = 0; i < consumers; i++) {
                ex.submit(() -> {
                    while (true) {
                        int v = queue.take();
                        if (v == poison) break;
                        sum.addAndGet(v);
                    }
                    done.countDown();
                    return null;
                });
            }
            done.await();
            return sum.get();
        } finally {
            ex.shutdown();
        }
    }

    /**
     * СЛ17. Запусти на каждый элемент асинхронное возведение в квадрат, дождись ВСЕХ через
     * {@link CompletableFuture#allOf} и верни сумму квадратов.
     */
    public static int allOfSumOfSquares(List<Integer> nums) throws Exception {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int n : nums) {
            int v = n;
            futures.add(CompletableFuture.supplyAsync(() -> v * v));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        int sum = 0;
        for (CompletableFuture<Integer> f : futures) sum += f.join();
        return sum;
    }

    /**
     * СЛ18. {@link ReadWriteLock}: много читателей могут работать параллельно, писатель — эксклюзивно.
     * {@code increment} под write-lock, {@code get} под read-lock. Реализуй оба метода.
     */
    public static final class ReadWriteCounter {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private long value = 0;
        public void increment() {
            lock.writeLock().lock();
            try {
                value++;
            } finally {
                lock.writeLock().unlock();
            }
        }
        public long get() {
            lock.readLock().lock();
            try {
                return value;
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * СЛ19. {@link CyclicBarrier}: {@code n} потоков выполняют фазу 1, затем ждут на барьере, и только когда
     * ВСЕ пришли — начинают фазу 2. Верни число нарушений (0): в фазе 2 у всех фаза 1 уже завершена.
     */
    public static int barrierSyncsPhases(int n) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(n);
        AtomicInteger phase1Done = new AtomicInteger(0);
        AtomicInteger violations = new AtomicInteger(0);
        ExecutorService ex = Executors.newFixedThreadPool(n);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(ex.submit(() -> {
                    phase1Done.incrementAndGet();
                    barrier.await();                       // ждём всех
                    if (phase1Done.get() != n) violations.incrementAndGet();
                    return null;
                }));
            }
            for (Future<?> f : futures) f.get();
            return violations.get();
        } finally {
            ex.shutdown();
        }
    }

    /**
     * СЛ20. Композиция и обработка ошибок {@link CompletableFuture}: посчитай {@code 10}, затем асинхронно
     * удвой ({@code thenCompose}); если по пути ошибка — восстановись значением {@code -1}
     * ({@code exceptionally}). При {@code fail == true} вычисление бросает исключение.
     */
    public static int asyncWithFallback(boolean fail) throws Exception {
        return CompletableFuture
                .supplyAsync(() -> {
                    if (fail) throw new RuntimeException("boom");
                    return 10;
                })
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> v * 2))
                .exceptionally(ex -> -1)
                .get();
    }
}
