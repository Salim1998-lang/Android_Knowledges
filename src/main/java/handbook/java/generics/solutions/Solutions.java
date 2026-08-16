package handbook.java.generics.solutions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Эталонные решения темы 3. Подсмотри, если застрял с {@link handbook.java.generics.Tasks}. */
public final class Solutions {

    private Solutions() { }

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
        return list.get(0);
    }

    /** Л2. Верни новый изменяемый список из двух элементов {@code [a, b]}. */
    public static <T> List<T> pairList(T a, T b) {
        List<T> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        return list;
    }

    /** Л3. Поменяй местами элементы списка на позициях {@code i} и {@code j}. */
    public static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    /** Л4. Заверни значение в {@link Box}. Подсказка: {@code new Box<>(value)} (ромбовидный оператор). */
    public static <T> Box<T> boxOf(T value) {
        return new Box<>(value);
    }

    /** Л5. Собери {@link Pair} из двух значений разных типов. Подсказка: {@code new Pair<>(a, b)}. */
    public static <A, B> Pair<A, B> pair(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * Л6. Верни максимум списка. Ограничение {@code <T extends Comparable<T>>} гарантирует, что
     * элементы можно сравнивать через {@code compareTo}. Список непустой.
     */
    public static <T extends Comparable<T>> T maxOf(List<T> list) {
        T best = list.get(0);
        for (T x : list) {
            if (x.compareTo(best) > 0) best = x;
        }
        return best;
    }

    /** Л7. Посчитай, сколько раз {@code target} встречается в списке (по {@code equals}). */
    public static <T> int frequency(List<T> list, T target) {
        int count = 0;
        for (T x : list) {
            if (Objects.equals(x, target)) count++;
        }
        return count;
    }

    /** Л8. Верни список из {@code times} копий {@code value}. */
    public static <T> List<T> repeat(T value, int times) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < times; i++) list.add(value);
        return list;
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Просуммируй числа как {@code double}. {@code ? extends Number} — верхняя граница: список
     * «производит» Number'ы, из него можно ЧИТАТЬ (принимает {@code List<Integer>}, {@code List<Double>}…).
     */
    public static double sumOfNumbers(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) sum += n.doubleValue();
        return sum;
    }

    /**
     * С10. Добавь в приёмник числа {@code 1..n}. {@code ? super Integer} — нижняя граница: список
     * «потребляет» Integer'ы, в него можно ПИСАТЬ (принимает {@code List<Integer>}, {@code List<Number>}, {@code List<Object>}).
     */
    public static void addInts(List<? super Integer> dst, int n) {
        for (int i = 1; i <= n; i++) dst.add(i);
    }

    /**
     * С11. Скопируй все элементы из {@code src} в {@code dst}. Классический PECS: источник —
     * {@code ? extends T} (producer), приёмник — {@code ? super T} (consumer).
     */
    public static <T> void copyAll(List<? extends T> src, List<? super T> dst) {
        for (T t : src) dst.add(t);
    }

    /**
     * С12. Вариантность: МАССИВЫ ковариантны ({@code Integer[]} является {@code Object[]}) — и потому
     * небезопасны: запись чужого типа падает в рантайме {@link ArrayStoreException}. ДЖЕНЕРИКИ же
     * инвариантны ({@code List<Integer>} НЕ является {@code List<Object>}) — этот же баг ловится ещё на
     * компиляции. Заведи {@code Object[] arr = new Integer[2]}, попробуй записать в {@code arr[0]} строку
     * и верни {@code true}, если поймал {@link ArrayStoreException}.
     */
    public static boolean arrayStoreThrows() {
        Object[] arr = new Integer[2];         // разрешено: массивы ковариантны
        try {
            arr[0] = "not an Integer";         // компилируется, но бросает ArrayStoreException
            return false;
        } catch (ArrayStoreException e) {
            return true;
        }
    }

    /**
     * С13. Максимум, но с «правильной» границей {@code Comparable<? super T>} — так метод принимает и
     * типы, чей {@code compareTo} определён у СУПЕРтипа (как у {@code Collections.max}). Список непустой.
     */
    public static <T extends Comparable<? super T>> T maxBound(List<T> list) {
        T best = list.get(0);
        for (T x : list) {
            if (x.compareTo(best) > 0) best = x;
        }
        return best;
    }

    /** С14. Верни {@code true}, если все элементы равны между собой (пустой список — {@code true}). */
    public static <T> boolean allEqual(List<T> list) {
        if (list.isEmpty()) return true;
        T first = list.get(0);
        for (T x : list) {
            if (!Objects.equals(x, first)) return false;
        }
        return true;
    }

    /** С15. Инвертируй карту: {@code key→value} становится {@code value→key} (значения уникальны). */
    public static <K, V> Map<V, K> invert(Map<K, V> map) {
        Map<V, K> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : map.entrySet()) {
            result.put(e.getValue(), e.getKey());
        }
        return result;
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Верни, совпадают ли рантайм-классы двух списков разной параметризации. Из-за СТИРАНИЯ
     * типов {@code List<String>} и {@code List<Integer>} на этапе выполнения — один и тот же
     * {@code ArrayList} (дженерики существуют только в компиляторе). Ожидается {@code true}.
     */
    public static boolean sameRawClass(List<String> a, List<Integer> b) {
        return a.getClass() == b.getClass();
    }

    /**
     * СЛ17. Скопируй список в массив. {@code new T[]} писать НЕЛЬЗЯ (стирание) — поэтому массив
     * передают снаружи как {@code template}. Подсказка: {@code list.toArray(template)}.
     */
    public static <T> T[] toArray(List<T> list, T[] template) {
        return list.toArray(template);
    }

    /**
     * СЛ18. Приведи {@code o} к типу {@code type}, если возможно, иначе верни {@code null}. Проверить
     * {@code o instanceof T} НЕЛЬЗЯ (стирание) — используем токен {@code Class<T>}. Подсказка:
     * {@code type.isInstance(o) ? type.cast(o) : null}.
     */
    public static <T> T castOrNull(Object o, Class<T> type) {
        return type.isInstance(o) ? type.cast(o) : null;
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
        int i = 0, j = list.size() - 1;
        while (i < j) {
            swap(list, i, j);
            i++;
            j--;
        }
    }

    /**
     * СЛ20. Верни {@link Pair} (минимум, максимум) списка. Ограничение {@code Comparable} + обобщённая
     * пара вместе. Список непустой.
     */
    public static <T extends Comparable<T>> Pair<T, T> minMax(List<T> list) {
        T min = list.get(0);
        T max = list.get(0);
        for (T x : list) {
            if (x.compareTo(min) < 0) min = x;
            if (x.compareTo(max) > 0) max = x;
        }
        return new Pair<>(min, max);
    }
}
