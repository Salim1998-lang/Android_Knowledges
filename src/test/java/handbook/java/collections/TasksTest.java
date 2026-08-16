package handbook.java.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Тесты темы 4 «Коллекции + equals/hashCode»: Set/Map/List, контракт equals/hashCode для ключей,
 * Comparator/Comparable, TreeMap-навигация, LinkedHashMap→LRU, fail-fast итераторы.
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_distinct_preservesOrder() {
        assertEquals(List.of(3, 1, 2), Tasks.distinct(List.of(3, 1, 3, 2, 1)));
    }

    @Test void Л2_hasDuplicates() {
        assertTrue(Tasks.hasDuplicates(List.of(1, 2, 1)));
        assertFalse(Tasks.hasDuplicates(List.of(1, 2, 3)));
    }

    @Test void Л3_countBy() {
        assertEquals(Map.of("a", 2, "b", 1), Tasks.countBy(List.of("a", "b", "a")));
    }

    @Test void Л4_sumValues() {
        assertEquals(6, Tasks.sumValues(Map.of("a", 1, "b", 2, "c", 3)));
    }

    @Test void Л5_sortedCopy_doesNotMutate() {
        List<Integer> src = new ArrayList<>(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), Tasks.sortedCopy(src));
        assertEquals(List.of(3, 1, 2), src, "исходный список не должен меняться");
    }

    @Test void Л6_reversedCopy() {
        assertEquals(List.of(3, 2, 1), Tasks.reversedCopy(List.of(1, 2, 3)));
    }

    @Test void Л7_union() {
        assertEquals(Set.of(1, 2, 3), Tasks.union(Set.of(1, 2), Set.of(2, 3)));
    }

    @Test void Л8_intersection() {
        assertEquals(Set.of(2), Tasks.intersection(Set.of(1, 2), Set.of(2, 3)));
    }

    // ── Средние ──

    @Test void С9_equalsHashCode_asKey() {
        assertEquals(new Tasks.Point(1, 2), new Tasks.Point(1, 2), "равные по координатам");
        assertEquals(new Tasks.Point(1, 2).hashCode(), new Tasks.Point(1, 2).hashCode(), "хеш согласован");
        List<Tasks.Point> pts = List.of(
                new Tasks.Point(0, 0), new Tasks.Point(1, 1), new Tasks.Point(0, 0));
        assertEquals(2, Tasks.uniquePoints(pts), "дубликат схлопнулся в HashSet");
    }

    @Test void С10_groupByLength() {
        Map<Integer, List<String>> g = Tasks.groupByLength(List.of("a", "bb", "cc", "d"));
        assertEquals(List.of("a", "d"), g.get(1));
        assertEquals(List.of("bb", "cc"), g.get(2));
    }

    @Test void С11_comparatorChaining() {
        assertEquals(List.of("a", "aa", "bb", "ccc"),
                Tasks.sortByLengthThenAlpha(List.of("ccc", "bb", "aa", "a")));
    }

    @Test void С12_comparable() {
        List<Tasks.Version> vs = new ArrayList<>(List.of(
                new Tasks.Version(1, 2), new Tasks.Version(1, 0), new Tasks.Version(0, 9)));
        java.util.Collections.sort(vs);
        assertEquals(0, vs.get(0).major);
        assertEquals(9, vs.get(0).minor);
        assertEquals(1, vs.get(2).major);
        assertEquals(2, vs.get(2).minor);
        assertTrue(new Tasks.Version(1, 0).compareTo(new Tasks.Version(1, 2)) < 0);
    }

    @Test void С13_treeMapCeiling() {
        TreeMap<Integer, String> m = new TreeMap<>(Map.of(10, "a", 20, "b", 30, "c"));
        assertEquals(20, Tasks.ceilingKey(m, 15));
        assertEquals(10, Tasks.ceilingKey(m, 10));
        assertNull(Tasks.ceilingKey(m, 31));
    }

    @Test void С14_iteratorRemove() {
        List<String> list = new ArrayList<>(List.of("a", "bbb", "cc", "d", "eeee"));
        Tasks.removeShorterThan(list, 3);
        assertEquals(List.of("bbb", "eeee"), list);
    }

    @Test void С15_mergeCounts() {
        Map<String, Integer> a = Map.of("x", 1, "y", 2);
        Map<String, Integer> b = Map.of("y", 3, "z", 4);
        assertEquals(Map.of("x", 1, "y", 5, "z", 4), Tasks.mergeCounts(a, b));
    }

    // ── Сложные ──

    @Test void СЛ16_lruCache_evictsEldest() {
        Tasks.LruCache<Integer, String> cache = new Tasks.LruCache<>(2);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.get(1);            // 1 стал «свежим», старший теперь 2
        cache.put(3, "c");       // вытесняется 2
        assertEquals(Set.of(1, 3), cache.keySet());
        assertNull(cache.get(2));
    }

    @Test void СЛ17_caseInsensitiveEquality() {
        assertEquals(new Tasks.CIString("Hi"), new Tasks.CIString("HI"));
        assertEquals(2, Tasks.uniqueIgnoreCase(List.of(
                new Tasks.CIString("Hello"), new Tasks.CIString("HELLO"), new Tasks.CIString("world"))));
    }

    @Test void СЛ18_sortKeysByValueDesc() {
        Map<String, Integer> m = Map.of("a", 1, "b", 3, "c", 2, "d", 3);
        assertEquals(List.of("b", "d", "c", "a"), Tasks.sortKeysByValueDesc(m));
    }

    @Test void СЛ19_mode() {
        assertEquals("a", Tasks.mode(List.of("a", "b", "a", "c", "b", "a")));
        assertEquals(2, Tasks.mode(List.of(2, 3, 3, 2)), "ничья → раньше встретившийся");
    }

    @Test void СЛ20_failFast() {
        assertTrue(Tasks.concurrentModThrows(),
                "структурная модификация во время for-each → ConcurrentModificationException");
    }
}
