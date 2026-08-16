package handbook.java.functional.solutions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Эталонные решения темы 6. Подсмотри, если застрял с {@link handbook.java.functional.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Удвой каждый элемент. Подсказка: {@code stream().map(n -> n * 2).collect(toList())}. */
    public static List<Integer> mapDouble(List<Integer> nums) {
        return nums.stream().map(n -> n * 2).collect(Collectors.toList());
    }

    /** Л2. Оставь только чётные. Подсказка: {@code filter(n -> n % 2 == 0)}. */
    public static List<Integer> filterEven(List<Integer> nums) {
        return nums.stream().filter(n -> n % 2 == 0).collect(Collectors.toList());
    }

    /** Л3. Сумма всех чисел. Подсказка: {@code mapToInt(Integer::intValue).sum()}. */
    public static int sum(List<Integer> nums) {
        return nums.stream().mapToInt(Integer::intValue).sum();
    }

    /** Л4. Переведи все строки в верхний регистр через METHOD REFERENCE {@code String::toUpperCase}. */
    public static List<String> toUpper(List<String> words) {
        return words.stream().map(String::toUpperCase).collect(Collectors.toList());
    }

    /** Л5. Есть ли хоть одно отрицательное? Подсказка: {@code anyMatch(n -> n < 0)}. */
    public static boolean anyNegative(List<Integer> nums) {
        return nums.stream().anyMatch(n -> n < 0);
    }

    /**
     * Л6. Посчитай, сколько элементов удовлетворяют переданному {@link Predicate} (функция как параметр —
     * функциональный интерфейс). Подсказка: {@code (int) stream().filter(predicate).count()}.
     */
    public static int countMatching(List<Integer> nums, Predicate<Integer> predicate) {
        return (int) nums.stream().filter(predicate).count();
    }

    /** Л7. Длины строк. Подсказка: {@code map(String::length)}. */
    public static List<Integer> lengths(List<String> words) {
        return words.stream().map(String::length).collect(Collectors.toList());
    }

    /** Л8. Склей строки через запятую. Подсказка: {@code collect(Collectors.joining(","))}. */
    public static String joinCsv(List<String> words) {
        return words.stream().collect(Collectors.joining(","));
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /** С9. Убери дубликаты и отсортируй по возрастанию. Подсказка: {@code distinct().sorted()}. */
    public static List<Integer> distinctSorted(List<Integer> nums) {
        return nums.stream().distinct().sorted().collect(Collectors.toList());
    }

    /**
     * С10. Сгруппируй слова по длине: {@code длина → список слов}. Подсказка:
     * {@code Collectors.groupingBy(String::length)}.
     */
    public static Map<Integer, List<String>> groupByLength(List<String> words) {
        return words.stream().collect(Collectors.groupingBy(String::length));
    }

    /**
     * С11. Посчитай слова по ПЕРВОЙ букве: {@code буква → количество}. Подсказка: downstream-коллектор
     * {@code groupingBy(s -> s.charAt(0), Collectors.counting())}.
     */
    public static Map<Character, Long> countByFirstChar(List<String> words) {
        return words.stream().collect(Collectors.groupingBy(s -> s.charAt(0), Collectors.counting()));
    }

    /**
     * С12. Собери карту {@code слово → его длина}. Подсказка: {@code Collectors.toMap(w -> w, String::length)}.
     * (Слова уникальны.)
     */
    public static Map<String, Integer> nameToLength(List<String> words) {
        return words.stream().collect(Collectors.toMap(w -> w, String::length));
    }

    /**
     * С13. Верни первое чётное как {@link Optional} (пусто, если чётных нет). Подсказка:
     * {@code filter(...).findFirst()}. Optional — типобезопасная замена возврату {@code null}.
     */
    public static Optional<Integer> firstEven(List<Integer> nums) {
        return nums.stream().filter(n -> n % 2 == 0).findFirst();
    }

    /**
     * С14. Верни длину строки внутри {@link Optional}, либо 0, если Optional пуст. Подсказка:
     * {@code opt.map(String::length).orElse(0)} — {@code map} применяется, только если значение есть.
     */
    public static int optionalLengthOrZero(Optional<String> maybe) {
        return maybe.map(String::length).orElse(0);
    }

    /**
     * С15. Произведение всех чисел (пустой список → 1). Подсказка: {@code reduce(1, (a, b) -> a * b)}
     * — начальное значение (identity) + аккумулятор.
     */
    public static int product(List<Integer> nums) {
        return nums.stream().reduce(1, (a, b) -> a * b);
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. «Расплющи» список списков в один список. Подсказка: {@code flatMap(List::stream)} —
     * превращает каждый элемент в под-поток и склеивает их.
     */
    public static List<Integer> flatten(List<List<Integer>> nested) {
        return nested.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    /**
     * СЛ17. Самое длинное слово как {@link Optional} (при равной длине — любое из максимальных).
     * Подсказка: {@code max(Comparator.comparingInt(String::length))}.
     */
    public static Optional<String> longestWord(List<String> words) {
        return words.stream().max(java.util.Comparator.comparingInt(String::length));
    }

    /**
     * СЛ18. Верни {@code n} наибольших чисел по убыванию. Подсказка:
     * {@code sorted(Comparator.reverseOrder()).limit(n)}.
     */
    public static List<Integer> topN(List<Integer> nums, int n) {
        return nums.stream()
                .sorted(java.util.Comparator.reverseOrder())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * СЛ19. Сгруппируй слова по длине, но в значениях — слова в ВЕРХНЕМ регистре. Подсказка:
     * downstream {@code Collectors.mapping(String::toUpperCase, Collectors.toList())}.
     */
    public static Map<Integer, List<String>> upperByLength(List<String> words) {
        return words.stream().collect(Collectors.groupingBy(
                String::length,
                Collectors.mapping(String::toUpperCase, Collectors.toList())));
    }

    /**
     * СЛ20. Частота слов в тексте (слова разделены одиночными пробелами): {@code слово → количество}.
     * Полный конвейер: {@code split} → {@code stream} → {@code groupingBy(w -> w, counting())}.
     */
    public static Map<String, Long> wordFrequency(String text) {
        return java.util.Arrays.stream(text.split(" "))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
    }
}
