package handbook.java.corelang.solutions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Эталонные решения темы 1. Подсмотри, если застрял с {@link handbook.java.corelang.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ── Лёгкие ──

    public static boolean boxedIdentity(int value) {
        Integer x = value;   // autoboxing → Integer.valueOf(value)
        Integer y = value;
        return x == y;       // ссылочное сравнение: true только в кэше −128..127
    }

    public static int wrappingSum(int a, int b) {
        return a + b;        // при переполнении молча заворачивается
    }

    public static long safeSum(int a, int b) {
        return (long) a + b; // расширение до long ДО сложения — переполнения нет
    }

    public static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    public static boolean contentEquals(String a, String b) {
        return Objects.equals(a, b);  // null-безопасно, сравнение по содержимому
    }

    public static int digitToInt(char c) {
        return c - '0';      // коды '0'..'9' идут подряд
    }

    public static String sign(int n) {
        return n > 0 ? "positive" : (n < 0 ? "negative" : "zero");
    }

    public static int unboxOrZero(Integer i) {
        return i == null ? 0 : i;  // защита от NPE при распаковке
    }

    // ── Средние ──

    public static boolean internedSameRef(String a, String b) {
        return a.intern() == b.intern();  // равные по содержимому → один объект пула
    }

    public static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    public static boolean arraysContentEqual(int[] a, int[] b) {
        return java.util.Arrays.equals(a, b);  // == сравнил бы ссылки
    }

    public static boolean nearlyEqual(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    public static int floorMod(int a, int b) {
        return Math.floorMod(a, b);
    }

    public static void doubleFirst(int[] arr) {
        arr[0] *= 2;  // меняем сам объект по ссылке — видно вызывающему
    }

    public static int normalizedCompare(String a, String b) {
        return Integer.signum(a.compareTo(b));
    }

    // ── Сложные ──

    public static Map<Character, Integer> charFrequency(String s) {
        Map<Character, Integer> freq = new LinkedHashMap<>();
        for (int i = 0; i < s.length(); i++) {
            freq.merge(s.charAt(i), 1, Integer::sum);
        }
        return freq;
    }

    public static String reverseWords(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            if (i < words.length - 1) sb.append(' ');
            sb.append(words[i]);
        }
        return sb.toString();
    }

    public static Integer tryParseInt(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            char left = s.charAt(i);
            char right = s.charAt(j);
            if (!Character.isLetterOrDigit(left)) { i++; continue; }
            if (!Character.isLetterOrDigit(right)) { j--; continue; }
            if (Character.toLowerCase(left) != Character.toLowerCase(right)) return false;
            i++;
            j--;
        }
        return true;
    }

    public static int midpoint(int low, int high) {
        return low + (high - low) / 2;  // без переполнения (low <= high)
    }
}
