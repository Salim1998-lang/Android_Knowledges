package handbook.java.generics;

import java.util.List;
import java.util.Map;

/**
 * Тема 3 «Дженерики» — 20 задач.
 * Реализуй помеченные {@code TODO} тела. Типы {@link Box} и {@link Pair} даны целиком.
 * Проверка: {@code ./gradlew test --tests "handbook.java.generics.*"}.
 * Эталон — в {@link handbook.java.generics.solutions.Solutions}.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    /** Обобщённый контейнер одного значения. */
    public static final class Box<T> {
        private final T value;
        public Box(T value) { this.value = value; }
        public T get() { return value; }
    }

    /** Обобщённая пара из двух (возможно, разных) типов. */
    public static final class Pair<A, B> {
        private final A first;
        private final B second;
        public Pair(A first, B second) { this.first = first; this.second = second; }
        public A first() { return first; }
        public B second() { return second; }
    }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Верни первый элемент списка. Тип-параметр {@code <T>} делает метод обобщённым. */
    public static <T> T firstOf(List<T> list) {
        throw new UnsupportedOperationException("TODO Л1 firstOf");
    }

    /** Л2. Верни новый изменяемый список из двух элементов {@code [a, b]}. */
    public static <T> List<T> pairList(T a, T b) {
        throw new UnsupportedOperationException("TODO Л2 pairList");
    }

    /** Л3. Поменяй местами элементы списка на позициях {@code i} и {@code j}. */
    public static <T> void swap(List<T> list, int i, int j) {
        throw new UnsupportedOperationException("TODO Л3 swap");
    }

    /** Л4. Заверни значение в {@link Box}. Подсказка: {@code new Box<>(value)} (ромбовидный оператор). */
    public static <T> Box<T> boxOf(T value) {
        throw new UnsupportedOperationException("TODO Л4 boxOf");
    }

    /** Л5. Собери {@link Pair} из двух значений разных типов. Подсказка: {@code new Pair<>(a, b)}. */
    public static <A, B> Pair<A, B> pair(A a, B b) {
        throw new UnsupportedOperationException("TODO Л5 pair");
    }

    /**
     * Л6. Верни максимум списка. Ограничение {@code <T extends Comparable<T>>} гарантирует, что
     * элементы можно сравнивать через {@code compareTo}. Список непустой.
     */
    public static <T extends Comparable<T>> T maxOf(List<T> list) {
        throw new UnsupportedOperationException("TODO Л6 maxOf");
    }

    /** Л7. Посчитай, сколько раз {@code target} встречается в списке (по {@code equals}). */
    public static <T> int frequency(List<T> list, T target) {
        throw new UnsupportedOperationException("TODO Л7 frequency");
    }

    /** Л8. Верни список из {@code times} копий {@code value}. */
    public static <T> List<T> repeat(T value, int times) {
        throw new UnsupportedOperationException("TODO Л8 repeat");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Просуммируй числа как {@code double}. {@code ? extends Number} — верхняя граница: список
     * «производит» Number'ы, из него можно ЧИТАТЬ (принимает {@code List<Integer>}, {@code List<Double>}…).
     * Это КОВАРИАНТНОСТЬ на стороне использования.
     */
    public static double sumOfNumbers(List<? extends Number> list) {
        throw new UnsupportedOperationException("TODO С9 sumOfNumbers");
    }

    /**
     * С10. Добавь в приёмник числа {@code 1..n}. {@code ? super Integer} — нижняя граница: список
     * «потребляет» Integer'ы, в него можно ПИСАТЬ (принимает {@code List<Integer>}, {@code List<Number>}, {@code List<Object>}).
     * Это КОНТРАВАРИАНТНОСТЬ на стороне использования.
     */
    public static void addInts(List<? super Integer> dst, int n) {
        throw new UnsupportedOperationException("TODO С10 addInts");
    }

    /**
     * С11. Скопируй все элементы из {@code src} в {@code dst}. Классический PECS: источник —
     * {@code ? extends T} (producer, ковариантно), приёмник — {@code ? super T} (consumer, контравариантно).
     */
    public static <T> void copyAll(List<? extends T> src, List<? super T> dst) {
        throw new UnsupportedOperationException("TODO С11 copyAll");
    }

    /**
     * С12. Вариантность: МАССИВЫ ковариантны ({@code Integer[]} является {@code Object[]}) — и потому
     * небезопасны: запись чужого типа падает в рантайме {@link ArrayStoreException}. ДЖЕНЕРИКИ же
     * инвариантны ({@code List<Integer>} НЕ является {@code List<Object>}) — этот же баг ловится ещё на
     * компиляции. Заведи {@code Object[] arr = new Integer[2]}, попробуй записать в {@code arr[0]} строку
     * и верни {@code true}, если поймал {@link ArrayStoreException}.
     */
    public static boolean arrayStoreThrows() {
        throw new UnsupportedOperationException("TODO С12 arrayStoreThrows");
    }

    /**
     * С13. Максимум, но с «правильной» границей {@code Comparable<? super T>} — так метод принимает и
     * типы, чей {@code compareTo} определён у СУПЕРтипа (как у {@code Collections.max}). Список непустой.
     */
    public static <T extends Comparable<? super T>> T maxBound(List<T> list) {
        throw new UnsupportedOperationException("TODO С13 maxBound");
    }

    /** С14. Верни {@code true}, если все элементы равны между собой (пустой список — {@code true}). */
    public static <T> boolean allEqual(List<T> list) {
        throw new UnsupportedOperationException("TODO С14 allEqual");
    }

    /** С15. Инвертируй карту: {@code key→value} становится {@code value→key} (значения уникальны). */
    public static <K, V> Map<V, K> invert(Map<K, V> map) {
        throw new UnsupportedOperationException("TODO С15 invert");
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Верни, совпадают ли рантайм-классы двух списков разной параметризации. Из-за СТИРАНИЯ
     * типов {@code List<String>} и {@code List<Integer>} на этапе выполнения — один и тот же
     * {@code ArrayList} (дженерики существуют только в компиляторе). Ожидается {@code true}.
     */
    public static boolean sameRawClass(List<String> a, List<Integer> b) {
        throw new UnsupportedOperationException("TODO СЛ16 sameRawClass");
    }

    /**
     * СЛ17. Скопируй список в массив. {@code new T[]} писать НЕЛЬЗЯ (стирание) — поэтому массив
     * передают снаружи как {@code template}. Подсказка: {@code list.toArray(template)}.
     */
    public static <T> T[] toArray(List<T> list, T[] template) {
        throw new UnsupportedOperationException("TODO СЛ17 toArray");
    }

    /**
     * СЛ18. Приведи {@code o} к типу {@code type}, если возможно, иначе верни {@code null}. Проверить
     * {@code o instanceof T} НЕЛЬЗЯ (стирание) — используем токен {@code Class<T>}. Подсказка:
     * {@code type.isInstance(o) ? type.cast(o) : null}.
     */
    public static <T> T castOrNull(Object o, Class<T> type) {
        throw new UnsupportedOperationException("TODO СЛ18 castOrNull");
    }

    /**
     * Разворот списка, у которого тип не важен: публичный метод берёт {@code List<?>} и делегирует
     * приватному обобщённому помощнику — это «захват wildcard» (wildcard capture).
     */
    public static void reverse(List<?> list) {
        reverseHelper(list);
    }

    /** СЛ19. Реализуй разворот на месте (два указателя с концов через {@link #swap}). */
    private static <T> void reverseHelper(List<T> list) {
        throw new UnsupportedOperationException("TODO СЛ19 reverseHelper");
    }

    /**
     * СЛ20. Верни {@link Pair} (минимум, максимум) списка. Ограничение {@code Comparable} + обобщённая
     * пара вместе. Список непустой.
     */
    public static <T extends Comparable<T>> Pair<T, T> minMax(List<T> list) {
        throw new UnsupportedOperationException("TODO СЛ20 minMax");
    }
}
