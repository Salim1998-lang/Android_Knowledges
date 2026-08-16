package handbook.java.concurrency;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Тема 7 «Потоки и модель памяти (JMM)» — 20 задач.
 * Реализуй помеченные {@code TODO} тела/методы. Инфраструктура ({@link VolatileFlag}, {@link Publisher},
 * {@link Account}) дана целиком.
 * Проверка: {@code ./gradlew test --tests "handbook.java.concurrency.*"}.
 * Эталон — в {@link handbook.java.concurrency.solutions.Solutions}.
 *
 * Тесты детерминированы: проверяют КОРРЕКТНЫЙ результат синхронизации (точную сумму, уникальность,
 * инвариант), а не сам факт гонки (её ненадёжно воспроизводить).
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Выполни {@code r} в НОВОМ потоке и дождись завершения. Подсказка: {@code new Thread(r).start()} + {@code join()}. */
    public static void runAndJoin(Runnable r) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л1 runAndJoin");
    }

    /** Л2. Запусти каждый Runnable в своём потоке и дождись ВСЕХ. */
    public static void runAll(List<Runnable> tasks) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л2 runAll");
    }

    /** Л3. Посчитай значение в отдельном потоке и верни его (результат заберём после join). */
    public static int computeOnThread(Supplier<Integer> supplier) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л3 computeOnThread");
    }

    /**
     * Л4. {@code threads} потоков, каждый {@code perThread} раз увеличивает общий счётчик. Верни итог.
     * Используй {@link AtomicInteger#incrementAndGet()} — атомарно, без гонок. Ожидается {@code threads*perThread}.
     */
    public static int atomicIncrements(int threads, int perThread) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л4 atomicIncrements");
    }

    /**
     * Л5. То же, но защити общий {@code int} блоком {@code synchronized} на общем объекте-замке
     * (интринсик-лок). Ожидается {@code threads*perThread}.
     */
    public static int synchronizedCount(int threads, int perThread) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л5 synchronizedCount");
    }

    /**
     * Л6. Видимость через {@code volatile}: запусти поток, крутящийся в {@code while (flag.running)},
     * затем сбрось флаг и дождись остановки. Верни {@code true}, если поток завершился. Поле
     * {@link VolatileFlag#running} объявлено {@code volatile} — иначе поток мог бы НЕ увидеть смену
     * значения и зациклиться навсегда.
     */
    public static boolean stopsWhenFlagCleared() throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л6 stopsWhenFlagCleared");
    }

    /**
     * Л7. Изоляция {@link ThreadLocal}: {@code threads} потоков кладут в общий ThreadLocal СВОЁ число и
     * потом читают его. Верни {@code true}, если каждый прочитал именно своё (потоки не мешают друг другу).
     */
    public static boolean threadLocalIsolation(int threads) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л7 threadLocalIsolation");
    }

    /**
     * Л8. Уникальные id: {@code threads} потоков берут номера через {@link AtomicInteger#getAndIncrement()}.
     * Верни число РАЗЛИЧНЫХ выданных id — атомарность гарантирует отсутствие дублей ({@code threads*perThread}).
     */
    public static int uniqueIds(int threads, int perThread) throws InterruptedException {
        throw new UnsupportedOperationException("TODO Л8 uniqueIds");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Happens-before через {@code join}: поток пишет в ОБЫЧНОЕ (не volatile) поле, а главный после
     * {@code join()} гарантированно видит запись. Верни прочитанное значение (ожидается 42).
     */
    public static int visibleAfterJoin() throws InterruptedException {
        throw new UnsupportedOperationException("TODO С9 visibleAfterJoin");
    }

    /**
     * С10. Атомарный максимум через CAS-цикл: обнови {@code current} до {@code candidate}, только если тот
     * больше. Подсказка: цикл {@code while(true)} с {@link AtomicInteger#compareAndSet(int,int)} —
     * перечитываем текущее значение и пробуем заменить, повторяя при неудаче.
     */
    public static void casMax(AtomicInteger current, int candidate) {
        throw new UnsupportedOperationException("TODO С10 casMax");
    }

    /**
     * С11. Идемпотентная одноразовая защёлка на {@code wait}/{@code notifyAll}: {@code await()} блокирует,
     * пока не позвали {@code release()}. Реализуй оба метода. {@code await} должен ждать в ЦИКЛЕ по условию
     * (защита от «ложных пробуждений»).
     */
    public static final class OneShotLatch {
        private boolean released = false;
        public synchronized void await() throws InterruptedException {
            throw new UnsupportedOperationException("TODO С11 OneShotLatch.await");
        }
        public synchronized void release() {
            throw new UnsupportedOperationException("TODO С11 OneShotLatch.release");
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
            throw new UnsupportedOperationException("TODO С12 Lazy.get");
        }
    }

    /**
     * С13. Неблокирующее накопление в {@link AtomicReference}: {@code threads} потоков по {@code perThread}
     * раз добавляют элемент в неизменяемый список через {@link AtomicReference#updateAndGet} (создаём КОПИЮ,
     * CAS повторится при конфликте). Верни итоговый размер ({@code threads*perThread}).
     */
    public static int concurrentAppend(int threads, int perThread) throws InterruptedException {
        throw new UnsupportedOperationException("TODO С13 concurrentAppend");
    }

    /**
     * С14. Параллельная сумма массива: раздели {@code data} на {@code threads} частей, каждый поток суммирует
     * свою часть и добавляет к общему итогу через {@code AtomicLong.addAndGet}. Верни сумму всего массива.
     */
    public static long parallelArraySum(int[] data, int threads) throws InterruptedException {
        throw new UnsupportedOperationException("TODO С14 parallelArraySum");
    }

    /**
     * С15. Прерывание потока: запусти поток в долгом {@code Thread.sleep}, прерви его {@code interrupt()} и
     * верни {@code true}, если поток поймал {@link InterruptedException}. Так в Java/Android кооперативно
     * ОТМЕНЯЮТ работу потока.
     */
    public static boolean interruptStops() throws InterruptedException {
        throw new UnsupportedOperationException("TODO С15 interruptStops");
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
            throw new UnsupportedOperationException("TODO СЛ16 BoundedBuffer.put");
        }
        public synchronized E take() throws InterruptedException {
            throw new UnsupportedOperationException("TODO СЛ16 BoundedBuffer.take");
        }
    }

    /**
     * СЛ17. Lock-free стек Трайбера на {@link AtomicReference}: {@code push}/{@code pop} через CAS-цикл, без
     * блокировок. Реализуй оба (при пустом стеке {@code pop} возвращает {@code null}).
     */
    public static final class TreiberStack<E> {
        private final AtomicReference<Node<E>> top = new AtomicReference<>();
        public void push(E value) {
            throw new UnsupportedOperationException("TODO СЛ17 TreiberStack.push");
        }
        public E pop() {
            throw new UnsupportedOperationException("TODO СЛ17 TreiberStack.pop");
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
        throw new UnsupportedOperationException("TODO СЛ18 publishThenRead");
    }

    /**
     * СЛ19. Перевод без дедлока: захватывай мониторы двух счетов в ЕДИНОМ порядке (по {@code id}), иначе два
     * встречных перевода могут заблокировать друг друга. Реализуй, сохраняя суммарный баланс.
     */
    public static void orderedTransfer(Account from, Account to, long amount) {
        throw new UnsupportedOperationException("TODO СЛ19 orderedTransfer");
    }

    /**
     * СЛ20. Спин-лок из CAS: реализуй взаимное исключение поверх {@link AtomicBoolean} — {@code lock()}
     * крутит {@code compareAndSet(false, true)}, пока не захватит; {@code unlock()} освобождает. Такой лок
     * защищает критическую секцию без {@code synchronized}.
     */
    public static final class SpinLock {
        private final AtomicBoolean locked = new AtomicBoolean(false);
        public void lock() {
            throw new UnsupportedOperationException("TODO СЛ20 SpinLock.lock");
        }
        public void unlock() {
            throw new UnsupportedOperationException("TODO СЛ20 SpinLock.unlock");
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
