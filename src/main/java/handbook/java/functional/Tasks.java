package handbook.java.functional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Тема 6 «Функциональщина Java 8+» — 20 задач.
 * Реализуй помеченные {@code TODO} тела с помощью лямбд, method references и Stream API.
 * Проверка: {@code ./gradlew test --tests "handbook.java.functional.*"}.
 * Эталон — в {@link handbook.java.functional.solutions.Solutions}.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Удвой каждый элемент. Подсказка: {@code stream().map(n -> n * 2).collect(toList())}. */
    public static List<Integer> mapDouble(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO Л1 mapDouble");
    }

    /** Л2. Оставь только чётные. Подсказка: {@code filter(n -> n % 2 == 0)}. */
    public static List<Integer> filterEven(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO Л2 filterEven");
    }

    /** Л3. Сумма всех чисел. Подсказка: {@code mapToInt(Integer::intValue).sum()}. */
    public static int sum(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO Л3 sum");
    }

    /** Л4. Переведи все строки в верхний регистр через METHOD REFERENCE {@code String::toUpperCase}. */
    public static List<String> toUpper(List<String> words) {
        throw new UnsupportedOperationException("TODO Л4 toUpper");
    }

    /** Л5. Есть ли хоть одно отрицательное? Подсказка: {@code anyMatch(n -> n < 0)}. */
    public static boolean anyNegative(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO Л5 anyNegative");
    }

    /**
     * Л6. Посчитай, сколько элементов удовлетворяют переданному {@link Predicate} (функция как параметр —
     * функциональный интерфейс). Подсказка: {@code (int) stream().filter(predicate).count()}.
     */
    public static int countMatching(List<Integer> nums, Predicate<Integer> predicate) {
        throw new UnsupportedOperationException("TODO Л6 countMatching");
    }

    /** Л7. Длины строк. Подсказка: {@code map(String::length)}. */
    public static List<Integer> lengths(List<String> words) {
        throw new UnsupportedOperationException("TODO Л7 lengths");
    }

    /** Л8. Склей строки через запятую. Подсказка: {@code collect(Collectors.joining(","))}. */
    public static String joinCsv(List<String> words) {
        throw new UnsupportedOperationException("TODO Л8 joinCsv");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /** С9. Убери дубликаты и отсортируй по возрастанию. Подсказка: {@code distinct().sorted()}. */
    public static List<Integer> distinctSorted(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO С9 distinctSorted");
    }

    /**
     * С10. Сгруппируй слова по длине: {@code длина → список слов}. Подсказка:
     * {@code Collectors.groupingBy(String::length)}.
     */
    public static Map<Integer, List<String>> groupByLength(List<String> words) {
        throw new UnsupportedOperationException("TODO С10 groupByLength");
    }

    /**
     * С11. Посчитай слова по ПЕРВОЙ букве: {@code буква → количество}. Подсказка: downstream-коллектор
     * {@code groupingBy(s -> s.charAt(0), Collectors.counting())}.
     */
    public static Map<Character, Long> countByFirstChar(List<String> words) {
        throw new UnsupportedOperationException("TODO С11 countByFirstChar");
    }

    /**
     * С12. Собери карту {@code слово → его длина}. Подсказка: {@code Collectors.toMap(w -> w, String::length)}.
     * (Слова уникальны.)
     */
    public static Map<String, Integer> nameToLength(List<String> words) {
        throw new UnsupportedOperationException("TODO С12 nameToLength");
    }

    /**
     * С13. Верни первое чётное как {@link Optional} (пусто, если чётных нет). Подсказка:
     * {@code filter(...).findFirst()}. Optional — типобезопасная замена возврату {@code null}.
     */
    public static Optional<Integer> firstEven(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO С13 firstEven");
    }

    /**
     * С14. Верни длину строки внутри {@link Optional}, либо 0, если Optional пуст. Подсказка:
     * {@code opt.map(String::length).orElse(0)} — {@code map} применяется, только если значение есть.
     */
    public static int optionalLengthOrZero(Optional<String> maybe) {
        throw new UnsupportedOperationException("TODO С14 optionalLengthOrZero");
    }

    /**
     * С15. Произведение всех чисел (пустой список → 1). Подсказка: {@code reduce(1, (a, b) -> a * b)}
     * — начальное значение (identity) + аккумулятор.
     */
    public static int product(List<Integer> nums) {
        throw new UnsupportedOperationException("TODO С15 product");
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. «Расплющи» список списков в один список. Подсказка: {@code flatMap(List::stream)} —
     * превращает каждый элемент в под-поток и склеивает их.
     */
    public static List<Integer> flatten(List<List<Integer>> nested) {
        throw new UnsupportedOperationException("TODO СЛ16 flatten");
    }

    /**
     * СЛ17. Самое длинное слово как {@link Optional} (при равной длине — любое из максимальных).
     * Подсказка: {@code max(Comparator.comparingInt(String::length))}.
     */
    public static Optional<String> longestWord(List<String> words) {
        throw new UnsupportedOperationException("TODO СЛ17 longestWord");
    }

    /**
     * СЛ18. Верни {@code n} наибольших чисел по убыванию. Подсказка:
     * {@code sorted(Comparator.reverseOrder()).limit(n)}.
     */
    public static List<Integer> topN(List<Integer> nums, int n) {
        throw new UnsupportedOperationException("TODO СЛ18 topN");
    }

    /**
     * СЛ19. Сгруппируй слова по длине, но в значениях — слова в ВЕРХНЕМ регистре. Подсказка:
     * downstream {@code Collectors.mapping(String::toUpperCase, Collectors.toList())}.
     */
    public static Map<Integer, List<String>> upperByLength(List<String> words) {
        throw new UnsupportedOperationException("TODO СЛ19 upperByLength");
    }

    /**
     * СЛ20. Частота слов в тексте (слова разделены одиночными пробелами): {@code слово → количество}.
     * Полный конвейер: {@code split} → {@code stream} → {@code groupingBy(w -> w, counting())}.
     */
    public static Map<String, Long> wordFrequency(String text) {
        throw new UnsupportedOperationException("TODO СЛ20 wordFrequency");
    }
}
