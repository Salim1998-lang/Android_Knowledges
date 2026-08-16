package handbook.java.concurrency.solutions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Эталонные решения темы 7. Подсмотри, если застрял с {@link handbook.java.concurrency.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Выполни {@code r} в НОВОМ потоке и дождись завершения. Подсказка: {@code new Thread(r).start()} + {@code join()}. */
    public static void runAndJoin(Runnable r) throws InterruptedException {
        Thread t = new Thread(r);
        t.start();
        t.join();
    }

    /** Л2. Запусти каждый Runnable в своём потоке и дождись ВСЕХ. */
    public static void runAll(List<Runnable> tasks) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (Runnable r : tasks) {
            Thread t = new Thread(r);
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) t.join();
    }

    /** Л3. Посчитай значение в отдельном потоке и верни его (результат заберём после join). */
    public static int computeOnThread(Supplier<Integer> supplier) throws InterruptedException {
        int[] holder = new int[1];
        Thread t = new Thread(() -> holder[0] = supplier.get());
        t.start();
        t.join();
        return holder[0];
    }

    /**
     * Л4. {@code threads} потоков, каждый {@code perThread} раз увеличивает общий счётчик. Верни итог.
     * Используй {@link AtomicInteger#incrementAndGet()} — атомарно, без гонок. Ожидается {@code threads*perThread}.
     */
    public static int atomicIncrements(int threads, int perThread) throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < perThread; j++) counter.incrementAndGet();
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        return counter.get();
    }

    /**
     * Л5. То же, но защити общий {@code int} блоком {@code synchronized} на общем объекте-замке
     * (интринсик-лок). Ожидается {@code threads*perThread}.
     */
    public static int synchronizedCount(int threads, int perThread) throws InterruptedException {
        int[] count = {0};
        Object lock = new Object();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    synchronized (lock) {
                        count[0]++;
                    }
                }
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        return count[0];
    }

    /**
     * Л6. Видимость через {@code volatile}: запусти поток, крутящийся в {@code while (flag.running)},
     * затем сбрось флаг и дождись остановки. Верни {@code true}, если поток завершился. Поле
     * {@link VolatileFlag#running} объявлено {@code volatile} — иначе поток мог бы НЕ увидеть смену
     * значения и зациклиться навсегда.
     */
    public static boolean stopsWhenFlagCleared() throws InterruptedException {
        VolatileFlag flag = new VolatileFlag();
        Thread worker = new Thread(() -> {
            while (flag.running) {
                // крутимся, пока не увидим сброс флага
            }
        });
        worker.start();
        flag.running = false;
        worker.join(2000);
        return !worker.isAlive();
    }

    /**
     * Л7. Изоляция {@link ThreadLocal}: {@code threads} потоков кладут в общий ThreadLocal СВОЁ число и
     * потом читают его. Верни {@code true}, если каждый прочитал именно своё (потоки не мешают друг другу).
     */
    public static boolean threadLocalIsolation(int threads) throws InterruptedException {
        ThreadLocal<Integer> local = new ThreadLocal<>();
        AtomicBoolean allCorrect = new AtomicBoolean(true);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int value = i;
            Thread t = new Thread(() -> {
                local.set(value);
                Thread.yield();
                if (local.get() != value) allCorrect.set(false);
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        return allCorrect.get();
    }

    /**
     * Л8. Уникальные id: {@code threads} потоков берут номера через {@link AtomicInteger#getAndIncrement()}.
     * Верни число РАЗЛИЧНЫХ выданных id — атомарность гарантирует отсутствие дублей ({@code threads*perThread}).
     */
    public static int uniqueIds(int threads, int perThread) throws InterruptedException {
        AtomicInteger seq = new AtomicInteger();
        List<List<Integer>> perThreadIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) perThreadIds.add(null);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int slot = i;
            Thread t = new Thread(() -> {
                List<Integer> mine = new ArrayList<>();
                for (int j = 0; j < perThread; j++) mine.add(seq.getAndIncrement());
                perThreadIds.set(slot, mine);
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        Set<Integer> all = new HashSet<>();
        for (List<Integer> ids : perThreadIds) all.addAll(ids);
        return all.size();
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Happens-before через {@code join}: поток пишет в ОБЫЧНОЕ (не volatile) поле, а главный после
     * {@code join()} гарантированно видит запись. Верни прочитанное значение (ожидается 42).
     */
    public static int visibleAfterJoin() throws InterruptedException {
        int[] data = new int[1];
        Thread t = new Thread(() -> data[0] = 42);
        t.start();
        t.join();               // join устанавливает happens-before: запись видна здесь
        return data[0];
    }

    /**
     * С10. Атомарный максимум через CAS-цикл: обнови {@code current} до {@code candidate}, только если тот
     * больше. Подсказка: цикл {@code while(true)} с {@link AtomicInteger#compareAndSet(int,int)} —
     * перечитываем текущее значение и пробуем заменить, повторяя при неудаче.
     */
    public static void casMax(AtomicInteger current, int candidate) {
        while (true) {
            int cur = current.get();
            if (candidate <= cur) return;
            if (current.compareAndSet(cur, candidate)) return;
        }
    }

    /**
     * С11. Идемпотентная одноразовая защёлка на {@code wait}/{@code notifyAll}: {@code await()} блокирует,
     * пока не позвали {@code release()}. Реализуй оба метода. {@code await} должен ждать в ЦИКЛЕ по условию
     * (защита от «ложных пробуждений»).
     */
    public static final class OneShotLatch {
        private boolean released = false;
        public synchronized void await() throws InterruptedException {
            while (!released) {
                wait();
            }
        }
        public synchronized void release() {
            released = true;
            notifyAll();
        }
    }

    /**
     * С12. Ленивая инициализация с двойной проверкой (double-checked locking): {@code get()} вычисляет
     * значение РОВНО один раз даже под конкуренцией. Поле {@code value} — {@code volatile} (обязательно
     * для корректности DCL). Реализуй {@code get()}.
     */
    public static final class Lazy<T> {
        private final Supplier<T> supplier;
        private volatile T value;
        public Lazy(Supplier<T> supplier) { this.supplier = supplier; }
        public T get() {
            T result = value;                 // 1-е чтение volatile (быстрый путь без лока)
            if (result == null) {
                synchronized (this) {
                    result = value;           // 2-е чтение под локом
                    if (result == null) {
                        result = supplier.get();
                        value = result;
                    }
                }
            }
            return result;
        }
    }

    /**
     * С13. Неблокирующее накопление в {@link AtomicReference}: {@code threads} потоков по {@code perThread}
     * раз добавляют элемент в неизменяемый список через {@link AtomicReference#updateAndGet} (создаём КОПИЮ,
     * CAS повторится при конфликте). Верни итоговый размер ({@code threads*perThread}).
     */
    public static int concurrentAppend(int threads, int perThread) throws InterruptedException {
        AtomicReference<List<Integer>> ref = new AtomicReference<>(List.of());
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    ref.updateAndGet(old -> {
                        List<Integer> copy = new ArrayList<>(old);
                        copy.add(1);
                        return copy;
                    });
                }
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        return ref.get().size();
    }

    /**
     * С14. Параллельная сумма массива: раздели {@code data} на {@code threads} частей, каждый поток суммирует
     * свою часть и добавляет к общему итогу через {@link AtomicLong#addAndGet}. Верни сумму всего массива.
     */
    public static long parallelArraySum(int[] data, int threads) throws InterruptedException {
        AtomicLong total = new AtomicLong();
        List<Thread> ts = new ArrayList<>();
        int chunk = (data.length + threads - 1) / threads;
        for (int i = 0; i < threads; i++) {
            int start = i * chunk;
            int end = Math.min(start + chunk, data.length);
            if (start >= end) break;
            Thread t = new Thread(() -> {
                long sum = 0;
                for (int k = start; k < end; k++) sum += data[k];
                total.addAndGet(sum);
            });
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) t.join();
        return total.get();
    }

    /**
     * С15. Прерывание потока: запусти поток в долгом {@code Thread.sleep}, прерви его {@code interrupt()} и
     * верни {@code true}, если поток поймал {@link InterruptedException}. Так в Java/Android кооперативно
     * ОТМЕНЯЮТ работу потока.
     */
    public static boolean interruptStops() throws InterruptedException {
        AtomicBoolean observed = new AtomicBoolean(false);
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                observed.set(true);           // прерывание получено
            }
        });
        worker.start();
        worker.interrupt();
        worker.join();
        return observed.get();
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Ограниченный буфер (producer-consumer) на {@code wait}/{@code notifyAll}: {@code put} ждёт, пока
     * есть место; {@code take} ждёт, пока есть элемент; оба будят другую сторону. Оба ожидания — в ЦИКЛЕ
     * по условию. Реализуй {@code put} и {@code take}.
     */
    public static final class BoundedBuffer<E> {
        private final Queue<E> queue = new LinkedList<>();
        private final int capacity;
        public BoundedBuffer(int capacity) { this.capacity = capacity; }
        public synchronized void put(E item) throws InterruptedException {
            while (queue.size() == capacity) {
                wait();
            }
            queue.add(item);
            notifyAll();
        }
        public synchronized E take() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            E item = queue.remove();
            notifyAll();
            return item;
        }
    }

    /**
     * СЛ17. Lock-free стек Трайбера на {@link AtomicReference}: {@code push}/{@code pop} через CAS-цикл, без
     * блокировок. Реализуй оба (при пустом стеке {@code pop} возвращает {@code null}).
     */
    public static final class TreiberStack<E> {
        private final AtomicReference<Node<E>> top = new AtomicReference<>();
        public void push(E value) {
            Node<E> oldHead;
            Node<E> newHead;
            do {
                oldHead = top.get();
                newHead = new Node<>(value, oldHead);
            } while (!top.compareAndSet(oldHead, newHead));
        }
        public E pop() {
            Node<E> oldHead;
            Node<E> newHead;
            do {
                oldHead = top.get();
                if (oldHead == null) return null;
                newHead = oldHead.next;
            } while (!top.compareAndSet(oldHead, newHead));
            return oldHead.value;
        }
        private static final class Node<E> {
            final E value;
            final Node<E> next;
            Node(E value, Node<E> next) { this.value = value; this.next = next; }
        }
    }

    /**
     * СЛ18. Публикация через {@code volatile}: писатель заполняет ОБЫЧНОЕ поле {@code data}, затем ставит
     * {@code volatile ready = true}; читатель крутится до {@code ready} и читает {@code data}. Volatile-запись
     * создаёт happens-before, поэтому читатель гарантированно видит {@code data}. Верни прочитанное (ожидается 42).
     */
    public static int publishThenRead() throws InterruptedException {
        Publisher p = new Publisher();
        int[] seen = new int[1];
        Thread reader = new Thread(() -> {
            while (!p.ready) {
                // ждём публикации
            }
            seen[0] = p.data;
        });
        Thread writer = new Thread(() -> {
            p.data = 42;        // обычная запись ДО...
            p.ready = true;     // ...volatile-записи — вместе дают безопасную публикацию
        });
        reader.start();
        writer.start();
        writer.join();
        reader.join();
        return seen[0];
    }

    /**
     * СЛ19. Перевод без дедлока: захватывай мониторы двух счетов в ЕДИНОМ порядке (по {@code id}), иначе два
     * встречных перевода могут заблокировать друг друга. Реализуй, сохраняя суммарный баланс.
     */
    public static void orderedTransfer(Account from, Account to, long amount) {
        Account first = from.id <= to.id ? from : to;
        Account second = from.id <= to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    /**
     * СЛ20. Спин-лок из CAS: реализуй взаимное исключение поверх {@link AtomicBoolean} — {@code lock()}
     * крутит {@code compareAndSet(false, true)}, пока не захватит; {@code unlock()} освобождает. Такой лок
     * защищает критическую секцию без {@code synchronized}.
     */
    public static final class SpinLock {
        private final AtomicBoolean locked = new AtomicBoolean(false);
        public void lock() {
            while (!locked.compareAndSet(false, true)) {
                // крутимся, пока лок занят
            }
        }
        public void unlock() {
            locked.set(false);
        }
    }

    // ═══════════════════════════ Инфраструктура (дана целиком) ═══════════════════════════

    /** Флаг с {@code volatile}-полем — гарантирует видимость смены значения другим потокам. */
    public static final class VolatileFlag {
        public volatile boolean running = true;
    }

    /** Пара «данные + флаг готовности» для демонстрации безопасной публикации через volatile. */
    public static final class Publisher {
        public int data = 0;              // обычное поле
        public volatile boolean ready = false;   // volatile-флаг публикации
    }

    /** Счёт с идентификатором (для упорядоченного захвата мониторов) и балансом. */
    public static final class Account {
        public final int id;
        public long balance;
        public Account(int id, long balance) { this.id = id; this.balance = balance; }
    }
}
