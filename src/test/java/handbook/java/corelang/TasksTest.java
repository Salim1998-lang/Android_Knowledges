package handbook.java.corelang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Тесты темы 1 «Язык, типы, строки». Проверяют не только результат, но и нюансы: кэш {@code Integer},
 * переполнение {@code int}, неизменяемость и пул строк, {@code ==} против {@code equals},
 * передачу ссылки «по значению».
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_boxedIdentity() {
        assertTrue(Tasks.boxedIdentity(100), "100 в кэше Integer (−128..127) → одна ссылка");
        assertFalse(Tasks.boxedIdentity(1000), "1000 вне кэша → разные объекты");
    }

    @Test void Л2_wrappingSum() {
        assertEquals(7, Tasks.wrappingSum(3, 4));
        assertEquals(Integer.MIN_VALUE, Tasks.wrappingSum(Integer.MAX_VALUE, 1), "int заворачивается");
    }

    @Test void Л3_safeSum() {
        assertEquals(2_147_483_648L, Tasks.safeSum(Integer.MAX_VALUE, 1), "в long переполнения нет");
        assertEquals(7L, Tasks.safeSum(3, 4));
    }

    @Test void Л4_reverse() {
        assertEquals("cba", Tasks.reverse("abc"));
        assertEquals("", Tasks.reverse(""));
    }

    @Test void Л5_contentEquals() {
        assertTrue(Tasks.contentEquals(new String("abc"), "abc"), "по содержимому равны");
        assertFalse(Tasks.contentEquals("abc", "abd"));
        assertTrue(Tasks.contentEquals(null, null), "не должно падать на null");
        assertFalse(Tasks.contentEquals(null, "x"));
    }

    @Test void Л6_digitToInt() {
        assertEquals(7, Tasks.digitToInt('7'));
        assertEquals(0, Tasks.digitToInt('0'));
    }

    @Test void Л7_sign() {
        assertEquals("positive", Tasks.sign(5));
        assertEquals("negative", Tasks.sign(-5));
        assertEquals("zero", Tasks.sign(0));
    }

    @Test void Л8_unboxOrZero() {
        assertEquals(42, Tasks.unboxOrZero(42));
        assertEquals(0, Tasks.unboxOrZero(null), "null → 0, без NPE");
    }

    // ── Средние ──

    @Test void С9_internedSameRef() {
        assertTrue(Tasks.internedSameRef(new String("xy"), "x" + "y"),
                "после intern() равные строки — один объект пула");
    }

    @Test void С10_join() {
        assertEquals("a-b-c", Tasks.join(List.of("a", "b", "c"), "-"));
        assertEquals("solo", Tasks.join(List.of("solo"), "-"), "без хвостового разделителя");
        assertEquals("", Tasks.join(List.of(), "-"));
    }

    @Test void С11_arraysContentEqual() {
        assertTrue(Tasks.arraysContentEqual(new int[]{1, 2, 3}, new int[]{1, 2, 3}));
        assertFalse(Tasks.arraysContentEqual(new int[]{1, 2}, new int[]{1, 2, 3}));
    }

    @Test void С12_nearlyEqual() {
        assertTrue(Tasks.nearlyEqual(0.1 + 0.2, 0.3, 1e-9), "0.1+0.2 != 0.3 точно, но близко");
        assertFalse(Tasks.nearlyEqual(1.0, 2.0, 1e-9));
    }

    @Test void С13_floorMod() {
        assertEquals(2, Tasks.floorMod(-1, 3), "математический остаток, не −1");
        assertEquals(1, Tasks.floorMod(7, 3));
    }

    @Test void С14_doubleFirst() {
        int[] arr = {5, 6, 7};
        Tasks.doubleFirst(arr);
        assertEquals(10, arr[0], "изменение объекта по ссылке видно вызывающему");
        assertEquals(6, arr[1]);
    }

    @Test void С15_normalizedCompare() {
        assertEquals(-1, Tasks.normalizedCompare("a", "b"));
        assertEquals(1, Tasks.normalizedCompare("b", "a"));
        assertEquals(0, Tasks.normalizedCompare("a", "a"));
    }

    // ── Сложные ──

    @Test void СЛ16_charFrequency() {
        assertEquals(Map.of('a', 2, 'b', 1), Tasks.charFrequency("aab"));
        assertEquals(Map.of(), Tasks.charFrequency(""));
    }

    @Test void СЛ17_reverseWords() {
        assertEquals("three two one", Tasks.reverseWords("one two three"));
        assertEquals("solo", Tasks.reverseWords("solo"));
    }

    @Test void СЛ18_tryParseInt() {
        assertEquals(Integer.valueOf(42), Tasks.tryParseInt("42"));
        assertEquals(Integer.valueOf(-7), Tasks.tryParseInt("-7"));
        assertNull(Tasks.tryParseInt("abc"), "не число → null");
        assertNull(Tasks.tryParseInt(""));
    }

    @Test void СЛ19_isPalindrome() {
        assertTrue(Tasks.isPalindrome("A man, a plan, a canal: Panama"));
        assertTrue(Tasks.isPalindrome(""));
        assertFalse(Tasks.isPalindrome("race a car"));
    }

    @Test void СЛ20_midpoint() {
        assertEquals(5, Tasks.midpoint(3, 7));
        int mid = Tasks.midpoint(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE - 1, mid, "без переполнения при больших int");
    }
}
