package handbook.java.memory;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Тема 9 «Память, GC и ссылки» — 20 задач.
 * Реализуй помеченные {@code TODO} тела/методы. Инфраструктура ({@link Outer}, {@link Node},
 * {@link PseudoActivity}, статическое поле и GC-хелперы {@link #awaitCleared}/{@link #forceGc}) дана целиком.
 * Проверка: {@code ./gradlew test --tests "handbook.java.memory.*"}.
 * Эталон — в {@link handbook.java.memory.solutions.Solutions}.
 *
 * GC недетерминирован; тесты сборки используют {@link #awaitCleared} (крутит {@code System.gc()} до
 * фактической очистки) — на HotSpot/JBR это надёжно.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Оберни значение в {@link WeakReference}. Слабая ссылка НЕ мешает сборщику собрать объект. */
    public static <T> WeakReference<T> makeWeak(T value) {
        throw new UnsupportedOperationException("TODO Л1 makeWeak");
    }

    /** Л2. Достань объект из ссылки. Подсказка: {@code ref.get()} (вернёт {@code null}, если уже собран). */
    public static <T> T deref(Reference<T> ref) {
        throw new UnsupportedOperationException("TODO Л2 deref");
    }

    /** Л3. Явно очищенная слабая ссылка возвращает {@code null}: создай WeakReference, вызови {@code clear()}, верни {@code get()}. */
    public static Object clearedWeakIsNull() {
        throw new UnsupportedOperationException("TODO Л3 clearedWeakIsNull");
    }

    /** Л4. Оберни значение в {@link SoftReference} — её сборщик очищает лишь при НЕХВАТКЕ памяти (кэш). */
    public static <T> SoftReference<T> makeSoft(T value) {
        throw new UnsupportedOperationException("TODO Л4 makeSoft");
    }

    /**
     * Л5. Держит ли класс НЕЯВНУЮ ссылку на внешний объект? У нестатического внутреннего класса, который
     * обращается к хозяину, есть синтетическое поле {@code this$0}; у статического вложенного — нет. Верни,
     * есть ли такое поле. Именно оно и приводит к утечкам в Android. Подсказка: {@code cls.getDeclaredFields()},
     * {@code f.isSynthetic()}, имя начинается с {@code "this$"}.
     */
    public static boolean capturesOuter(Class<?> cls) {
        throw new UnsupportedOperationException("TODO Л5 capturesOuter");
    }

    /**
     * Л6. Верни ВНЕШНИЙ объект, на который ссылается внутренний экземпляр (через рефлексию поля {@code this$0}).
     * Показывает буквально: нестатический внутренний класс держит ссылку на объект-хозяина.
     */
    public static Object outerReferenceOf(Object inner) throws Exception {
        throw new UnsupportedOperationException("TODO Л6 outerReferenceOf");
    }

    /**
     * Л7. Гигиена {@link ThreadLocal}: положи значение, затем {@code remove()} и верни {@code get()} ({@code null}).
     * В пулах потоков незачищенный ThreadLocal живёт вместе с потоком — источник утечек.
     */
    public static Object threadLocalRemove() {
        throw new UnsupportedOperationException("TODO Л7 threadLocalRemove");
    }

    /**
     * Л8. Достижимый объект НЕ собирается: держи сильную ссылку, создай к нему слабую, прогони {@link #forceGc}
     * и верни {@code true}, если объект жив ({@code weak.get() != null}). GC никогда не трогает достижимое.
     */
    public static boolean reachableSurvivesGc() {
        throw new UnsupportedOperationException("TODO Л8 reachableSurvivesGc");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Недостижимый объект собирается: создай объект, слабую ссылку, ОБНУЛИ сильную и верни, был ли он
     * собран ({@link #awaitCleared}). Подсказка: {@code obj = null; return awaitCleared(weak);}.
     */
    public static boolean gcCollectsUnreachable() {
        throw new UnsupportedOperationException("TODO С9 gcCollectsUnreachable");
    }

    /**
     * С10. Утечка через статическое поле: положи объект в {@link Holders#STATIC_LEAK} (это GC-root), обнули
     * локальную ссылку, прогони {@link #forceGc} и верни {@code true}, если объект ЖИВ (статик держит → не собрать).
     */
    public static boolean staticFieldKeepsAlive() {
        throw new UnsupportedOperationException("TODO С10 staticFieldKeepsAlive");
    }

    /**
     * С11. Лечение утечки: обнули {@link Holders#STATIC_LEAK}, и объект становится собираемым. Помести объект
     * в статик, сделай слабую ссылку, обнули статик и верни {@link #awaitCleared} (ожидается {@code true}).
     */
    public static boolean clearStaticFrees() {
        throw new UnsupportedOperationException("TODO С11 clearStaticFrees");
    }

    /**
     * С12. {@link java.util.WeakHashMap}: ключи держатся СЛАБО — когда на ключ нет других ссылок, запись
     * исчезает. Положи запись, обнули сильную ссылку на ключ, прогони GC и верни размер карты (ожидается 0).
     */
    public static int weakHashMapEvicts() {
        throw new UnsupportedOperationException("TODO С12 weakHashMapEvicts");
    }

    /**
     * С13. {@link ReferenceQueue}: слабую ссылку с очередью после сборки объекта СБ ставит в очередь (сигнал
     * «пора чистить»). Обнули сильную ссылку и верни {@code true}, если ссылка попала в очередь.
     * Подсказка: {@code new WeakReference<>(obj, queue)} + {@link #awaitEnqueued}.
     */
    public static boolean referenceQueueEnqueued() {
        throw new UnsupportedOperationException("TODO С13 referenceQueueEnqueued");
    }

    /**
     * С14. Кэш со СЛАБЫМИ значениями: {@code put} хранит значение как {@link WeakReference}, {@code get}
     * возвращает значение или {@code null}, если оно уже собрано. Реализуй оба метода.
     */
    public static final class WeakValueCache<K, V> {
        private final Map<K, WeakReference<V>> map = new HashMap<>();
        public void put(K key, V value) {
            throw new UnsupportedOperationException("TODO С14 WeakValueCache.put");
        }
        public V get(K key) {
            throw new UnsupportedOperationException("TODO С14 WeakValueCache.get");
        }
    }

    /**
     * С15. Детерминированная очистка вместо {@code finalize()} (тот устарел и ненадёжен): используй
     * {@link AutoCloseable} + try-with-resources. Верни {@code true}, если {@code close()} выполнился.
     */
    public static boolean autoCloseableCleanup() {
        throw new UnsupportedOperationException("TODO С15 autoCloseableCleanup");
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Кэш с {@link SoftReference}-значениями (память-чувствительный): {@code get} возвращает значение,
     * пока сборщик его не вытеснил под давлением памяти. Реализуй {@code put} и {@code get}.
     */
    public static final class SoftCache<K, V> {
        private final Map<K, SoftReference<V>> map = new HashMap<>();
        public void put(K key, V value) {
            throw new UnsupportedOperationException("TODO СЛ16 SoftCache.put");
        }
        public V get(K key) {
            throw new UnsupportedOperationException("TODO СЛ16 SoftCache.get");
        }
    }

    /**
     * СЛ17. Лечение утечки «как в Handler»: долгоживущая очередь должна держать {@link PseudoActivity}
     * СЛАБО, чтобы activity могла быть собрана после закрытия. Верни {@link WeakReference} на activity
     * (её и «положил бы» в отложенное сообщение безопасный код).
     */
    public static WeakReference<PseudoActivity> scheduleWeakly(PseudoActivity activity) {
        throw new UnsupportedOperationException("TODO СЛ17 scheduleWeakly");
    }

    /**
     * СЛ18. Транзитивная достижимость: A ссылается на B. Обнули A — и B тоже станет недостижимым. Создай
     * цепочку {@link Node}, слабые ссылки на оба узла, обнули A и верни {@code true}, если СОБРАНЫ ОБА.
     */
    public static boolean transitiveCollection() {
        throw new UnsupportedOperationException("TODO СЛ18 transitiveCollection");
    }

    /**
     * СЛ19. {@link java.lang.ref.PhantomReference} + {@link ReferenceQueue} — пост-мортем очистка: фантомную
     * ссылку СБ ставит в очередь ПОСЛЕ сборки (её {@code get()} всегда {@code null}). Обнули объект и верни
     * {@code true}, если фантом попал в очередь (сигнал безопасно освобождать нативные ресурсы).
     */
    public static boolean phantomCleanup() {
        throw new UnsupportedOperationException("TODO СЛ19 phantomCleanup");
    }

    /**
     * СЛ20. Мини-детектор утечек (суть LeakCanary): создай объект фабрикой, слабую ссылку, «забудь» объект и
     * верни, был ли он собран. {@code true} — объект утилизируется; {@code false} — что-то держит его (утечка).
     */
    public static boolean isCollectable(Supplier<Object> factory) {
        throw new UnsupportedOperationException("TODO СЛ20 isCollectable");
    }

    // ═══════════════════════════ Инфраструктура и GC-хелперы (даны целиком) ═══════════════════════════

    /**
     * Внешний класс. {@link Inner} нестатический и ОБРАЩАЕТСЯ к методу хозяина — компилятор создаёт
     * синтетическое поле {@code this$0} (ссылку на {@code Outer}). {@link StaticNested} статический —
     * никакой ссылки на хозяина. (Нюанс: у inner, НЕ использующего внешний объект, {@code this$0}
     * может и не появиться — ссылка возникает именно из-за обращения к хозяину.)
     */
    public static final class Outer {
        private String label() { return "outer"; }
        public final class Inner {
            public String describe() { return label(); }   // вызов метода Outer → нужен this$0
        }
        public static final class StaticNested {
            public String describe() { return "nested"; }
        }
    }

    /** Узел односвязной цепочки — для демонстрации транзитивной достижимости. */
    public static final class Node {
        public final Node next;
        public Node(Node next) { this.next = next; }
    }

    /** Заглушка «Activity» для демонстрации утечки/её лечения (аналог Android Activity/Context). */
    public static final class PseudoActivity {
        public final String name;
        public PseudoActivity(String name) { this.name = name; }
    }

    /** Держатель со статическим полем — статик является GC-root (типичный источник утечек). */
    public static final class Holders {
        public static Object STATIC_LEAK;
    }

    /** Крутит {@code System.gc()} + аллокации, пока {@code ref} не очистится (или лимит). Возвращает, очищена ли. */
    public static boolean awaitCleared(Reference<?> ref) {
        for (int i = 0; i < 200 && ref.get() != null; i++) gcPressure();
        return ref.get() == null;
    }

    /** Крутит GC, пока в очереди не появится ссылка (сигнал о сборке). Возвращает, появилась ли. */
    public static boolean awaitEnqueued(ReferenceQueue<?> queue) {
        for (int i = 0; i < 200; i++) {
            gcPressure();
            if (queue.poll() != null) return true;
        }
        return false;
    }

    /** Несколько проходов GC под давлением памяти (для проверок «объект жив»). */
    public static void forceGc() {
        for (int i = 0; i < 10; i++) gcPressure();
    }

    private static void gcPressure() {
        System.gc();
        @SuppressWarnings("unused")
        byte[] junk = new byte[512 * 1024];
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
