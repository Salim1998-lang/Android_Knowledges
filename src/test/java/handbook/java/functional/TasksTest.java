package handbook.java.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Тесты темы 6 «Функциональщина Java 8+»: лямбды, method references, Stream API
 * (map/filter/reduce/collect/flatMap), коллекторы, Optional.
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_mapDouble() {
        assertEquals(List.of(2, 4, 6), Tasks.mapDouble(List.of(1, 2, 3)));
    }

    @Test void Л2_filterEven() {
        assertEquals(List.of(2, 4), Tasks.filterEven(List.of(1, 2, 3, 4, 5)));
    }

    @Test void Л3_sum() {
        assertEquals(15, Tasks.sum(List.of(1, 2, 3, 4, 5)));
        assertEquals(0, Tasks.sum(List.of()));
    }

    @Test void Л4_toUpper_methodRef() {
        assertEquals(List.of("A", "BB"), Tasks.toUpper(List.of("a", "bb")));
    }

    @Test void Л5_anyNegative() {
        assertTrue(Tasks.anyNegative(List.of(1, -2, 3)));
        assertFalse(Tasks.anyNegative(List.of(1, 2, 3)));
    }

    @Test void Л6_countMatching_predicateParam() {
        assertEquals(2, Tasks.countMatching(List.of(1, 2, 3, 4), n -> n > 2));
        assertEquals(0, Tasks.countMatching(List.of(1, 2), n -> n > 5));
    }

    @Test void Л7_lengths() {
        assertEquals(List.of(1, 3, 2), Tasks.lengths(List.of("a", "bbb", "cc")));
    }

    @Test void Л8_joinCsv() {
        assertEquals("a,b,c", Tasks.joinCsv(List.of("a", "b", "c")));
        assertEquals("", Tasks.joinCsv(List.of()));
    }

    // ── Средние ──

    @Test void С9_distinctSorted() {
        assertEquals(List.of(1, 2, 3), Tasks.distinctSorted(List.of(3, 1, 2, 3, 1)));
    }

    @Test void С10_groupByLength() {
        Map<Integer, List<String>> g = Tasks.groupByLength(List.of("a", "bb", "cc", "d"));
        assertEquals(List.of("a", "d"), g.get(1));
        assertEquals(List.of("bb", "cc"), g.get(2));
    }

    @Test void С11_countByFirstChar() {
        assertEquals(Map.of('a', 2L, 'b', 1L),
                Tasks.countByFirstChar(List.of("apple", "avocado", "banana")));
    }

    @Test void С12_nameToLength() {
        assertEquals(Map.of("a", 1, "bb", 2, "ccc", 3),
                Tasks.nameToLength(List.of("a", "bb", "ccc")));
    }

    @Test void С13_firstEven_optional() {
        assertEquals(Optional.of(2), Tasks.firstEven(List.of(1, 2, 3, 4)));
        assertEquals(Optional.empty(), Tasks.firstEven(List.of(1, 3, 5)));
    }

    @Test void С14_optionalMapOrElse() {
        assertEquals(5, Tasks.optionalLengthOrZero(Optional.of("hello")));
        assertEquals(0, Tasks.optionalLengthOrZero(Optional.empty()));
    }

    @Test void С15_product_reduce() {
        assertEquals(24, Tasks.product(List.of(1, 2, 3, 4)));
        assertEquals(1, Tasks.product(List.of()), "пустой список → identity 1");
    }

    // ── Сложные ──

    @Test void СЛ16_flatten() {
        assertEquals(List.of(1, 2, 3, 4, 5),
                Tasks.flatten(List.of(List.of(1, 2), List.of(3), List.of(4, 5))));
    }

    @Test void СЛ17_longestWord() {
        assertEquals(Optional.of("banana"), Tasks.longestWord(List.of("a", "banana", "cc")));
        assertEquals(Optional.empty(), Tasks.longestWord(List.of()));
    }

    @Test void СЛ18_topN() {
        assertEquals(List.of(9, 5, 4), Tasks.topN(List.of(3, 1, 4, 1, 5, 9, 2), 3));
    }

    @Test void СЛ19_upperByLength() {
        Map<Integer, List<String>> g = Tasks.upperByLength(List.of("a", "bb", "cc", "d"));
        assertEquals(List.of("A", "D"), g.get(1));
        assertEquals(List.of("BB", "CC"), g.get(2));
    }

    @Test void СЛ20_wordFrequency() {
        assertEquals(Map.of("a", 2L, "b", 1L, "c", 1L), Tasks.wordFrequency("a b a c"));
    }
}
