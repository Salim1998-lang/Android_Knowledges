package handbook.java.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Тесты темы 7 «Потоки и модель памяти». Проверки ДЕТЕРМИНИРОВАНЫ: корректная синхронизация даёт
 * точный результат. @Timeout страхует от зависаний при неверной реализации wait/notify или спин-лока.
 */
@Timeout(value = 15, unit = TimeUnit.SECONDS)
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_runAndJoin() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        Tasks.runAndJoin(() -> flag.set(7));
        assertEquals(7, flag.get(), "после join результат потока виден");
    }

    @Test void Л2_runAll() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) tasks.add(counter::incrementAndGet);
        Tasks.runAll(tasks);
        assertEquals(10, counter.get());
    }

    @Test void Л3_computeOnThread() throws Exception {
        assertEquals(42, Tasks.computeOnThread(() -> 6 * 7));
    }

    @Test void Л4_atomicIncrements() throws Exception {
        assertEquals(80_000, Tasks.atomicIncrements(8, 10_000));
    }

    @Test void Л5_synchronizedCount() throws Exception {
        assertEquals(80_000, Tasks.synchronizedCount(8, 10_000));
    }

    @Test void Л6_volatileStop() throws Exception {
        assertTrue(Tasks.stopsWhenFlagCleared(), "поток должен увидеть сброс volatile-флага и остановиться");
    }

    @Test void Л7_threadLocalIsolation() throws Exception {
        assertTrue(Tasks.threadLocalIsolation(50));
    }

    @Test void Л8_uniqueIds() throws Exception {
        assertEquals(8_000, Tasks.uniqueIds(8, 1_000), "getAndIncrement не выдаёт дублей");
    }

    // ── Средние ──

    @Test void С9_visibleAfterJoin() throws Exception {
        assertEquals(42, Tasks.visibleAfterJoin());
    }

    @Test void С10_casMax() throws Exception {
        AtomicInteger m = new AtomicInteger(0);
        Tasks.casMax(m, 5);
        Tasks.casMax(m, 3);   // меньше — не меняет
        Tasks.casMax(m, 10);
        assertEquals(10, m.get());

        AtomicInteger shared = new AtomicInteger(0);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int base = i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < 1_000; j++) Tasks.casMax(shared, base * 1_000 + j);
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        assertEquals(7_999, shared.get(), "конкурентный максимум точен");
    }

    @Test void С11_oneShotLatch() throws Exception {
        Tasks.OneShotLatch latch = new Tasks.OneShotLatch();
        AtomicInteger passed = new AtomicInteger(0);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    passed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            t.start();
            ts.add(t);
        }
        latch.release();
        for (Thread t : ts) t.join();
        assertEquals(5, passed.get(), "все ждавшие проходят после release");
    }

    @Test void С12_lazyDoubleChecked() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        Object singleton = new Object();
        Tasks.Lazy<Object> lazy = new Tasks.Lazy<>(() -> {
            calls.incrementAndGet();
            return singleton;
        });
        List<Object> results = new ArrayList<>();
        Object[] slots = new Object[16];
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            int slot = i;
            Thread t = new Thread(() -> slots[slot] = lazy.get());
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        for (Object o : slots) results.add(o);
        assertEquals(1, calls.get(), "supplier вызван РОВНО один раз");
        assertTrue(results.stream().allMatch(o -> o == singleton), "все получили один и тот же объект");
    }

    @Test void С13_concurrentAppend() throws Exception {
        assertEquals(2_000, Tasks.concurrentAppend(4, 500));
    }

    @Test void С14_parallelArraySum() throws Exception {
        int[] data = new int[100_000];
        long expected = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = i % 100;
            expected += data[i];
        }
        assertEquals(expected, Tasks.parallelArraySum(data, 8));
    }

    @Test void С15_interruptStops() throws Exception {
        assertTrue(Tasks.interruptStops(), "поток должен поймать InterruptedException");
    }

    // ── Сложные ──

    @Test void СЛ16_boundedBuffer() throws Exception {
        Tasks.BoundedBuffer<Integer> buffer = new Tasks.BoundedBuffer<>(4);
        AtomicLong consumedSum = new AtomicLong(0);
        long producedSum = 0;
        for (int i = 1; i <= 100; i++) producedSum += i;

        Thread p1 = producer(buffer, 1, 50);
        Thread p2 = producer(buffer, 51, 100);
        Thread c1 = consumer(buffer, 50, consumedSum);
        Thread c2 = consumer(buffer, 50, consumedSum);
        p1.start(); p2.start(); c1.start(); c2.start();
        p1.join(); p2.join(); c1.join(); c2.join();

        assertEquals(producedSum, consumedSum.get(), "ни один элемент не потерян и не задвоен");
    }

    @Test void СЛ17_treiberStack() throws Exception {
        Tasks.TreiberStack<Integer> stack = new Tasks.TreiberStack<>();
        List<Thread> pushers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < 100; j++) stack.push(1);
            });
            t.start();
            pushers.add(t);
        }
        for (Thread t : pushers) t.join();

        int count = 0;
        while (stack.pop() != null) count++;
        assertEquals(400, count, "все 400 элементов на месте (CAS без потерь)");
    }

    @Test void СЛ18_publishThenRead() throws Exception {
        assertEquals(42, Tasks.publishThenRead());
    }

    @Test void СЛ19_orderedTransferKeepsTotal() throws Exception {
        int n = 5;
        List<Tasks.Account> accounts = new ArrayList<>();
        for (int i = 0; i < n; i++) accounts.add(new Tasks.Account(i, 100));
        long total = 500;

        AtomicInteger errors = new AtomicInteger(0);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread t = new Thread(() -> {
                Random rnd = new Random();
                for (int j = 0; j < 2_000; j++) {
                    Tasks.Account from = accounts.get(rnd.nextInt(n));
                    Tasks.Account to = accounts.get(rnd.nextInt(n));
                    Tasks.orderedTransfer(from, to, rnd.nextInt(10));
                }
            });
            // без этого исключение из потока «проглотится» и тест ложно позеленеет
            t.setUncaughtExceptionHandler((thread, ex) -> errors.incrementAndGet());
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();

        assertEquals(0, errors.get(), "переводы должны выполняться без исключений в потоках");
        long sum = 0;
        for (Tasks.Account a : accounts) sum += a.balance;
        assertEquals(total, sum, "суммарный баланс — инвариант, дедлока нет");
    }

    @Test void СЛ20_spinLock() throws Exception {
        Tasks.SpinLock lock = new Tasks.SpinLock();
        int[] counter = {0};
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < 1_000; j++) {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        assertEquals(8_000, counter[0], "CAS-спинлок обеспечил взаимное исключение");
    }

    // ── помощники для BoundedBuffer ──

    private static Thread producer(Tasks.BoundedBuffer<Integer> buffer, int from, int to) {
        return new Thread(() -> {
            try {
                for (int i = from; i <= to; i++) buffer.put(i);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static Thread consumer(Tasks.BoundedBuffer<Integer> buffer, int count, AtomicLong sink) {
        return new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) sink.addAndGet(buffer.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
