package handbook.java.collections.solutions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Эталонные решения темы 4. Подсмотри, если застрял с {@link handbook.java.collections.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Убери дубликаты, СОХРАНИВ порядок первого появления. Подсказка: {@link LinkedHashSet}. */
    public static <T> List<T> distinct(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    /** Л2. Есть ли в списке дубликаты? Подсказка: сравни размер {@code HashSet} и списка. */
    public static <T> boolean hasDuplicates(List<T> list) {
        return new HashSet<>(list).size() < list.size();
    }

    /** Л3. Посчитай, сколько раз встречается каждый элемент. Подсказка: {@code map.merge(x, 1, Integer::sum)}. */
    public static <T> Map<T, Integer> countBy(List<T> list) {
        Map<T, Integer> counts = new LinkedHashMap<>();
        for (T x : list) counts.merge(x, 1, Integer::sum);
        return counts;
    }

    /** Л4. Сумма всех значений карты. */
    public static int sumValues(Map<String, Integer> map) {
        int sum = 0;
        for (int v : map.values()) sum += v;
        return sum;
    }

    /** Л5. Верни ОТСОРТИРОВАННУЮ КОПИЮ (исходный список не меняем). Подсказка: копия + {@code Collections.sort}. */
    public static List<Integer> sortedCopy(List<Integer> list) {
        List<Integer> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return copy;
    }

    /** Л6. Верни РАЗВЁРНУТУЮ КОПИЮ. Подсказка: копия + {@code Collections.reverse}. */
    public static <T> List<T> reversedCopy(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    /** Л7. Объединение множеств (все элементы обоих). Подсказка: {@code addAll}. */
    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    /** Л8. Пересечение множеств (общие элементы). Подсказка: {@code retainAll}. */
    public static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * Точка в 2D. Чтобы работать КЛЮЧОМ в {@code HashMap}/элементом {@code HashSet}, класс обязан
     * переопределить {@code equals} И {@code hashCode} согласованно.
     */
    public static final class Point {
        final int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }

        /** С9 (часть 1). Равенство по содержимому: та же пара координат. */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        /** С9 (часть 2). Согласованный с equals хеш. Подсказка: {@code Objects.hash(x, y)}. */
        @Override public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /** С9. Сколько РАЗНЫХ точек в списке (равные по координатам считаются одной). Использует equals/hashCode. */
    public static int uniquePoints(List<Point> points) {
        return new HashSet<>(points).size();
    }

    /**
     * С10. Сгруппируй слова по длине: {@code длина → список слов} (порядок слов внутри — как во входе).
     * Подсказка: {@code map.computeIfAbsent(len, k -> new ArrayList<>()).add(word)}.
     */
    public static Map<Integer, List<String>> groupByLength(List<String> words) {
        Map<Integer, List<String>> groups = new LinkedHashMap<>();
        for (String w : words) {
            groups.computeIfAbsent(w.length(), k -> new ArrayList<>()).add(w);
        }
        return groups;
    }

    /**
     * С11. Отсортируй КОПИЮ строк: сначала по длине (возр.), при равной длине — лексикографически.
     * Подсказка: {@code Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())}.
     */
    public static List<String> sortByLengthThenAlpha(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        copy.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        return copy;
    }

    /** Версия вида {@code major.minor} с естественным порядком (для сортировки/TreeSet). */
    public static final class Version implements Comparable<Version> {
        final int major, minor;
        public Version(int major, int minor) { this.major = major; this.minor = minor; }

        /**
         * С12. Естественный порядок: сначала по {@code major}, при равенстве — по {@code minor}.
         * Верни отрицательное/0/положительное. Подсказка: {@code Integer.compare(...)}.
         */
        @Override public int compareTo(Version other) {
            int byMajor = Integer.compare(major, other.major);
            return byMajor != 0 ? byMajor : Integer.compare(minor, other.minor);
        }
    }

    /**
     * С13. Верни наименьший ключ карты, который {@code >= key} (или {@code null}, если такого нет).
     * {@link TreeMap} отсортирован — у него есть навигация. Подсказка: {@code map.ceilingKey(key)}.
     */
    public static Integer ceilingKey(TreeMap<Integer, String> map, int key) {
        return map.ceilingKey(key);
    }

    /**
     * С14. Удали ИЗ ИСХОДНОГО списка все строки короче {@code minLen}. Во время итерации нельзя звать
     * {@code list.remove} (fail-fast → {@link ConcurrentModificationException}) — удаляй через
     * {@code Iterator.remove()}.
     */
    public static void removeShorterThan(List<String> list, int minLen) {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().length() < minLen) it.remove();
        }
    }

    /** С15. Слей две карты-счётчика, СУММИРУЯ значения одинаковых ключей. Подсказка: {@code merge}. */
    public static Map<String, Integer> mergeCounts(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> result = new LinkedHashMap<>(a);
        for (Map.Entry<String, Integer> e : b.entrySet()) {
            result.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        return result;
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * LRU-кэш поверх {@link LinkedHashMap} в режиме access-order: при переполнении вытесняется
     * давно не используемый элемент (так устроен и {@code android.util.LruCache}).
     */
    public static final class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;
        public LruCache(int capacity) {
            super(16, 0.75f, true);   // true = access-order: get двигает элемент в «свежие»
            this.capacity = capacity;
        }
        /** СЛ16. Вытесняй самый старый элемент, когда размер превысил {@code capacity}. */
        @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    /**
     * Строка с равенством БЕЗ учёта регистра. Демонстрирует: equals/hashCode задают СЕМАНТИКУ
     * равенства, и оба должны игнорировать регистр согласованно.
     */
    public static final class CIString {
        final String value;
        public CIString(String value) { this.value = value; }

        /** СЛ17 (часть 1). Равенство без учёта регистра. Подсказка: {@code equalsIgnoreCase}. */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CIString)) return false;
            return value.equalsIgnoreCase(((CIString) o).value);
        }

        /** СЛ17 (часть 2). Хеш, согласованный с equals: одинаковый для разного регистра. */
        @Override public int hashCode() {
            return value.toLowerCase().hashCode();
        }
    }

    /** СЛ17. Сколько РАЗНЫХ строк без учёта регистра ({@code "Hi"} и {@code "HI"} — одна). */
    public static int uniqueIgnoreCase(List<CIString> strings) {
        return new HashSet<>(strings).size();
    }

    /**
     * СЛ18. Верни ключи карты, отсортированные по значению по УБЫВАНИЮ; при равных значениях — по
     * ключу по возрастанию (детерминизм). Подсказка: отсортируй {@code entrySet} компаратором.
     */
    public static List<String> sortKeysByValueDesc(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                .reversed()
                .thenComparing(Map.Entry::getKey));
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) keys.add(e.getKey());
        return keys;
    }

    /**
     * СЛ19. Верни самый частый элемент; при равенстве частот — тот, что встретился РАНЬШЕ.
     * Подсказка: {@link LinkedHashMap} счётчиков + один проход с строгим {@code >}. Список непустой.
     */
    public static <T> T mode(List<T> list) {
        Map<T, Integer> counts = new LinkedHashMap<>();
        for (T x : list) counts.merge(x, 1, Integer::sum);
        T best = null;
        int bestCount = 0;
        for (Map.Entry<T, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    /**
     * СЛ20. Fail-fast: измени список СТРУКТУРНО во время цикла for-each и верни {@code true}, поймав
     * {@link ConcurrentModificationException}. Так итераторы коллекций защищаются от порчи структуры.
     */
    public static boolean concurrentModThrows() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        try {
            for (Integer x : list) {
                if (x == 1) list.remove(x);   // структурное изменение во время итерации
            }
            return false;
        } catch (ConcurrentModificationException e) {
            return true;
        }
    }
}
