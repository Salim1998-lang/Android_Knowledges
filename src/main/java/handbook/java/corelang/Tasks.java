package handbook.java.corelang;

import java.util.List;
import java.util.Map;

/**
 * Тема 1 «Язык, типы, строки» — 20 задач.
 * Реализуй методы (замени тело {@code throw new UnsupportedOperationException(...)}).
 * Проверка: {@code ./gradlew test --tests "handbook.java.corelang.*"}.
 * Эталон — в {@link handbook.java.corelang.solutions.Solutions}.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /**
     * Л1. Упакуй {@code value} в {@link Integer} дважды через autoboxing и верни, ссылаются ли
     * обе обёртки на ОДИН объект ({@code ==}). Подсказка: {@code Integer x = value; Integer y = value;}
     * — для −128..127 сработает кэш {@code Integer} и вернётся {@code true}, иначе {@code false}.
     */
    public static boolean boxedIdentity(int value) {
        throw new UnsupportedOperationException("TODO Л1 boxedIdentity");
    }

    /** Л2. Верни {@code a + b} как {@code int}. Для {@code MAX_VALUE + 1} получится заворот в минус — это нормально. */
    public static int wrappingSum(int a, int b) {
        throw new UnsupportedOperationException("TODO Л2 wrappingSum");
    }

    /** Л3. Верни сумму {@code a + b} БЕЗ переполнения — как {@code long}. Подсказка: {@code (long) a + b}. */
    public static long safeSum(int a, int b) {
        throw new UnsupportedOperationException("TODO Л3 safeSum");
    }

    /** Л4. Верни строку {@code s}, развёрнутую задом наперёд. Подсказка: {@code new StringBuilder(s).reverse()}. */
    public static String reverse(String s) {
        throw new UnsupportedOperationException("TODO Л4 reverse");
    }

    /**
     * Л5. Верни, равны ли строки ПО СОДЕРЖИМОМУ, не падая на {@code null}.
     * Подсказка: {@code java.util.Objects.equals(a, b)} (не {@code ==}, не {@code a.equals(b)}).
     */
    public static boolean contentEquals(String a, String b) {
        throw new UnsupportedOperationException("TODO Л5 contentEquals");
    }

    /** Л6. Верни числовое значение цифрового символа: {@code '7' → 7}. Подсказка: {@code c - '0'}. */
    public static int digitToInt(char c) {
        throw new UnsupportedOperationException("TODO Л6 digitToInt");
    }

    /** Л7. Верни {@code "positive"}, {@code "zero"} или {@code "negative"} для знака {@code n}. */
    public static String sign(int n) {
        throw new UnsupportedOperationException("TODO Л7 sign");
    }

    /**
     * Л8. Распакуй {@code i} в {@code int}, но если {@code i == null} — верни 0 (без NPE).
     * Подсказка: {@code i == null ? 0 : i}.
     */
    public static int unboxOrZero(Integer i) {
        throw new UnsupportedOperationException("TODO Л8 unboxOrZero");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Верни, указывают ли {@code a.intern()} и {@code b.intern()} на один объект пула ({@code ==}).
     * Для равных по содержимому строк — {@code true}. Подсказка: {@code a.intern() == b.intern()}.
     */
    public static boolean internedSameRef(String a, String b) {
        throw new UnsupportedOperationException("TODO С9 internedSameRef");
    }

    /**
     * С10. Склей {@code parts} через разделитель {@code sep} БЕЗ хвостового разделителя, используя
     * {@link StringBuilder}. Для {@code ["a","b","c"], "-"} → {@code "a-b-c"}. Пустой список → {@code ""}.
     */
    public static String join(List<String> parts, String sep) {
        throw new UnsupportedOperationException("TODO С10 join");
    }

    /**
     * С11. Верни, равны ли массивы ПО СОДЕРЖИМОМУ (не по ссылке!). Подсказка: {@code java.util.Arrays.equals}.
     */
    public static boolean arraysContentEqual(int[] a, int[] b) {
        throw new UnsupportedOperationException("TODO С11 arraysContentEqual");
    }

    /**
     * С12. Верни, равны ли {@code a} и {@code b} с точностью до {@code eps} (для double нельзя {@code ==}).
     * Подсказка: {@code Math.abs(a - b) < eps}.
     */
    public static boolean nearlyEqual(double a, double b, double eps) {
        throw new UnsupportedOperationException("TODO С12 nearlyEqual");
    }

    /**
     * С13. Верни {@code a mod b} с математическим (неотрицательным для {@code b>0}) остатком:
     * {@code floorMod(-1, 3) == 2}, тогда как {@code -1 % 3 == -1}. Подсказка: {@code Math.floorMod}.
     */
    public static int floorMod(int a, int b) {
        throw new UnsupportedOperationException("TODO С13 floorMod");
    }

    /**
     * С14. Удвой на месте нулевой элемент массива: {@code arr[0] *= 2}. Метод ничего не возвращает —
     * изменение видно вызывающему, потому что меняем сам объект по ссылке (а не переприсваиваем параметр).
     */
    public static void doubleFirst(int[] arr) {
        throw new UnsupportedOperationException("TODO С14 doubleFirst");
    }

    /**
     * С15. Сравни строки лексикографически и верни нормализованный знак: −1, 0 или 1.
     * Подсказка: {@code Integer.signum(a.compareTo(b))}.
     */
    public static int normalizedCompare(String a, String b) {
        throw new UnsupportedOperationException("TODO С15 normalizedCompare");
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. Верни частоту каждого символа строки как {@code Map<Character, Integer>} (порядок не важен).
     * Для {@code "aab"} → {@code {a=2, b=1}}. Подсказка: {@code map.merge(c, 1, Integer::sum)}
     * — заодно посмотри, как autoboxing упаковывает и ключ-{@code char}, и значение-{@code int}.
     */
    public static Map<Character, Integer> charFrequency(String s) {
        throw new UnsupportedOperationException("TODO СЛ16 charFrequency");
    }

    /**
     * СЛ17. Разверни порядок СЛОВ (разделены одиночными пробелами), сами слова не трогай.
     * {@code "one two three"} → {@code "three two one"}. Подсказка: {@code s.split(" ")} + {@link StringBuilder}.
     */
    public static String reverseWords(String s) {
        throw new UnsupportedOperationException("TODO СЛ17 reverseWords");
    }

    /**
     * СЛ18. Попробуй распарсить {@code s} в число: верни {@link Integer} при успехе или {@code null},
     * если это не целое (обёртка нужна именно чтобы уметь вернуть {@code null}). Подсказка:
     * {@code try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }}.
     */
    public static Integer tryParseInt(String s) {
        throw new UnsupportedOperationException("TODO СЛ18 tryParseInt");
    }

    /**
     * СЛ19. Палиндром ли строка, если игнорировать регистр и все не-буквенно-цифровые символы?
     * {@code "A man, a plan, a canal: Panama"} → {@code true}. Подсказка: два указателя с концов,
     * {@code Character.isLetterOrDigit} и {@code Character.toLowerCase}.
     */
    public static boolean isPalindrome(String s) {
        throw new UnsupportedOperationException("TODO СЛ19 isPalindrome");
    }

    /**
     * СЛ20. Верни среднее (округляя вниз) двух {@code int} БЕЗ переполнения при больших значениях.
     * Наивное {@code (low + high) / 2} переполняется. Подсказка: {@code low + (high - low) / 2}
     * (предполагаем {@code low <= high}).
     */
    public static int midpoint(int low, int high) {
        throw new UnsupportedOperationException("TODO СЛ20 midpoint");
    }
}
