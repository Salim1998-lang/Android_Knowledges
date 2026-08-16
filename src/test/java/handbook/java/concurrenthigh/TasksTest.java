package handbook.java.concurrenthigh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Тесты темы 8 «java.util.concurrent». Детерминированные проверки корректного использования пулов,
 * Future/CompletableFuture, ConcurrentHashMap, BlockingQueue, латчей/семафоров/барьеров, локов.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_runOnExecutor() throws Exception {
        assertEquals(42, Tasks.runOnExecutor(() -> 6 * 7));
    }

    @Test void Л2_squareAll_parallel() throws Exception {
        assertEquals(List.of(1, 4, 9, 16), Tasks.squareAll(List.of(1, 2, 3, 4)));
    }

    @Test void Л3_invokeAllSum() throws Exception {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2, () -> 3);
        assertEquals(6, Tasks.invokeAllSum(tasks));
    }

    @Test void Л4_poolTaskCount() throws Exception {
        assertEquals(1_000, Tasks.poolTaskCount(4, 1_000));
    }

    @Test void Л5_countWords_concurrentHashMap() throws Exception {
        Map<String, Integer> counts = Tasks.countWords(List.of("a", "b", "a", "a", "b"));
        assertEquals(3, counts.get("a"));
        assertEquals(2, counts.get("b"));
    }

    @Test void Л6_countDownLatch() throws Exception {
        assertEquals(5, Tasks.latchWaitsForWorkers(5));
    }

    @Test void Л7_blockingQueue() throws Exception {
        assertEquals(List.of(1, 2, 3, 4, 5), Tasks.queueTransfer(List.of(1, 2, 3, 4, 5)));
    }

    @Test void Л8_copyOnWrite_noCME() {
        assertTrue(Tasks.cowNoConcurrentModification(), "CopyOnWriteArrayList не бросает CME при обходе");
    }

    // ── Средние ──

    @Test void С9_completableFuture_thenApply() throws Exception {
        assertEquals(20, Tasks.asyncDouble(10));
    }

    @Test void С10_thenCombine() throws Exception {
        assertEquals(7, Tasks.combineAsync(3, 4));
    }

    @Test void С11_memoizer_computesOncePerKey() throws Exception {
        AtomicInteger computations = new AtomicInteger(0);
        Tasks.Memoizer<Integer, Integer> memo = new Tasks.Memoizer<>(k -> {
            computations.incrementAndGet();
            return k * k;
        });
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Thread t = new Thread(() -> {
                for (int k = 0; k < 5; k++) memo.get(k);
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        assertEquals(25, memo.get(5));   // и новый ключ считается
        assertEquals(6, computations.get(), "каждый из 6 ключей вычислен ровно раз");
    }

    @Test void С12_semaphoreLimitsConcurrency() throws Exception {
        int max = Tasks.maxConcurrent(3, 12);
        assertTrue(max <= 3, "семафор ограничил одновременность: было " + max);
        assertTrue(max >= 1);
    }

    @Test void С13_futureTimeout() throws Exception {
        assertTrue(Tasks.futureTimesOut(), "долгая задача должна дать TimeoutException");
    }

    @Test void С14_parallelSum() throws Exception {
        int[] data = new int[10_000];
        long expected = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
            expected += i;
        }
        assertEquals(expected, Tasks.parallelSum(data, 8));
    }

    @Test void С15_reentrantLock() throws Exception {
        assertEquals(80_000, Tasks.lockedCount(8, 10_000));
    }

    // ── Сложные ──

    @Test void СЛ16_poisonPillPipeline() throws Exception {
        List<Integer> items = new ArrayList<>();
        long expected = 0;
        for (int i = 1; i <= 100; i++) {
            items.add(i);
            expected += i;
        }
        assertEquals(expected, Tasks.poisonPillPipeline(items));
    }

    @Test void СЛ17_allOfSumOfSquares() throws Exception {
        assertEquals(1 + 4 + 9 + 16, Tasks.allOfSumOfSquares(List.of(1, 2, 3, 4)));
    }

    @Test void СЛ18_readWriteLock() throws Exception {
        Tasks.ReadWriteCounter counter = new Tasks.ReadWriteCounter();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread writer = new Thread(() -> {
                for (int j = 0; j < 1_000; j++) counter.increment();
            });
            Thread reader = new Thread(() -> {
                for (int j = 0; j < 1_000; j++) counter.get();
            });
            writer.start();
            reader.start();
            ts.add(writer);
            ts.add(reader);
        }
        for (Thread t : ts) t.join();
        assertEquals(8_000, counter.get());
    }

    @Test void СЛ19_cyclicBarrier() throws Exception {
        assertEquals(0, Tasks.barrierSyncsPhases(6), "барьер: фаза 2 стартует только после общей фазы 1");
    }

    @Test void СЛ20_completableFuture_composeAndRecover() throws Exception {
        assertEquals(20, Tasks.asyncWithFallback(false), "10 → удвоить → 20");
        assertEquals(-1, Tasks.asyncWithFallback(true), "ошибка → exceptionally → -1");
    }
}
