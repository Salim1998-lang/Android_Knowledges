package handbook.java.generics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Тесты темы 3 «Дженерики»: тип-параметры и обобщённые методы/классы, границы {@code extends},
 * wildcards ({@code ? extends} / {@code ? super}, PECS), стирание типов и его обходы.
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_firstOf() {
        assertEquals("a", Tasks.firstOf(List.of("a", "b", "c")));
        assertEquals(10, Tasks.firstOf(List.of(10, 20)));
    }

    @Test void Л2_pairList() {
        assertEquals(List.of(1, 2), Tasks.pairList(1, 2));
    }

    @Test void Л3_swap() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        Tasks.swap(list, 0, 2);
        assertEquals(List.of("c", "b", "a"), list);
    }

    @Test void Л4_boxOf() {
        Tasks.Box<String> box = Tasks.boxOf("hi");
        assertEquals("hi", box.get());
    }

    @Test void Л5_pair() {
        Tasks.Pair<String, Integer> p = Tasks.pair("age", 30);
        assertEquals("age", p.first());
        assertEquals(30, p.second());
    }

    @Test void Л6_maxOf() {
        assertEquals(9, Tasks.maxOf(List.of(3, 9, 1, 7)));
        assertEquals("c", Tasks.maxOf(List.of("a", "c", "b")));
    }

    @Test void Л7_frequency() {
        assertEquals(3, Tasks.frequency(List.of(1, 2, 1, 3, 1), 1));
        assertEquals(0, Tasks.frequency(List.of("x"), "y"));
    }

    @Test void Л8_repeat() {
        assertEquals(List.of("z", "z", "z"), Tasks.repeat("z", 3));
        assertEquals(List.of(), Tasks.repeat("z", 0));
    }

    // ── Средние ──

    @Test void С9_sumExtendsNumber() {
        assertEquals(6.0, Tasks.sumOfNumbers(List.of(1, 2, 3)), 1e-9);
        assertEquals(3.5, Tasks.sumOfNumbers(List.of(1.5, 2.0)), 1e-9);
    }

    @Test void С10_addSuperInteger() {
        List<Number> dst = new ArrayList<>();
        Tasks.addInts(dst, 3);
        assertEquals(List.of(1, 2, 3), dst);
    }

    @Test void С11_copyAllPECS() {
        List<Object> dst = new ArrayList<>();
        Tasks.copyAll(List.of(1, 2, 3), dst);
        assertEquals(List.of(1, 2, 3), dst);
    }

    @Test void С12_arrayCovarianceVsGenericInvariance() {
        assertTrue(Tasks.arrayStoreThrows(),
                "массивы ковариантны → ArrayStoreException в рантайме; дженерики инвариантны → ловится компилятором");
    }

    @Test void С13_maxBound() {
        assertEquals(9, Tasks.maxBound(List.of(3, 9, 1)));
    }

    @Test void С14_allEqual() {
        assertTrue(Tasks.allEqual(List.of(5, 5, 5)));
        assertFalse(Tasks.allEqual(List.of(5, 6, 5)));
        assertTrue(Tasks.allEqual(List.of()));
    }

    @Test void С15_invert() {
        assertEquals(Map.of("one", 1, "two", 2), Tasks.invert(Map.of(1, "one", 2, "two")));
    }

    // ── Сложные ──

    @Test void СЛ16_typeErasure() {
        assertTrue(Tasks.sameRawClass(new ArrayList<>(List.of("a")), new ArrayList<>(List.of(1))),
                "List<String> и List<Integer> — один класс в рантайме (стирание типов)");
    }

    @Test void СЛ17_toArray() {
        String[] arr = Tasks.toArray(List.of("a", "b"), new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, arr);
    }

    @Test void СЛ18_castOrNull() {
        assertEquals("hi", Tasks.castOrNull("hi", String.class));
        assertNull(Tasks.castOrNull(42, String.class), "не String → null");
        assertEquals(Integer.valueOf(42), Tasks.castOrNull(42, Integer.class));
    }

    @Test void СЛ19_reverseWildcardCapture() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4));
        Tasks.reverse(list);
        assertEquals(List.of(4, 3, 2, 1), list);
    }

    @Test void СЛ20_minMax() {
        Tasks.Pair<Integer, Integer> mm = Tasks.minMax(List.of(3, 9, 1, 7));
        assertEquals(1, mm.first());
        assertEquals(9, mm.second());
    }
}
